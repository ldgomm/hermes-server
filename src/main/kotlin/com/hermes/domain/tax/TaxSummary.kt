package com.hermes.domain.tax

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money

data class TaxSummary(
    val subtotalTaxable: Money,
    val subtotalZero: Money,
    val subtotalExempt: Money,
    val subtotalNotSubject: Money,
    val totalDiscount: Money,
    val totalTax: Money,
    val grandTotal: Money,
) {
    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.USD): TaxSummary = TaxSummary(
            subtotalTaxable = Money.zero(currency),
            subtotalZero = Money.zero(currency),
            subtotalExempt = Money.zero(currency),
            subtotalNotSubject = Money.zero(currency),
            totalDiscount = Money.zero(currency),
            totalTax = Money.zero(currency),
            grandTotal = Money.zero(currency),
        )
    }
}
