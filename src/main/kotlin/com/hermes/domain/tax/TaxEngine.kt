package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode

object TaxEngine {
    fun calculateLine(input: TaxLineInput): TaxLineResult {
        val currency = input.currency
        val grossAmount = input.unitPrice.multiply(input.quantity.value)

        if (input.discount > grossAmount) {
            throw DomainRuleViolation("Tax line discount cannot be greater than gross amount.")
        }

        val netAmount = grossAmount - input.discount
        val snapshot = input.taxProfileSnapshot

        val taxAmount = when {
            snapshot.isTaxedWithPositiveRate && input.priceTaxMode == PriceTaxMode.TAX_EXCLUSIVE ->
                Money.of(
                    netAmount.amount.multiply(snapshot.fraction()).setScale(2, RoundingMode.HALF_UP),
                    currency,
                )

            snapshot.isTaxedWithPositiveRate && input.priceTaxMode == PriceTaxMode.TAX_INCLUSIVE -> {
                val divisor = BigDecimal.ONE.add(snapshot.fraction())
                val base = netAmount.amount
                    .divide(divisor, 6, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP)

                Money.of(netAmount.amount.subtract(base).setScale(2, RoundingMode.HALF_UP), currency)
            }

            else -> Money.zero(currency)
        }

        val baseAmount = when {
            snapshot.isTaxedWithPositiveRate && input.priceTaxMode == PriceTaxMode.TAX_INCLUSIVE -> netAmount - taxAmount
            else -> netAmount
        }

        val zero = Money.zero(currency)
        val taxableBase = if (snapshot.treatment in setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL)) {
            baseAmount
        } else {
            zero
        }
        val zeroRateBase = if (snapshot.treatment == TaxTreatment.IVA_ZERO) baseAmount else zero
        val exemptBase = if (snapshot.treatment == TaxTreatment.EXEMPT_IVA) baseAmount else zero
        val notSubjectBase = if (snapshot.treatment == TaxTreatment.NOT_SUBJECT_TO_IVA) baseAmount else zero
        val internalNoTaxBase = if (snapshot.treatment == TaxTreatment.NO_TAX_INTERNAL) baseAmount else zero

        val total = when (input.priceTaxMode) {
            PriceTaxMode.TAX_EXCLUSIVE -> baseAmount + taxAmount
            PriceTaxMode.TAX_INCLUSIVE -> netAmount
        }

        return TaxLineResult(
            lineId = input.lineId,
            description = input.description,
            grossAmount = grossAmount,
            discount = input.discount,
            taxableBase = taxableBase,
            zeroRateBase = zeroRateBase,
            exemptBase = exemptBase,
            notSubjectBase = notSubjectBase,
            internalNoTaxBase = internalNoTaxBase,
            taxAmount = taxAmount,
            total = total,
            taxProfileSnapshot = snapshot,
        )
    }

    fun calculate(lines: List<TaxLineInput>): TaxCalculationResult {
        if (lines.isEmpty()) throw DomainRuleViolation("Tax calculation requires at least one line.")
        val results = lines.map(::calculateLine)
        return TaxCalculationResult(lines = results, summary = TaxSummary.from(results))
    }
}
