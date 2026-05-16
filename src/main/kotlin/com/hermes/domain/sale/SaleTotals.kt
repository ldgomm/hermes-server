package com.hermes.domain.sale

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

data class SaleTotals(
    val subtotal: Money,
    val discount: Money,
    val taxTotal: Money,
    val grandTotal: Money,
    val currency: CurrencyCode,
) {
    init {
        val amounts = listOf(subtotal, discount, taxTotal, grandTotal)
        if (amounts.any { it.currency != currency }) {
            throw DomainRuleViolation("All sale totals must use the sale currency.")
        }
    }

    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.USD): SaleTotals =
            SaleTotals(
                subtotal = Money.zero(currency),
                discount = Money.zero(currency),
                taxTotal = Money.zero(currency),
                grandTotal = Money.zero(currency),
                currency = currency,
            )

        fun fromItems(items: List<SaleItem>, fallbackCurrency: CurrencyCode = CurrencyCode.USD): SaleTotals {
            val activeItems = items.filterNot { it.status == SaleItemStatus.CANCELED }
            val currency = activeItems.firstOrNull()?.unitPrice?.currency ?: fallbackCurrency
            return activeItems.fold(zero(currency)) { current, item ->
                SaleTotals(
                    subtotal = current.subtotal + item.grossTotal,
                    discount = current.discount + item.discount,
                    taxTotal = current.taxTotal + item.taxTotal,
                    grandTotal = current.grandTotal + item.lineTotal,
                    currency = currency,
                )
            }
        }
    }
}
