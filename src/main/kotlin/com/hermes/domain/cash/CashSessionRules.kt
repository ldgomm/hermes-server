package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

object CashSessionRules {

    fun assertCanRecordMovement(status: CashSessionStatus) {
        when (status) {
            CashSessionStatus.OPEN -> Unit
            CashSessionStatus.CLOSING -> {
                throw DomainRuleViolation("Cannot record cash movements while cash session is closing.")
            }
            CashSessionStatus.CLOSED -> {
                throw DomainRuleViolation("Cannot record cash movements in a closed cash session.")
            }
            CashSessionStatus.CANCELED -> {
                throw DomainRuleViolation("Cannot record cash movements in a canceled cash session.")
            }
        }
    }

    fun assertCanStartClosing(status: CashSessionStatus) {
        if (status != CashSessionStatus.OPEN) {
            throw DomainRuleViolation("Only an open cash session can start closing.")
        }

        CashSessionStateMachine.assertCanTransition(
            from = status,
            to = CashSessionStatus.CLOSING
        )
    }

    fun assertCanReopenFromClosing(status: CashSessionStatus) {
        if (status != CashSessionStatus.CLOSING) {
            throw DomainRuleViolation("Only a closing cash session can be reopened.")
        }

        CashSessionStateMachine.assertCanTransition(
            from = status,
            to = CashSessionStatus.OPEN
        )
    }

    fun assertCanClose(status: CashSessionStatus) {
        if (status != CashSessionStatus.CLOSING) {
            throw DomainRuleViolation("Only a closing cash session can be closed.")
        }

        CashSessionStateMachine.assertCanTransition(
            from = status,
            to = CashSessionStatus.CLOSED
        )
    }

    fun assertCanCancel(status: CashSessionStatus) {
        if (status !in setOf(CashSessionStatus.OPEN, CashSessionStatus.CLOSING)) {
            throw DomainRuleViolation("Only an open or closing cash session can be canceled.")
        }

        CashSessionStateMachine.assertCanTransition(
            from = status,
            to = CashSessionStatus.CANCELED
        )
    }

    fun assertValidMovement(
        type: CashMovementType,
        direction: CashMovementDirection,
        amount: Money
    ) {
        if (amount.amount.signum() <= 0) {
            throw DomainRuleViolation("Cash movement amount must be greater than zero.")
        }

        when (type) {
            CashMovementType.OPENING_BALANCE -> {
                if (direction !in setOf(CashMovementDirection.IN, CashMovementDirection.NEUTRAL)) {
                    throw DomainRuleViolation("Opening balance must be IN or NEUTRAL.")
                }
            }

            CashMovementType.SALE_PAYMENT,
            CashMovementType.MANUAL_INCOME -> {
                if (direction != CashMovementDirection.IN) {
                    throw DomainRuleViolation("$type must be an IN cash movement.")
                }
            }

            CashMovementType.MANUAL_EXPENSE,
            CashMovementType.WITHDRAWAL,
            CashMovementType.REFUND -> {
                if (direction != CashMovementDirection.OUT) {
                    throw DomainRuleViolation("$type must be an OUT cash movement.")
                }
            }

            CashMovementType.ADJUSTMENT -> {
                if (direction == CashMovementDirection.NEUTRAL) {
                    throw DomainRuleViolation("Adjustment must be IN or OUT.")
                }
            }
        }
    }
}
