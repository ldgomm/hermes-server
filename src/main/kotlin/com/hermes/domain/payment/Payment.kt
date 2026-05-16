package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class Payment private constructor(
    val id: String,
    val organizationId: String,
    val saleId: String,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val paidAt: Instant,
    val reference: String?,
    val notes: String?
) {

    val isEffective: Boolean
        get() = status == PaymentStatus.PAID

    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Payment id cannot be blank.")
        }

        if (organizationId.isBlank()) {
            throw DomainRuleViolation("Payment organization id cannot be blank.")
        }

        if (saleId.isBlank()) {
            throw DomainRuleViolation("Payment sale id cannot be blank.")
        }

        if (amount.amount.signum() <= 0) {
            throw DomainRuleViolation("Payment amount must be greater than zero.")
        }
    }

    fun void(): Payment {
        if (status == PaymentStatus.REFUNDED) {
            throw DomainRuleViolation("Refunded payment cannot be voided.")
        }

        if (status == PaymentStatus.VOIDED) {
            throw DomainRuleViolation("Payment is already voided.")
        }

        return copy(status = PaymentStatus.VOIDED)
    }

    fun refund(): Payment {
        if (status == PaymentStatus.VOIDED) {
            throw DomainRuleViolation("Voided payment cannot be refunded.")
        }

        if (status == PaymentStatus.REFUNDED) {
            throw DomainRuleViolation("Payment is already refunded.")
        }

        return copy(status = PaymentStatus.REFUNDED)
    }

    companion object {
        fun record(
            id: String,
            organizationId: String,
            saleId: String,
            amount: Money,
            method: PaymentMethod,
            paidAt: Instant,
            reference: String? = null,
            notes: String? = null
        ): Payment {
            return Payment(
                id = id,
                organizationId = organizationId,
                saleId = saleId,
                amount = amount,
                method = method,
                status = PaymentStatus.PAID,
                paidAt = paidAt,
                reference = reference?.trim()?.takeIf { it.isNotBlank() },
                notes = notes?.trim()?.takeIf { it.isNotBlank() }
            )
        }
    }
}
