package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation

data class SaleItem private constructor(
    val id: String,
    val catalogItemId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Quantity,
    val discount: Money,
    val status: SaleItemStatus
) {

    val grossTotal: Money
        get() = unitPrice.multiply(quantity.value)

    val lineTotal: Money
        get() = grossTotal - discount

    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Sale item id cannot be blank.")
        }

        if (catalogItemId.isBlank()) {
            throw DomainRuleViolation("Catalog item id cannot be blank.")
        }

        if (name.isBlank()) {
            throw DomainRuleViolation("Sale item name cannot be blank.")
        }

        if (discount > grossTotal) {
            throw DomainRuleViolation("Sale item discount cannot be greater than gross total.")
        }
    }

    fun start(): SaleItem {
        if (status != SaleItemStatus.PENDING) {
            throw DomainRuleViolation("Only a pending sale item can be started.")
        }

        return copy(status = SaleItemStatus.IN_PROGRESS)
    }

    fun markReady(): SaleItem {
        if (status !in setOf(SaleItemStatus.PENDING, SaleItemStatus.IN_PROGRESS)) {
            throw DomainRuleViolation("Only pending or in-progress sale items can be marked ready.")
        }

        return copy(status = SaleItemStatus.READY)
    }

    fun deliver(): SaleItem {
        if (status !in setOf(SaleItemStatus.READY, SaleItemStatus.IN_PROGRESS)) {
            throw DomainRuleViolation("Only ready or in-progress sale items can be delivered.")
        }

        return copy(status = SaleItemStatus.DELIVERED)
    }

    fun cancel(): SaleItem {
        if (status == SaleItemStatus.DELIVERED) {
            throw DomainRuleViolation("Delivered sale items cannot be canceled.")
        }

        if (status == SaleItemStatus.CANCELED) {
            throw DomainRuleViolation("Sale item is already canceled.")
        }

        return copy(status = SaleItemStatus.CANCELED)
    }

    companion object {
        fun create(
            id: String,
            catalogItemId: String,
            name: String,
            unitPrice: Money,
            quantity: Quantity,
            discount: Money = Money.zero(unitPrice.currency)
        ): SaleItem {
            return SaleItem(
                id = id,
                catalogItemId = catalogItemId,
                name = name,
                unitPrice = unitPrice,
                quantity = quantity,
                discount = discount,
                status = SaleItemStatus.PENDING
            )
        }
    }
}
