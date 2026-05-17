package com.hermes.domain.tax

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation

data class TaxLineInput(
    val lineId: String,
    val description: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val discount: Money = Money.zero(unitPrice.currency),
    val taxProfileSnapshot: TaxProfileSnapshot,
    val priceTaxMode: PriceTaxMode = PriceTaxMode.TAX_EXCLUSIVE,
) {
    init {
        if (lineId.isBlank()) throw DomainRuleViolation("Tax line id cannot be blank.")
        if (description.isBlank()) throw DomainRuleViolation("Tax line description cannot be blank.")
        if (unitPrice.currency != discount.currency) {
            throw DomainRuleViolation("Tax line unit price and discount must use the same currency.")
        }
    }

    val currency: CurrencyCode get() = unitPrice.currency
}
