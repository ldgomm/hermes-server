package com.hermes.domain.sale

import com.hermes.domain.shared.StateTransitionValidator

object SaleOperationalStateMachine {

    private val validator = StateTransitionValidator(
        entityName = "sale",
        transitions = mapOf(
            SaleOperationalStatus.DRAFT to setOf(
                SaleOperationalStatus.PENDING,
                SaleOperationalStatus.CONFIRMED,
                SaleOperationalStatus.CANCELED
            ),
            SaleOperationalStatus.PENDING to setOf(
                SaleOperationalStatus.CONFIRMED,
                SaleOperationalStatus.CANCELED
            ),
            SaleOperationalStatus.CONFIRMED to setOf(
                SaleOperationalStatus.IN_PROGRESS,
                SaleOperationalStatus.READY,
                SaleOperationalStatus.DELIVERED,
                SaleOperationalStatus.CANCELED
            ),
            SaleOperationalStatus.IN_PROGRESS to setOf(
                SaleOperationalStatus.READY,
                SaleOperationalStatus.CANCELED
            ),
            SaleOperationalStatus.READY to setOf(
                SaleOperationalStatus.DELIVERED,
                SaleOperationalStatus.CANCELED
            ),
            SaleOperationalStatus.DELIVERED to setOf(
                SaleOperationalStatus.CLOSED
            ),
            SaleOperationalStatus.CLOSED to emptySet(),
            SaleOperationalStatus.CANCELED to emptySet()
        )
    )

    fun canTransition(from: SaleOperationalStatus, to: SaleOperationalStatus): Boolean {
        return validator.canTransition(from, to)
    }

    fun assertCanTransition(from: SaleOperationalStatus, to: SaleOperationalStatus) {
        validator.assertCanTransition(from, to)
    }
}
