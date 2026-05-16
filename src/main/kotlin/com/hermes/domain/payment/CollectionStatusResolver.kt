package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

object CollectionStatusResolver {

    fun resolve(
        totalDue: Money,
        paidAmount: Money,
        dueAt: Instant? = null,
        now: Instant,
        isVoided: Boolean = false,
        isWrittenOff: Boolean = false
    ): CollectionStatus {
        if (isVoided && isWrittenOff) {
            throw DomainRuleViolation("Collection cannot be voided and written off at the same time.")
        }

        if (isVoided) return CollectionStatus.VOIDED
        if (isWrittenOff) return CollectionStatus.WRITTEN_OFF

        val zero = Money.zero(totalDue.currency)

        if (totalDue == zero) {
            return CollectionStatus.NOT_REQUIRED
        }

        if (paidAmount >= totalDue) {
            return CollectionStatus.COLLECTED
        }

        if (paidAmount > zero) {
            return CollectionStatus.PARTIALLY_COLLECTED
        }

        if (dueAt != null && dueAt.isBefore(now)) {
            return CollectionStatus.OVERDUE
        }

        return CollectionStatus.PENDING
    }
}
