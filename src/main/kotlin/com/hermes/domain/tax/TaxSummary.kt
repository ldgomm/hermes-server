package com.hermes.domain.tax

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

data class TaxSummary(
    val currency: CurrencyCode,
    val grossSubtotal: Money,
    val totalDiscount: Money,
    val subtotalTaxable: Money,
    val subtotalZeroRate: Money,
    val subtotalExempt: Money,
    val subtotalNotSubject: Money,
    val subtotalInternalNoTax: Money,
    val totalTax: Money,
    val grandTotal: Money,
    val taxesByRate: List<TaxSummaryRateLine>,
) {
    init {
        listOf(
            grossSubtotal,
            totalDiscount,
            subtotalTaxable,
            subtotalZeroRate,
            subtotalExempt,
            subtotalNotSubject,
            subtotalInternalNoTax,
            totalTax,
            grandTotal,
        ).forEach {
            if (it.currency != currency) throw DomainRuleViolation("Tax summary contains mixed currencies.")
        }
    }

    companion object {
        fun from(lines: List<TaxLineResult>): TaxSummary {
            if (lines.isEmpty()) throw DomainRuleViolation("Tax summary requires at least one line.")
            val currency = lines.first().total.currency
            lines.forEach { line ->
                if (line.total.currency != currency) throw DomainRuleViolation("Cannot summarize tax lines with mixed currencies.")
            }

            fun zero() = Money.zero(currency)
            fun Iterable<Money>.total(): Money = fold(zero()) { acc, value -> acc + value }

            val taxesByRate = lines
                .groupBy { RateKey.from(it.taxProfileSnapshot) }
                .map { (key, grouped) ->
                    TaxSummaryRateLine(
                        sriTaxCode = key.sriTaxCode,
                        sriRateCode = key.sriRateCode,
                        taxKind = key.taxKind,
                        rate = key.rate,
                        treatment = key.treatment,
                        base = grouped.map { it.baseForSri }.total(),
                        taxAmount = grouped.map { it.taxAmount }.total(),
                    )
                }
                .sortedWith(compareBy({ it.sriTaxCode ?: "" }, { it.sriRateCode ?: "" }, { it.treatment.name }))

            return TaxSummary(
                currency = currency,
                grossSubtotal = lines.map { it.grossAmount }.total(),
                totalDiscount = lines.map { it.discount }.total(),
                subtotalTaxable = lines.map { it.taxableBase }.total(),
                subtotalZeroRate = lines.map { it.zeroRateBase }.total(),
                subtotalExempt = lines.map { it.exemptBase }.total(),
                subtotalNotSubject = lines.map { it.notSubjectBase }.total(),
                subtotalInternalNoTax = lines.map { it.internalNoTaxBase }.total(),
                totalTax = lines.map { it.taxAmount }.total(),
                grandTotal = lines.map { it.total }.total(),
                taxesByRate = taxesByRate,
            )
        }
    }
}

data class TaxSummaryRateLine(
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val taxKind: TaxKind?,
    val rate: java.math.BigDecimal,
    val treatment: TaxTreatment,
    val base: Money,
    val taxAmount: Money,
)

private data class RateKey(
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val taxKind: TaxKind?,
    val rate: java.math.BigDecimal,
    val treatment: TaxTreatment,
) {
    companion object {
        fun from(snapshot: TaxProfileSnapshot): RateKey = RateKey(
            sriTaxCode = snapshot.sriTaxCode,
            sriRateCode = snapshot.sriRateCode,
            taxKind = snapshot.taxKind,
            rate = snapshot.rate,
            treatment = snapshot.treatment,
        )
    }
}
