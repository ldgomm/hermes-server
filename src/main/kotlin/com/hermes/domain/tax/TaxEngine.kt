package com.hermes.domain.tax

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object TaxEngine {
    private const val INTERNAL_SCALE = 8

    fun calculate(
        lines: List<TaxLineInput>,
        globalDiscount: Money? = null,
        calculationDate: LocalDate = LocalDate.now(),
    ): TaxCalculationResult {
        if (lines.isEmpty()) throw DomainRuleViolation("Tax calculation requires at least one line.")

        val currency = lines.first().unitPrice.currency
        lines.forEach { line ->
            if (line.unitPrice.currency != currency || line.discount.currency != currency) {
                throw DomainRuleViolation("All tax lines must use the same currency.")
            }
            if (line.taxProfile == null) {
                throw DomainRuleViolation("Tax profile is required for line ${line.lineId}.")
            }
            if (!line.manualDiscountAllowed && line.discount.amount > BigDecimal.ZERO) {
                throw DomainRuleViolation("Manual discount is not allowed for line ${line.lineId}.")
            }
        }

        val effectiveGlobalDiscount = globalDiscount ?: Money.zero(currency)
        if (effectiveGlobalDiscount.currency != currency) {
            throw DomainRuleViolation("Global discount currency must match line currency.")
        }

        val preliminaryBases = lines.map { line -> preDiscountBase(line) }
        val preliminarySubtotal = preliminaryBases.fold(BigDecimal.ZERO, BigDecimal::add)
        if (effectiveGlobalDiscount.amount > preliminarySubtotal.setScale(2, RoundingMode.HALF_UP)) {
            throw DomainRuleViolation("Global discount cannot be greater than taxable subtotal.")
        }

        val results = lines.mapIndexed { index, line ->
            val profile = line.taxProfile ?: throw DomainRuleViolation("Tax profile is required for line ${line.lineId}.")
            val snapshot = TaxProfileSnapshot.from(profile, calculationDate)
            val baseBeforeDiscount = preliminaryBases[index]
            val globalPart = proratedDiscount(
                lineBase = baseBeforeDiscount,
                subtotal = preliminarySubtotal,
                discount = effectiveGlobalDiscount.amount,
            )
            val totalDiscount = line.discount.amount.add(globalPart)
            if (totalDiscount > baseBeforeDiscount.setScale(2, RoundingMode.HALF_UP)) {
                throw DomainRuleViolation("Line discount cannot be greater than line subtotal.")
            }

            val taxableBaseAmount = baseBeforeDiscount.subtract(totalDiscount).max(BigDecimal.ZERO)
            val taxAmount = if (profile.isTaxable) {
                taxableBaseAmount.multiply(snapshot.ratePercent)
                    .divide(BigDecimal("100"), INTERNAL_SCALE, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val taxableBase = Money.of(taxableBaseAmount, currency)
            val tax = Money.of(taxAmount, currency)

            TaxLineResult(
                lineId = line.lineId,
                description = line.description,
                snapshot = snapshot,
                grossAmount = Money.of(grossAmount(line), currency),
                discount = Money.of(totalDiscount, currency),
                taxableBase = taxableBase,
                taxAmount = tax,
                total = taxableBase + tax,
            )
        }

        return TaxCalculationResult(
            lines = results,
            summary = buildSummary(results, currency),
        )
    }

    private fun grossAmount(line: TaxLineInput): BigDecimal =
        line.unitPrice.amount.multiply(line.quantity.value)

    private fun preDiscountBase(line: TaxLineInput): BigDecimal {
        val profile = line.taxProfile ?: throw DomainRuleViolation("Tax profile is required for line ${line.lineId}.")
        val gross = grossAmount(line)
        val ratePercent = profile.rate?.ratePercent ?: BigDecimal.ZERO
        return if (line.priceIncludesTax && profile.isTaxable && ratePercent > BigDecimal.ZERO) {
            val divisor = BigDecimal.ONE.add(ratePercent.divide(BigDecimal("100"), INTERNAL_SCALE, RoundingMode.HALF_UP))
            gross.divide(divisor, INTERNAL_SCALE, RoundingMode.HALF_UP)
        } else {
            gross
        }
    }

    private fun proratedDiscount(lineBase: BigDecimal, subtotal: BigDecimal, discount: BigDecimal): BigDecimal {
        if (discount.compareTo(BigDecimal.ZERO) == 0 || subtotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return discount.multiply(lineBase).divide(subtotal, INTERNAL_SCALE, RoundingMode.HALF_UP)
    }

    private fun buildSummary(results: List<TaxLineResult>, currency: CurrencyCode): TaxSummary {
        var taxable = Money.zero(currency)
        var zero = Money.zero(currency)
        var exempt = Money.zero(currency)
        var notSubject = Money.zero(currency)
        var discount = Money.zero(currency)
        var tax = Money.zero(currency)
        var grandTotal = Money.zero(currency)

        results.forEach { line ->
            when (line.snapshot.treatment) {
                TaxTreatment.IVA_FULL,
                TaxTreatment.IVA_REDUCED_OR_SPECIAL -> taxable += line.taxableBase
                TaxTreatment.IVA_ZERO -> zero += line.taxableBase
                TaxTreatment.EXEMPT_IVA -> exempt += line.taxableBase
                TaxTreatment.NOT_SUBJECT_TO_IVA,
                TaxTreatment.NO_TAX_INTERNAL -> notSubject += line.taxableBase
            }
            discount += line.discount
            tax += line.taxAmount
            grandTotal += line.total
        }

        return TaxSummary(
            subtotalTaxable = taxable,
            subtotalZero = zero,
            subtotalExempt = exempt,
            subtotalNotSubject = notSubject,
            totalDiscount = discount,
            totalTax = tax,
            grandTotal = grandTotal,
        )
    }
}
