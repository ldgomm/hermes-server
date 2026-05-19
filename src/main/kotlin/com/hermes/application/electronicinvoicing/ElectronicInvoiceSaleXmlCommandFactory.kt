package com.hermes.application.electronicinvoicing

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.money.Money
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleTaxSummary
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Builds the canonical SRI invoice XML command from an already persisted sale.
 *
 * Important boundary:
 * - Sale/tax snapshots are the source of financial truth.
 * - OrganizationSriSettings are the source of issuer/SRI configuration truth.
 * - This factory does not reserve sequences, sign XML, submit to SRI or read secrets.
 */
class SaleBackedElectronicInvoiceXmlCommandFactory(
    private val saleRepository: OperationalSaleRepository,
    private val settingsRepository: OrganizationSriSettingsRepository,
) : ElectronicInvoiceXmlCommandFactory {

    override fun build(command: PrepareElectronicInvoiceXmlCommand): BuildSriInvoiceXmlCommand {
        val issue = command.issueCommand
        val sale = saleRepository.findById(issue.organizationId, issue.saleId)
            ?: throw DomainRuleViolation("Sale does not exist for electronic invoice emission.")
        if (sale.organizationId != issue.organizationId) {
            throw DomainRuleViolation("Sale does not belong to requested organization.")
        }

        val settings = settingsRepository.findByOrganizationId(issue.organizationId)
            ?: throw DomainRuleViolation("SRI settings are required to build electronic invoice XML.")
        settings.assertMatchesIssue(issue)

        val activeItems = sale.activeItems
        if (activeItems.isEmpty()) throw DomainRuleViolation("Cannot issue electronic invoice for sale without active items.")

        val taxSummary = SaleTaxSummary.fromItems(activeItems)
        val totalTaxes = taxSummary.taxesByRate.map { rateLine ->
            SriInvoiceTotalTax(
                codigo = rateLine.taxCode,
                codigoPorcentaje = rateLine.rateCode,
                baseImponible = rateLine.base.amount.sriMoney(),
                tarifa = rateLine.rate.sriRate(),
                valor = rateLine.taxAmount.amount.sriMoney(),
            )
        }
        if (totalTaxes.isEmpty()) {
            throw DomainRuleViolation("Electronic invoice requires at least one tax summary line.")
        }

        val customer = sale.customerSnapshot
        val buyerIdentification = customer.resolveSriIdentification()
        val payments = sale.toSriPayments()

        return BuildSriInvoiceXmlCommand(
            schemaVersion = settings.invoiceSchemaVersion,
            infoTributaria = SriInvoiceTaxInfo(
                environment = issue.environment,
                razonSocial = settings.legalName,
                nombreComercial = settings.commercialName,
                ruc = settings.ruc,
                accessKey = command.accessKey,
                documentType = SriDocumentType.INVOICE,
                series = issue.series,
                sequential = command.sequentialReservation.sequential,
                dirMatriz = settings.matrixAddress,
                contribuyenteRimpe = settings.rimpeLegend.toSriRimpeLegend(),
            ),
            infoFactura = SriInvoiceInfo(
                fechaEmision = issue.issuedDate,
                dirEstablecimiento = settings.establishmentAddress,
                contribuyenteEspecial = settings.specialTaxpayerCode,
                obligadoContabilidad = if (settings.obligatedToKeepAccounting) {
                    SriAccountingObligation.YES
                } else {
                    SriAccountingObligation.NO
                },
                buyerIdentificationType = buyerIdentification.first,
                buyerLegalName = customer.displayName.trim(),
                buyerIdentification = buyerIdentification.second,
                buyerAddress = null,
                totalSinImpuestos = activeItems.sumOf { it.netTotal.amount }.sriMoney(),
                totalDescuento = activeItems.sumOf { it.discount.amount }.sriMoney(),
                totalConImpuestos = totalTaxes,
                propina = BigDecimal.ZERO.sriMoney(),
                importeTotal = sale.total.amount.sriMoney(),
                pagos = payments,
            ),
            detalles = activeItems.map { item -> item.toSriInvoiceDetail() },
            infoAdicional = buildInfoAdicional(sale, customer),
        )
    }

    private fun OrganizationSriSettings.assertMatchesIssue(issue: IssueElectronicInvoiceCommand) {
        if (environment != issue.environment) {
            throw DomainRuleViolation("SRI settings environment does not match requested issue environment.")
        }
        if (ruc != issue.issuerRuc.trim()) {
            throw DomainRuleViolation("SRI settings RUC does not match requested issuer RUC.")
        }
        if (series != issue.series) {
            throw DomainRuleViolation("SRI settings series does not match requested issue series.")
        }
    }

    private fun SaleItem.toSriInvoiceDetail(): SriInvoiceDetail {
        val detailTaxes = if (taxes.isEmpty()) {
            listOf(
                SriInvoiceDetailTax(
                    codigo = taxProfileSnapshot.sriTaxCode,
                    codigoPorcentaje = taxProfileSnapshot.sriRateCode,
                    tarifa = taxProfileSnapshot.rate.value.sriRate(),
                    baseImponible = netTotal.amount.sriMoney(),
                    valor = BigDecimal.ZERO.sriMoney(),
                )
            )
        } else {
            taxes.map { tax ->
                SriInvoiceDetailTax(
                    codigo = tax.taxCode,
                    codigoPorcentaje = tax.rateCode,
                    tarifa = tax.rate.value.sriRate(),
                    baseImponible = tax.taxableBase.amount.sriMoney(),
                    valor = tax.amount.amount.sriMoney(),
                )
            }
        }

        return SriInvoiceDetail(
            codigoPrincipal = catalogSnapshot.globalCatalogId.trim().takeIf { it.isNotBlank() } ?: catalogItemId,
            codigoAuxiliar = catalogItemId,
            descripcion = name.trim(),
            unidadMedida = catalogSnapshot.unitCode.trim().takeIf { it.isNotBlank() },
            cantidad = quantity.value.sriQuantity(),
            precioUnitario = unitPrice.amount.sriUnitPrice(),
            descuento = discount.amount.sriMoney(),
            precioTotalSinImpuesto = netTotal.amount.sriMoney(),
            detallesAdicionales = listOf(
                SriInvoiceDetailAdditional(nombre = "taxProfile", valor = taxProfileSnapshot.code),
            ),
            impuestos = detailTaxes,
        )
    }

    private fun Sale.toSriPayments(): List<SriInvoicePayment> {
        val effectivePayments = payments.filter { it.isEffective }
        val paid = effectivePayments.fold(Money.zero(total.currency)) { current, payment -> current + payment.amount }

        if (effectivePayments.isEmpty() || paid.amount.sriMoney() != total.amount.sriMoney()) {
            return listOf(
                SriInvoicePayment(
                    formaPago = if (effectivePayments.isEmpty()) {
                        SriInvoicePaymentForm.WITHOUT_FINANCIAL_SYSTEM
                    } else {
                        SriInvoicePaymentForm.OTHER_WITH_FINANCIAL_SYSTEM
                    },
                    total = total.amount.sriMoney(),
                )
            )
        }

        return effectivePayments
            .groupBy { it.method.toSriPaymentForm() }
            .map { (form, values) ->
                SriInvoicePayment(
                    formaPago = form,
                    total = values.fold(Money.zero(total.currency)) { current, payment -> current + payment.amount }.amount.sriMoney(),
                )
            }
    }

    private fun PaymentMethod.toSriPaymentForm(): SriInvoicePaymentForm = when (this) {
        PaymentMethod.CASH -> SriInvoicePaymentForm.WITHOUT_FINANCIAL_SYSTEM
        PaymentMethod.CARD_MANUAL,
        PaymentMethod.CARD_GATEWAY -> SriInvoicePaymentForm.CREDIT_CARD

        PaymentMethod.BANK_TRANSFER,
        PaymentMethod.DIGITAL_WALLET,
        PaymentMethod.OTHER -> SriInvoicePaymentForm.OTHER_WITH_FINANCIAL_SYSTEM
    }

    private fun CustomerSnapshot.resolveSriIdentification(): Pair<SriIdentificationType, String> {
        val identification = taxId?.trim()?.takeIf { it.isNotBlank() }
            ?: SriIdentificationType.FINAL_CONSUMER_IDENTIFICATION
        val type = taxIdType?.trim()?.takeIf { it.isNotBlank() }?.let { rawType ->
            runCatching { SriIdentificationType.fromStorage(rawType) }.getOrNull()
        } ?: SriIdentificationType.inferBasic(identification)

        return type to identification
    }

    private fun String?.toSriRimpeLegend(): SriInvoiceRimpeLegend {
        val normalized = this?.trim()?.takeIf { it.isNotBlank() } ?: return SriInvoiceRimpeLegend.NONE
        return when {
            normalized.equals(SriInvoiceRimpeLegend.NEGOCIO_POPULAR_REGIMEN_RIMPE.xmlValue, ignoreCase = true) ->
                SriInvoiceRimpeLegend.NEGOCIO_POPULAR_REGIMEN_RIMPE

            normalized.equals(SriInvoiceRimpeLegend.CONTRIBUYENTE_REGIMEN_RIMPE.xmlValue, ignoreCase = true) ->
                SriInvoiceRimpeLegend.CONTRIBUYENTE_REGIMEN_RIMPE

            normalized.contains("NEGOCIO POPULAR", ignoreCase = true) ->
                SriInvoiceRimpeLegend.NEGOCIO_POPULAR_REGIMEN_RIMPE

            normalized.contains("RIMPE", ignoreCase = true) ->
                SriInvoiceRimpeLegend.CONTRIBUYENTE_REGIMEN_RIMPE

            else -> SriInvoiceRimpeLegend.NONE
        }
    }

    private fun buildInfoAdicional(sale: Sale, customer: CustomerSnapshot): List<SriInvoiceAdditionalField> =
        buildList {
            add(SriInvoiceAdditionalField(nombre = "saleId", valor = sale.id))
            sale.saleNumber?.takeIf { it.isNotBlank() }
                ?.let { add(SriInvoiceAdditionalField(nombre = "saleNumber", valor = it)) }
            customer.email?.takeIf { it.isNotBlank() }
                ?.let { add(SriInvoiceAdditionalField(nombre = "email", valor = it)) }
        }

    private fun BigDecimal.sriMoney(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
    private fun BigDecimal.sriRate(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
    private fun BigDecimal.sriQuantity(): BigDecimal = setScale(6, RoundingMode.HALF_UP)
    private fun BigDecimal.sriUnitPrice(): BigDecimal = setScale(6, RoundingMode.HALF_UP)
}
