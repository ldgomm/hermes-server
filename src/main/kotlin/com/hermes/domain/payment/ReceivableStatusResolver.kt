package com.hermes.domain.payment

import com.hermes.domain.money.Money
import java.time.Instant

object ReceivableStatusResolver {
    fun resolve(
        totalDue: Money,
        paidAmount: Money,
        dueAt: Instant?,
        now: Instant,
        isCanceled: Boolean = false,
        isWrittenOff: Boolean = false,
    ): ReceivableStatus {
        if (isCanceled) return ReceivableStatus.CANCELED
        if (isWrittenOff) return ReceivableStatus.WRITTEN_OFF
        if (paidAmount >= totalDue) return ReceivableStatus.SETTLED
        if (dueAt == null) return ReceivableStatus.NOT_APPLICABLE
        if (dueAt.isBefore(now)) return ReceivableStatus.OVERDUE
        if (paidAmount.amount.signum() > 0) return ReceivableStatus.PARTIALLY_COLLECTED
        return ReceivableStatus.PENDING_RECEIVABLE
    }
}
