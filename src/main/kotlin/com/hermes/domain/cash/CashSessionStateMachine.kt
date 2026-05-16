package com.hermes.domain.cash

import com.hermes.domain.shared.StateTransitionValidator

object CashSessionStateMachine {

    private val validator = StateTransitionValidator(
        entityName = "cash session",
        transitions = mapOf(
            CashSessionStatus.OPEN to setOf(
                CashSessionStatus.CLOSING,
                CashSessionStatus.CANCELED
            ),
            CashSessionStatus.CLOSING to setOf(
                CashSessionStatus.CLOSED,
                CashSessionStatus.OPEN,
                CashSessionStatus.CANCELED
            ),
            CashSessionStatus.CLOSED to emptySet(),
            CashSessionStatus.CANCELED to emptySet()
        )
    )

    fun canTransition(
        from: CashSessionStatus,
        to: CashSessionStatus
    ): Boolean {
        return validator.canTransition(from, to)
    }

    fun assertCanTransition(
        from: CashSessionStatus,
        to: CashSessionStatus
    ) {
        validator.assertCanTransition(from, to)
    }
}
