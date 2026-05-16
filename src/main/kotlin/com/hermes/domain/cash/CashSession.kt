package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class CashSession private constructor(
    val id: String,
    val organizationId: String,
    val openedBy: String,
    val openedAt: Instant,
    val status: CashSessionStatus,
    val openingBalance: Money,
    val movements: List<CashMovement>,
    val closingStartedAt: Instant?,
    val closedAt: Instant?,
    val canceledAt: Instant?
) {

    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Cash session id cannot be blank.")
        }

        if (organizationId.isBlank()) {
            throw DomainRuleViolation("Cash session organization id cannot be blank.")
        }

        if (openedBy.isBlank()) {
            throw DomainRuleViolation("Cash session openedBy cannot be blank.")
        }

        movements.forEach { movement ->
            if (movement.cashSessionId != id) {
                throw DomainRuleViolation("Cash movement does not belong to this cash session.")
            }

            if (movement.organizationId != organizationId) {
                throw DomainRuleViolation("Cash movement does not belong to this organization.")
            }
        }
    }

    val expectedCashAmount: Money
        get() {
            return movements.fold(openingBalance) { current, movement ->
                when (movement.direction) {
                    CashMovementDirection.IN -> current + movement.amount
                    CashMovementDirection.OUT -> current - movement.amount
                    CashMovementDirection.NEUTRAL -> current
                }
            }
        }

    fun recordMovement(movement: CashMovement): CashSession {
        CashSessionRules.assertCanRecordMovement(status)

        if (movement.cashSessionId != id) {
            throw DomainRuleViolation("Cannot record movement for another cash session.")
        }

        if (movement.organizationId != organizationId) {
            throw DomainRuleViolation("Cannot record movement for another organization.")
        }

        if (movement.amount.currency != openingBalance.currency) {
            throw DomainRuleViolation("Cash movement currency must match cash session currency.")
        }

        return copy(movements = movements + movement)
    }

    fun startClosing(startedAt: Instant): CashSession {
        CashSessionRules.assertCanStartClosing(status)

        return copy(
            status = CashSessionStatus.CLOSING,
            closingStartedAt = startedAt
        )
    }

    fun reopenFromClosing(): CashSession {
        CashSessionRules.assertCanReopenFromClosing(status)

        return copy(
            status = CashSessionStatus.OPEN,
            closingStartedAt = null
        )
    }

    fun close(closedAt: Instant): CashSession {
        CashSessionRules.assertCanClose(status)

        return copy(
            status = CashSessionStatus.CLOSED,
            closedAt = closedAt
        )
    }

    fun cancel(canceledAt: Instant): CashSession {
        CashSessionRules.assertCanCancel(status)

        return copy(
            status = CashSessionStatus.CANCELED,
            canceledAt = canceledAt
        )
    }

    companion object {
        fun open(
            id: String,
            organizationId: String,
            openedBy: String,
            openingBalance: Money,
            openedAt: Instant
        ): CashSession {
            return CashSession(
                id = id,
                organizationId = organizationId,
                openedBy = openedBy,
                openedAt = openedAt,
                status = CashSessionStatus.OPEN,
                openingBalance = openingBalance,
                movements = emptyList(),
                closingStartedAt = null,
                closedAt = null,
                canceledAt = null
            )
        }
    }
}
