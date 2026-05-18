package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriAccessKey
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEmissionType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriIdentificationType
import com.hermes.domain.electronicinvoicing.SriInvoicePaymentForm
import com.hermes.domain.electronicinvoicing.SriInvoiceRimpeLegend
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.domain.electronicinvoicing.SriSeries
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Small factory helpers for tests, fixtures, and early application wiring.
 * Real Phase 11.7 orchestration should map from Sale/CommercialDocument snapshots
 * into the command explicitly instead of hiding business decisions inside the XML builder.
 */
object SriInvoiceXmlFactories {
    @Suppress("LongParameterList")
    fun minimalInvoice(
        issuedDate: LocalDate,
        environment: SriEnvironment,
        series: SriSeries,
        sequential: SriSequential,
        accessKey: SriAccessKey,
        issuerRuc: String,
        issuerLegalName: String,
        issuerMatrixAddress: String,
        buyerIdentificationType: SriIdentificationType,
        buyerIdentification: String,
        buyerLegalName: String,
        buyerAddress: String? = null,
        subtotal: BigDecimal,
        taxRate: BigDecimal,
        taxRateCode: String,
        taxAmount: BigDecimal,
        total: BigDecimal,
        itemCode: String,
        itemDescription: String,
        quantity: BigDecimal = BigDecimal.ONE.setScale(6),
        unitPrice: BigDecimal = subtotal.setScale(6),
        email: String? = null,
        rimpeLegend: SriInvoiceRimpeLegend = SriInvoiceRimpeLegend.NONE,
    ): BuildSriInvoiceXmlCommand = BuildSriInvoiceXmlCommand(
        infoTributaria = SriInvoiceTaxInfo(
            environment = environment,
            emissionType = SriEmissionType.NORMAL,
            razonSocial = issuerLegalName,
            ruc = issuerRuc,
            accessKey = accessKey,
            documentType = SriDocumentType.INVOICE,
            series = series,
            sequential = sequential,
            dirMatriz = issuerMatrixAddress,
            contribuyenteRimpe = rimpeLegend,
        ),
        infoFactura = SriInvoiceInfo(
            fechaEmision = issuedDate,
            buyerIdentificationType = buyerIdentificationType,
            buyerLegalName = buyerLegalName,
            buyerIdentification = buyerIdentification,
            buyerAddress = buyerAddress,
            totalSinImpuestos = subtotal.setScale(2),
            totalDescuento = BigDecimal.ZERO.setScale(2),
            totalConImpuestos = listOf(
                SriInvoiceTotalTax(
                    codigo = "2",
                    codigoPorcentaje = taxRateCode,
                    baseImponible = subtotal.setScale(2),
                    tarifa = taxRate.setScale(2),
                    valor = taxAmount.setScale(2),
                )
            ),
            importeTotal = total.setScale(2),
            pagos = listOf(SriInvoicePayment(SriInvoicePaymentForm.WITHOUT_FINANCIAL_SYSTEM, total.setScale(2))),
        ),
        detalles = listOf(
            SriInvoiceDetail(
                codigoPrincipal = itemCode,
                descripcion = itemDescription,
                cantidad = quantity,
                precioUnitario = unitPrice,
                descuento = BigDecimal.ZERO.setScale(2),
                precioTotalSinImpuesto = subtotal.setScale(2),
                impuestos = listOf(
                    SriInvoiceDetailTax(
                        codigo = "2",
                        codigoPorcentaje = taxRateCode,
                        tarifa = taxRate.setScale(2),
                        baseImponible = subtotal.setScale(2),
                        valor = taxAmount.setScale(2),
                    )
                )
            )
        ),
        infoAdicional = listOfNotNull(email?.let { SriInvoiceAdditionalField("Email", it) }),
    )
}
