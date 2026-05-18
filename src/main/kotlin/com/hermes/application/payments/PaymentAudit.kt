package com.hermes.application.payments

import java.time.Instant

enum class PaymentAuditAction {
    PAYMENT_REGISTERED,
    CASH_MOVEMENT_CREATED,
    RECEIVABLE_CREATED,
}

data class PaymentAuditEvent(
    val action: PaymentAuditAction,
    val actorUserId: String,
    val organizationId: String,
    val targetId: String,
    val saleId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val createdAt: Instant,
)

fun interface PaymentAuditLogger {
    fun log(event: PaymentAuditEvent)
}

object NoopPaymentAuditLogger : PaymentAuditLogger {
    override fun log(event: PaymentAuditEvent) = Unit
}
