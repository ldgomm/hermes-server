package com.hermes.application.payments

import com.hermes.domain.money.Money
import com.hermes.domain.payment.PaymentMethod
import java.time.Instant

data class RegisterPaymentCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: Instant? = null,
    val reference: String? = null,
    val notes: String? = null,
    val markRemainingAsReceivable: Boolean = false,
    val receivableDueAt: Instant? = null,
)
