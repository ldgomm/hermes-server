package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity

data class TaxLineInput(
    val lineId: String,
    val description: String,
    val unitPrice: Money,
    val quantity: Quantity,
    val taxProfile: TaxProfile?,
    val discount: Money = Money.zero(unitPrice.currency),
    val priceIncludesTax: Boolean = false,
    val manualDiscountAllowed: Boolean = true,
) {
    init {
        require(lineId.isNotBlank()) { "Tax line id cannot be blank." }
        require(description.isNotBlank()) { "Tax line description cannot be blank." }
    }
}
