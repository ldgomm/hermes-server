package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

@ConsistentCopyVisibility
data class CashSession private constructor(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val openedBy: String,
    val openedAt: Instant,
    val status: CashSessionStatus,
    val openingBalance: Money,
    val movements: List<CashMovement>,
    val closingStartedAt: Instant?,
    val closedAt: Instant?,
    val canceledAt: Instant?,
    val countedCashAmount: Money?,
    val differenceAmount: Money?,
    val closingNotes: String?,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Cash session id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Cash session organization id cannot be blank.")
        if (openedBy.isBlank()) throw DomainRuleViolation("Cash session openedBy cannot be blank.")

        countedCashAmount?.let {
            if (it.currency != openingBalance.currency) throw DomainRuleViolation("Counted cash currency must match opening balance currency.")
        }
        differenceAmount?.let {
            if (it.currency != openingBalance.currency) throw DomainRuleViolation("Cash difference currency must match opening balance currency.")
        }

        movements.forEach { movement ->
            if (movement.cashSessionId != id) throw DomainRuleViolation("Cash movement does not belong to this cash session.")
            if (movement.organizationId != organizationId) throw DomainRuleViolation("Cash movement does not belong to this organization.")
            if (branchId != null && movement.branchId != null && movement.branchId != branchId) {
                throw DomainRuleViolation("Cash movement does not belong to this branch.")
            }
        }
    }

    val expectedCashAmount: Money
        get() = movements.fold(openingBalance) { current, movement ->
            when (movement.direction) {
                CashMovementDirection.IN -> current + movement.amount
                CashMovementDirection.OUT -> current - movement.amount
                CashMovementDirection.NEUTRAL -> current
            }
        }

    fun recordMovement(movement: CashMovement): CashSession {
        CashSessionRules.assertCanRecordMovement(status)
        if (movement.cashSessionId != id) throw DomainRuleViolation("Cannot record movement for another cash session.")
        if (movement.organizationId != organizationId) throw DomainRuleViolation("Cannot record movement for another organization.")
        if (branchId != null && movement.branchId != null && movement.branchId != branchId) {
            throw DomainRuleViolation("Cannot record movement for another branch.")
        }
        if (movement.amount.currency != openingBalance.currency) {
            throw DomainRuleViolation("Cash movement currency must match cash session currency.")
        }
        return copy(movements = movements + movement)
    }

    fun startClosing(startedAt: Instant): CashSession {
        CashSessionRules.assertCanStartClosing(status)
        return copy(status = CashSessionStatus.CLOSING, closingStartedAt = startedAt)
    }

    fun reopenFromClosing(): CashSession {
        CashSessionRules.assertCanReopenFromClosing(status)
        return copy(status = CashSessionStatus.OPEN, closingStartedAt = null)
    }

    fun close(
        closedAt: Instant,
        countedCashAmount: Money? = null,
        closingNotes: String? = null,
    ): CashSession {
        CashSessionRules.assertCanClose(status)
        val counted = countedCashAmount
        val difference = counted?.let { countedAmount ->
            if (countedAmount >= expectedCashAmount) {
                countedAmount - expectedCashAmount
            } else {
                expectedCashAmount - countedAmount
            }
        }
        return copy(
            status = CashSessionStatus.CLOSED,
            closedAt = closedAt,
            countedCashAmount = counted,
            differenceAmount = difference,
            closingNotes = closingNotes?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    fun cancel(canceledAt: Instant): CashSession {
        CashSessionRules.assertCanCancel(status)
        return copy(status = CashSessionStatus.CANCELED, canceledAt = canceledAt)
    }

    companion object {
        fun open(
            id: String,
            organizationId: String,
            openedBy: String,
            openingBalance: Money,
            openedAt: Instant,
            branchId: String? = null,
        ): CashSession = CashSession(
            id = id.trim(),
            organizationId = organizationId.trim(),
            branchId = branchId?.trim()?.takeIf { it.isNotBlank() },
            openedBy = openedBy.trim(),
            openedAt = openedAt,
            status = CashSessionStatus.OPEN,
            openingBalance = openingBalance,
            movements = emptyList(),
            closingStartedAt = null,
            closedAt = null,
            canceledAt = null,
            countedCashAmount = null,
            differenceAmount = null,
            closingNotes = null,
        )

        fun restore(
            id: String,
            organizationId: String,
            branchId: String?,
            openedBy: String,
            openedAt: Instant,
            status: CashSessionStatus,
            openingBalance: Money,
            movements: List<CashMovement>,
            closingStartedAt: Instant?,
            closedAt: Instant?,
            canceledAt: Instant?,
            countedCashAmount: Money?,
            differenceAmount: Money?,
            closingNotes: String?,
        ): CashSession = CashSession(
            id = id.trim(),
            organizationId = organizationId.trim(),
            branchId = branchId?.trim()?.takeIf { it.isNotBlank() },
            openedBy = openedBy.trim(),
            openedAt = openedAt,
            status = status,
            openingBalance = openingBalance,
            movements = movements,
            closingStartedAt = closingStartedAt,
            closedAt = closedAt,
            canceledAt = canceledAt,
            countedCashAmount = countedCashAmount,
            differenceAmount = differenceAmount,
            closingNotes = closingNotes?.trim()?.takeIf { it.isNotBlank() },
        )
    }
}
