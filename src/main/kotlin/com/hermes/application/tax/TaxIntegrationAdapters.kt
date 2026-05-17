package com.hermes.application.tax

import com.hermes.domain.percentage.Percentage
import com.hermes.domain.sale.SaleItemTax
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.tax.TaxCalculationResult
import com.hermes.domain.tax.TaxEmissionType
import com.hermes.domain.tax.TaxEmissionValidation
import com.hermes.domain.tax.TaxLineResult
import com.hermes.domain.tax.TaxProfileSnapshot
import com.hermes.domain.tax.TaxTreatment
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Small adapter layer prepared for Fase 7/ventas and Fase 8/documentos.
 *
 * The Tax Engine remains the source of truth. Sales and commercial documents should
 * consume immutable tax snapshots produced here instead of recalculating taxes in
 * their own modules.
 */
object TaxSaleIntegrationAdapter {
    fun prepareSalePayload(
        command: TaxSaleValidationCommand,
        validationResult: TaxSaleValidationResult,
    ): TaxPreparedSalePayload {
        val byLineId = validationResult.calculation.lines.associateBy { it.lineId }

        val preparedLines = command.lines.map { commandLine ->
            val taxLine = byLineId[commandLine.lineId]
                ?: error("Tax calculation did not return line ${commandLine.lineId}.")

            TaxPreparedSaleLine(
                lineId = commandLine.lineId,
                catalogItemId = commandLine.catalogItemId,
                taxProfileSnapshot = taxLine.taxProfileSnapshot,
                taxLineResult = taxLine,
            )
        }

        return TaxPreparedSalePayload(
            organizationId = validationResult.organizationId,
            occurredAt = validationResult.occurredAt,
            lines = preparedLines,
            calculation = validationResult.calculation,
        )
    }

    fun TaxProfileSnapshot.toSaleSnapshot(): TaxProfileSnapshotForSale =
        TaxProfileSnapshotForSale(
            code = profileCode,
            taxName = profileName,
            rate = Percentage.of(rate),
            sriTaxCode = sriTaxCode ?: "INTERNAL",
            sriRateCode = sriRateCode ?: "0",
            treatment = treatment,
            legalBasis = legalBasis,
            effectiveFrom = LocalDate.ofInstant(effectiveFrom, ZoneOffset.UTC),
            source = source.name,
        )

    fun TaxLineResult.toSaleItemTaxOrNull(): SaleItemTax? {
        if (taxProfileSnapshot.treatment == TaxTreatment.NO_TAX_INTERNAL) return null

        return SaleItemTax(
            taxCode = taxProfileSnapshot.sriTaxCode ?: "INTERNAL",
            rateCode = taxProfileSnapshot.sriRateCode ?: "0",
            rate = Percentage.of(taxProfileSnapshot.rate),
            taxableBase = baseForSri,
            amount = taxAmount,
        )
    }
}

object TaxDocumentIntegrationAdapter {
    fun assertReadyForCommercialDocument(
        emissionType: TaxEmissionType,
        calculation: TaxCalculationResult,
    ): TaxCalculationResult {
        TaxEmissionValidation.assertCanPrepareEmission(
            emissionType = emissionType,
            lines = calculation.lines,
            summary = calculation.summary,
        )
        return calculation
    }

    fun TaxCalculationResult.documentTaxTotals(): TaxDocumentTaxTotals =
        TaxDocumentTaxTotals(
            totalSinImpuestos = summary.subtotalTaxable +
                summary.subtotalZeroRate +
                summary.subtotalExempt +
                summary.subtotalNotSubject,
            totalDescuento = summary.totalDiscount,
            totalImpuesto = summary.totalTax,
            importeTotal = summary.grandTotal,
            impuestos = summary.taxesByRate.map { rateLine ->
                TaxDocumentTaxLine(
                    sriTaxCode = rateLine.sriTaxCode,
                    sriRateCode = rateLine.sriRateCode,
                    treatment = rateLine.treatment,
                    rate = rateLine.rate,
                    base = rateLine.base,
                    taxAmount = rateLine.taxAmount,
                )
            },
        )
}

data class TaxDocumentTaxTotals(
    val totalSinImpuestos: com.hermes.domain.money.Money,
    val totalDescuento: com.hermes.domain.money.Money,
    val totalImpuesto: com.hermes.domain.money.Money,
    val importeTotal: com.hermes.domain.money.Money,
    val impuestos: List<TaxDocumentTaxLine>,
)

data class TaxDocumentTaxLine(
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val treatment: TaxTreatment,
    val rate: java.math.BigDecimal,
    val base: com.hermes.domain.money.Money,
    val taxAmount: com.hermes.domain.money.Money,
)
