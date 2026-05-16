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
    val status: PaymentLifecycleStatus, //Unresolved reference 'PaymentLifecycleStatus'.
    val paidAt: Instant,
    val reference: String?,
    val notes: String?,
) {
    val isEffective: Boolean
        get() = status.isEffective //Unresolved reference 'isEffective'.

    init {
        if (id.isBlank()) throw DomainRuleViolation("Payment id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Payment organization id cannot be blank.")
        if (saleId.isBlank()) throw DomainRuleViolation("Payment sale id cannot be blank.")
        if (amount.amount.signum() <= 0) throw DomainRuleViolation("Payment amount must be greater than zero.")
    }

    fun allocate(): Payment {
        if (status != PaymentLifecycleStatus.CONFIRMED) { //Unresolved reference 'PaymentLifecycleStatus'.
            throw DomainRuleViolation("Only a confirmed payment can be allocated.")
        }
        return copy(status = PaymentLifecycleStatus.ALLOCATED)
    }

    fun void(): Payment {
        if (status in setOf(PaymentLifecycleStatus.ALLOCATED, PaymentLifecycleStatus.REVERSED)) {
            throw DomainRuleViolation("Allocated or reversed payments cannot be voided.")
        }
        if (status == PaymentLifecycleStatus.VOIDED) {
            throw DomainRuleViolation("Payment is already voided.")
        }
        return copy(status = PaymentLifecycleStatus.VOIDED)
    }

    fun reverse(): Payment {
        if (status !in setOf(PaymentLifecycleStatus.CONFIRMED, PaymentLifecycleStatus.ALLOCATED)) {
            throw DomainRuleViolation("Only confirmed or allocated payments can be reversed.")
        }
        return copy(status = PaymentLifecycleStatus.REVERSED)
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
            notes: String? = null,
        ): Payment {
            return Payment(
                id = id,
                organizationId = organizationId,
                saleId = saleId,
                amount = amount,
                method = method,
                status = PaymentLifecycleStatus.CONFIRMED,
                paidAt = paidAt,
                reference = reference?.trim()?.takeIf { it.isNotBlank() },
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
            )
        }
    }
}
