package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.shared.DomainRuleViolation

data class SaleItemTax(
    val taxCode: String,
    val rateCode: String,
    val rate: Percentage,
    val taxableBase: Money,
    val amount: Money,
) {
    init {
        if (taxCode.isBlank()) throw DomainRuleViolation("Sale item tax code cannot be blank.")
        if (rateCode.isBlank()) throw DomainRuleViolation("Sale item tax rate code cannot be blank.")
        if (taxableBase.currency != amount.currency) {
            throw DomainRuleViolation("Sale item tax base and amount must use the same currency.")
        }
    }
}
