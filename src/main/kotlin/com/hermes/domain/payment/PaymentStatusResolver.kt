package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

object PaymentStatusResolver {

    fun resolve(
        totalDue: Money,
        paidAmount: Money,
        isVoided: Boolean = false,
        isRefunded: Boolean = false
    ): PaymentStatus {
        if (isVoided && isRefunded) {
            throw DomainRuleViolation("Payment cannot be voided and refunded at the same time.")
        }

        if (isVoided) return PaymentStatus.VOIDED
        if (isRefunded) return PaymentStatus.REFUNDED

        val zero = Money.zero(totalDue.currency)

        return when {
            totalDue == zero && paidAmount == zero -> PaymentStatus.PAID
            paidAmount == zero -> PaymentStatus.UNPAID
            paidAmount < totalDue -> PaymentStatus.PARTIALLY_PAID
            paidAmount == totalDue -> PaymentStatus.PAID
            paidAmount > totalDue -> PaymentStatus.OVERPAID
            else -> throw DomainRuleViolation("Unable to resolve payment status.")
        }
    }
}
