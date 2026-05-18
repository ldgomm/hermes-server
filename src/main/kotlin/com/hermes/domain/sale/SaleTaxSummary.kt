package com.hermes.domain.sale

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.math.BigDecimal

/**
 * Canonical sales tax summary for operational sales.
 *
 * This class does not replace the Tax Engine. It summarizes the immutable tax
 * snapshots already captured in SaleItem so that sales, future documents, reports
 * and Mongo projections can answer: which base/tax was applied per treatment and
 * per SRI code/rate at sale time.
 */
data class SaleTaxSummary(
    val currency: CurrencyCode,
    val subtotalTaxable: Money,
    val subtotalZeroRate: Money,
    val subtotalExempt: Money,
    val subtotalNotSubject: Money,
    val subtotalInternalNoTax: Money,
    val taxTotal: Money,
    val taxesByRate: List<SaleTaxSummaryRateLine>,
) {
    init {
        listOf(
            subtotalTaxable,
            subtotalZeroRate,
            subtotalExempt,
            subtotalNotSubject,
            subtotalInternalNoTax,
            taxTotal,
        ).forEach { money ->
            if (money.currency != currency) {
                throw DomainRuleViolation("Sale tax summary contains mixed currencies.")
            }
        }
        taxesByRate.forEach { rateLine ->
            if (rateLine.base.currency != currency || rateLine.taxAmount.currency != currency) {
                throw DomainRuleViolation("Sale tax summary rate line contains mixed currencies.")
            }
        }
    }

    val taxableBaseTotal: Money
        get() = subtotalTaxable + subtotalZeroRate + subtotalExempt + subtotalNotSubject + subtotalInternalNoTax

    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.USD): SaleTaxSummary =
            SaleTaxSummary(
                currency = currency,
                subtotalTaxable = Money.zero(currency),
                subtotalZeroRate = Money.zero(currency),
                subtotalExempt = Money.zero(currency),
                subtotalNotSubject = Money.zero(currency),
                subtotalInternalNoTax = Money.zero(currency),
                taxTotal = Money.zero(currency),
                taxesByRate = emptyList(),
            )

        fun fromItems(items: List<SaleItem>, fallbackCurrency: CurrencyCode = CurrencyCode.USD): SaleTaxSummary {
            val activeItems = items.filterNot { it.status == SaleItemStatus.CANCELED }
            val currency = activeItems.firstOrNull()?.unitPrice?.currency ?: fallbackCurrency
            activeItems.forEach { item ->
                if (item.unitPrice.currency != currency || item.discount.currency != currency) {
                    throw DomainRuleViolation("Cannot summarize sale taxes with mixed item currencies.")
                }
            }

            if (activeItems.isEmpty()) return zero(currency)

            fun zero() = Money.zero(currency)
            fun Iterable<Money>.sumMoney(): Money = fold(zero()) { current, value -> current + value }

            val taxRows = activeItems.flatMap { item -> item.toTaxSummaryRows(currency) }
            val taxesByRate = taxRows
                .groupBy { row -> row.key }
                .map { (key, rows) ->
                    SaleTaxSummaryRateLine(
                        taxCode = key.taxCode,
                        rateCode = key.rateCode,
                        rate = key.rate,
                        treatment = key.treatment,
                        base = rows.map { it.base }.sumMoney(),
                        taxAmount = rows.map { it.taxAmount }.sumMoney(),
                    )
                }
                .sortedWith(compareBy({ it.taxCode }, { it.rateCode }, { it.treatment.name }))

            return SaleTaxSummary(
                currency = currency,
                subtotalTaxable = taxRows.filter { it.key.treatment in positiveIvaTreatments }.map { it.base }.sumMoney(),
                subtotalZeroRate = taxRows.filter { it.key.treatment == TaxTreatment.IVA_ZERO }.map { it.base }.sumMoney(),
                subtotalExempt = taxRows.filter { it.key.treatment == TaxTreatment.EXEMPT_IVA }.map { it.base }.sumMoney(),
                subtotalNotSubject = taxRows.filter { it.key.treatment == TaxTreatment.NOT_SUBJECT_TO_IVA }.map { it.base }.sumMoney(),
                subtotalInternalNoTax = taxRows.filter { it.key.treatment == TaxTreatment.NO_TAX_INTERNAL }.map { it.base }.sumMoney(),
                taxTotal = taxesByRate.map { it.taxAmount }.sumMoney(),
                taxesByRate = taxesByRate,
            )
        }

        private val positiveIvaTreatments = setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL)
    }
}

data class SaleTaxSummaryRateLine(
    val taxCode: String,
    val rateCode: String,
    val rate: BigDecimal,
    val treatment: TaxTreatment,
    val base: Money,
    val taxAmount: Money,
) {
    init {
        if (taxCode.isBlank()) throw DomainRuleViolation("Sale tax summary tax code cannot be blank.")
        if (rateCode.isBlank()) throw DomainRuleViolation("Sale tax summary rate code cannot be blank.")
        if (rate < BigDecimal.ZERO) throw DomainRuleViolation("Sale tax summary rate cannot be negative.")
        if (base.currency != taxAmount.currency) {
            throw DomainRuleViolation("Sale tax summary base and tax amount must use the same currency.")
        }
    }
}

private data class SaleTaxSummaryRow(
    val key: SaleTaxSummaryRateKey,
    val base: Money,
    val taxAmount: Money,
)

private data class SaleTaxSummaryRateKey(
    val taxCode: String,
    val rateCode: String,
    val rate: BigDecimal,
    val treatment: TaxTreatment,
)

private fun SaleItem.toTaxSummaryRows(currency: CurrencyCode): List<SaleTaxSummaryRow> {
    if (taxes.isEmpty()) {
        val base = netTotal
        if (base.currency != currency) throw DomainRuleViolation("Sale item tax base currency must match sale currency.")
        return listOf(
            SaleTaxSummaryRow(
                key = SaleTaxSummaryRateKey(
                    taxCode = taxProfileSnapshot.sriTaxCode,
                    rateCode = taxProfileSnapshot.sriRateCode,
                    rate = taxProfileSnapshot.rate.value,
                    treatment = taxProfileSnapshot.treatment,
                ),
                base = base,
                taxAmount = Money.zero(currency),
            )
        )
    }

    return taxes.map { tax ->
        if (tax.taxableBase.currency != currency || tax.amount.currency != currency) {
            throw DomainRuleViolation("Sale item tax currency must match sale currency.")
        }
        SaleTaxSummaryRow(
            key = SaleTaxSummaryRateKey(
                taxCode = tax.taxCode,
                rateCode = tax.rateCode,
                rate = tax.rate.value,
                treatment = taxProfileSnapshot.treatment,
            ),
            base = tax.taxableBase,
            taxAmount = tax.amount,
        )
    }
}
