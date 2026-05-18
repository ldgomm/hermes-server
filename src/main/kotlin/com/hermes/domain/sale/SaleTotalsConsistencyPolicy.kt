package com.hermes.domain.sale

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Defensive consistency checks for totals and tax snapshots captured in a Sale.
 *
 * Use this after building/restoring a Sale and before exposing it to documents,
 * payment/cash flows or reports. It intentionally does not recalculate tax rates;
 * that responsibility remains in Tax Engine. Here we verify the sale aggregate is
 * internally coherent.
 */
object SaleTotalsConsistencyPolicy {
    fun assertConsistent(sale: Sale) {
        val expectedTotals = SaleTotals.fromItems(sale.items, sale.totals.currency)
        assertTotalsEqual(expectedTotals, sale.totals)
        assertTaxSummaryMatchesTotals(sale.items, sale.totals.currency, sale.totals.taxTotal)
        assertPaidAndReceivableAreCoherent(sale)
    }

    fun expectedTaxSummary(sale: Sale): SaleTaxSummary =
        SaleTaxSummary.fromItems(sale.items, sale.totals.currency)

    private fun assertTotalsEqual(expected: SaleTotals, actual: SaleTotals) {
        if (expected.currency != actual.currency) {
            throw DomainRuleViolation("Sale totals currency is inconsistent.")
        }
        assertMoneyEquals("subtotal", expected.subtotal, actual.subtotal)
        assertMoneyEquals("discount", expected.discount, actual.discount)
        assertMoneyEquals("taxTotal", expected.taxTotal, actual.taxTotal)
        assertMoneyEquals("grandTotal", expected.grandTotal, actual.grandTotal)
    }

    private fun assertTaxSummaryMatchesTotals(items: List<SaleItem>, currency: CurrencyCode, saleTaxTotal: Money) {
        val taxSummary = SaleTaxSummary.fromItems(items, currency)
        assertMoneyEquals("tax summary total", saleTaxTotal, taxSummary.taxTotal)
    }

    private fun assertPaidAndReceivableAreCoherent(sale: Sale) {
        if (sale.paidAmount.currency != sale.totals.currency) {
            throw DomainRuleViolation("Sale paid amount currency must match sale totals currency.")
        }
        if (sale.paidAmount > sale.totals.grandTotal) {
            throw DomainRuleViolation("Sale paid amount cannot be greater than grand total before overpayment support exists.")
        }
        // This subtraction also proves paidAmount <= grandTotal with Money's own invariant.
        val receivable = sale.totals.grandTotal - sale.paidAmount
        if (receivable.currency != sale.totals.currency) {
            throw DomainRuleViolation("Sale receivable amount currency must match sale totals currency.")
        }
    }

    private fun assertMoneyEquals(label: String, expected: Money, actual: Money) {
        if (expected.currency != actual.currency || expected.amount.compareTo(actual.amount) != 0) {
            throw DomainRuleViolation(
                "Sale $label is inconsistent. Expected ${expected.amount.toPlainString()} ${expected.currency.value}, " +
                    "got ${actual.amount.toPlainString()} ${actual.currency.value}."
            )
        }
    }
}
