package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleTotals
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Fase 8.4 — Persistence boundary guard for sales.
 *
 * The Sale aggregate calculates totals from active items, but every persistence
 * boundary must still fail fast if a sale is about to be stored with an invalid
 * financial/tax shape. This keeps sales safe before Fase 9 adds payments/cash
 * transactions and before commercial documents consume sale snapshots.
 */
class SalePersistenceGuard {
    fun assertReadyToPersist(sale: Sale) {
        assertHasItemsWhenRequired(sale)
        assertItemFinancialConsistency(sale)
        assertSaleTotalsConsistency(sale)
        assertPaymentDoesNotExceedTotal(sale)
    }

    private fun assertHasItemsWhenRequired(sale: Sale) {
        if (sale.operationalStatus == SaleOperationalStatus.CANCELED) return
        if (sale.activeItems.isEmpty()) {
            throw DomainRuleViolation("Sale requires at least one active item before it can be persisted.")
        }
    }

    private fun assertItemFinancialConsistency(sale: Sale) {
        sale.activeItems.forEach { item ->
            assertItemHasExpectedTaxShape(item)
            assertMoney("item gross total", item.grossTotal)
            assertMoney("item net total", item.netTotal)
            assertMoney("item tax total", item.taxTotal)
            assertMoney("item line total", item.lineTotal)

            val expectedTaxTotal = item.taxes.fold(Money.zero(item.unitPrice.currency)) { current, tax ->
                current + tax.amount
            }
            if (item.taxTotal != expectedTaxTotal) {
                throw DomainRuleViolation("Sale item tax total does not match its tax lines.")
            }

            val expectedLineTotal = item.netTotal + item.taxTotal
            if (item.lineTotal != expectedLineTotal) {
                throw DomainRuleViolation("Sale item line total does not match net total plus tax total.")
            }
        }
    }

    private fun assertItemHasExpectedTaxShape(item: SaleItem) {
        if (item.taxProfileSnapshot.code.isBlank()) {
            throw DomainRuleViolation("Sale item tax profile snapshot code cannot be blank.")
        }
        if (item.taxProfileSnapshot.taxName.isBlank()) {
            throw DomainRuleViolation("Sale item tax profile snapshot tax name cannot be blank.")
        }

        val taxablePositiveRate = item.taxProfileSnapshot.rate.value.signum() > 0 &&
            item.taxProfileSnapshot.treatment.name in taxableTreatments

        if (taxablePositiveRate && item.taxes.isEmpty()) {
            throw DomainRuleViolation("Taxed sale items require at least one tax line.")
        }
    }

    private fun assertSaleTotalsConsistency(sale: Sale) {
        val rebuilt = SaleTotals.fromItems(sale.activeItems)
        val current = sale.totals

        if (rebuilt.subtotal != current.subtotal) {
            throw DomainRuleViolation("Sale subtotal does not match active items.")
        }
        if (rebuilt.discount != current.discount) {
            throw DomainRuleViolation("Sale discount total does not match active items.")
        }
        if (rebuilt.taxTotal != current.taxTotal) {
            throw DomainRuleViolation("Sale tax total does not match active items.")
        }
        if (rebuilt.grandTotal != current.grandTotal) {
            throw DomainRuleViolation("Sale grand total does not match active items.")
        }
    }

    private fun assertPaymentDoesNotExceedTotal(sale: Sale) {
        if (sale.operationalStatus == SaleOperationalStatus.CANCELED) return
        if (sale.paidAmount > sale.total) {
            throw DomainRuleViolation("Sale paid amount cannot exceed sale total before overpayment support exists.")
        }
    }

    private fun assertMoney(label: String, money: Money) {
        if (money.amount.scale() > 2) {
            throw DomainRuleViolation("$label cannot have more than 2 decimal places.")
        }
    }

    companion object {
        private val taxableTreatments = setOf("IVA_FULL", "IVA_REDUCED_OR_SPECIAL")
    }
}
