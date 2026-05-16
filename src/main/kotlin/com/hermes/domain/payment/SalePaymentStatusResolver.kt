package com.hermes.domain.payment

import com.hermes.domain.money.Money

object SalePaymentStatusResolver {
    fun resolve(
        totalDue: Money,
        paidAmount: Money,
        isVoided: Boolean = false,
        isRefunded: Boolean = false,
    ): SalePaymentStatus {
        if (isVoided) return SalePaymentStatus.VOIDED
        if (isRefunded) return SalePaymentStatus.REFUNDED
        if (paidAmount.amount.signum() == 0) return SalePaymentStatus.UNPAID
        if (paidAmount < totalDue) return SalePaymentStatus.PARTIALLY_PAID
        if (paidAmount == totalDue) return SalePaymentStatus.PAID
        return SalePaymentStatus.OVERPAID
    }
}
