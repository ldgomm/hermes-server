package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

@ConsistentCopyVisibility
data class CashMovement private constructor(
    val id: String,
    val cashSessionId: String,
    val organizationId: String,
    val branchId: String?,
    val type: CashMovementType,
    val direction: CashMovementDirection,
    val amount: Money,
    val occurredAt: Instant,
    val referenceId: String?,
    val notes: String?,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Cash movement id cannot be blank.")
        if (cashSessionId.isBlank()) throw DomainRuleViolation("Cash movement cash session id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Cash movement organization id cannot be blank.")
        CashSessionRules.assertValidMovement(type = type, direction = direction, amount = amount)
    }

    companion object {
        fun create(
            id: String,
            cashSessionId: String,
            organizationId: String,
            type: CashMovementType,
            direction: CashMovementDirection,
            amount: Money,
            occurredAt: Instant,
            referenceId: String? = null,
            notes: String? = null,
            branchId: String? = null,
        ): CashMovement = CashMovement(
            id = id.trim(),
            cashSessionId = cashSessionId.trim(),
            organizationId = organizationId.trim(),
            branchId = branchId?.trim()?.takeIf { it.isNotBlank() },
            type = type,
            direction = direction,
            amount = amount,
            occurredAt = occurredAt,
            referenceId = referenceId?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
        )

        fun restore(
            id: String,
            cashSessionId: String,
            organizationId: String,
            branchId: String?,
            type: CashMovementType,
            direction: CashMovementDirection,
            amount: Money,
            occurredAt: Instant,
            referenceId: String?,
            notes: String?,
        ): CashMovement = CashMovement(
            id = id.trim(),
            cashSessionId = cashSessionId.trim(),
            organizationId = organizationId.trim(),
            branchId = branchId?.trim()?.takeIf { it.isNotBlank() },
            type = type,
            direction = direction,
            amount = amount,
            occurredAt = occurredAt,
            referenceId = referenceId?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
        )
    }
}
