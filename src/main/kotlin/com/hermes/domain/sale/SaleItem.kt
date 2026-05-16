package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation

@ConsistentCopyVisibility
data class SaleItem private constructor(
    val id: String,
    val catalogItemId: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Quantity,
    val discount: Money,
    val status: SaleItemStatus,
    val catalogSnapshot: CatalogItemSnapshot,
    val taxProfileSnapshot: TaxProfileSnapshotForSale,
    val taxes: List<SaleItemTax>,
) {
    val grossTotal: Money
        get() = unitPrice.multiply(quantity.value)

    val netTotal: Money
        get() = grossTotal - discount

    val taxTotal: Money
        get() = taxes.fold(Money.zero(unitPrice.currency)) { current, tax -> current + tax.amount }

    val lineTotal: Money
        get() = netTotal + taxTotal

    init {
        if (id.isBlank()) throw DomainRuleViolation("Sale item id cannot be blank.")
        if (catalogItemId.isBlank()) throw DomainRuleViolation("Catalog item id cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Sale item name cannot be blank.")
        if (catalogSnapshot.catalogItemId != catalogItemId) {
            throw DomainRuleViolation("Sale item catalog snapshot must match catalog item id.")
        }
        if (catalogSnapshot.name.isBlank()) {
            throw DomainRuleViolation("Sale item catalog snapshot name cannot be blank.")
        }
        if (discount.currency != unitPrice.currency) {
            throw DomainRuleViolation("Sale item discount currency must match unit price currency.")
        }
        if (discount > grossTotal) {
            throw DomainRuleViolation("Sale item discount cannot be greater than gross total.")
        }
        taxes.forEach { tax ->
            if (tax.amount.currency != unitPrice.currency || tax.taxableBase.currency != unitPrice.currency) {
                throw DomainRuleViolation("Sale item tax currency must match unit price currency.")
            }
        }
    }

    fun start(): SaleItem {
        SaleItemStateMachine.assertCanTransition(status, SaleItemStatus.IN_PROGRESS)
        return copy(status = SaleItemStatus.IN_PROGRESS)
    }

    fun markReady(): SaleItem {
        SaleItemStateMachine.assertCanTransition(status, SaleItemStatus.READY)
        return copy(status = SaleItemStatus.READY)
    }

    fun deliver(): SaleItem {
        SaleItemStateMachine.assertCanTransition(status, SaleItemStatus.DELIVERED)
        return copy(status = SaleItemStatus.DELIVERED)
    }

    fun cancel(): SaleItem {
        SaleItemStateMachine.assertCanTransition(status, SaleItemStatus.CANCELED)
        return copy(status = SaleItemStatus.CANCELED)
    }

    companion object {
        fun create(
            id: String,
            catalogItemId: String,
            name: String,
            unitPrice: Money,
            quantity: Quantity,
            discount: Money = Money.zero(unitPrice.currency),
            catalogSnapshot: CatalogItemSnapshot,
            taxProfileSnapshot: TaxProfileSnapshotForSale,
            taxes: List<SaleItemTax> = emptyList(),
        ): SaleItem {
            return SaleItem(
                id = id,
                catalogItemId = catalogItemId,
                name = name,
                unitPrice = unitPrice,
                quantity = quantity,
                discount = discount,
                status = SaleItemStatus.PENDING,
                catalogSnapshot = catalogSnapshot,
                taxProfileSnapshot = taxProfileSnapshot,
                taxes = taxes,
            )
        }
    }
}
