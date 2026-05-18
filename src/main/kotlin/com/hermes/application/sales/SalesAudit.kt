package com.hermes.application.sales

import java.time.Instant

enum class SalesAuditAction {
    SALE_CREATED,
    SALE_ITEM_ADDED,
    SALE_ITEM_REMOVED,
    SALE_CONFIRMED,
    SALE_STATUS_CHANGED,
    SALE_CANCELED,
    SALE_CLOSED,
    SALE_VIEWED,
    SALE_LISTED,
    RESERVATION_CREATED,
    RESERVATION_VIEWED,
    RESERVATION_STATUS_CHANGED,
    RESERVATION_LISTED,
}

data class SalesAuditEvent(
    val action: SalesAuditAction,
    val actorUserId: String?,
    val organizationId: String,
    val targetId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface SalesAuditLogger {
    fun log(event: SalesAuditEvent)
}

object NoopSalesAuditLogger : SalesAuditLogger {
    override fun log(event: SalesAuditEvent) = Unit
}
