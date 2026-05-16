package com.hermes.domain.sale

import com.hermes.domain.shared.StateTransitionValidator

object SaleItemStateMachine {
    private val validator = StateTransitionValidator(
        entityName = "sale item",
        transitions = mapOf(
            SaleItemStatus.PENDING to setOf(
                SaleItemStatus.IN_PROGRESS,
                SaleItemStatus.READY,
                SaleItemStatus.CANCELED,
            ),
            SaleItemStatus.IN_PROGRESS to setOf(
                SaleItemStatus.READY,
                SaleItemStatus.DELIVERED,
                SaleItemStatus.CANCELED,
            ),
            SaleItemStatus.READY to setOf(
                SaleItemStatus.DELIVERED,
                SaleItemStatus.CANCELED,
            ),
            SaleItemStatus.DELIVERED to emptySet(),
            SaleItemStatus.CANCELED to emptySet(),
        ),
    )

    fun canTransition(from: SaleItemStatus, to: SaleItemStatus): Boolean =
        validator.canTransition(from, to)

    fun assertCanTransition(from: SaleItemStatus, to: SaleItemStatus) {
        validator.assertCanTransition(from, to)
    }
}
