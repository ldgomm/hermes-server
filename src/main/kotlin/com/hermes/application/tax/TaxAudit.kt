package com.hermes.application.tax

import java.time.Instant

enum class TaxAuditAction {
    TAX_RATE_CREATED,
    TAX_RATE_UPDATED,
    TAX_RATE_DEPRECATED,
    TAX_PROFILE_CREATED,
    TAX_PROFILE_UPDATED,
    TAX_PROFILE_DEPRECATED,
    ORGANIZATION_TAX_SETTINGS_UPDATED,
    TAX_CALCULATION_PREVIEWED,
}

data class TaxAuditEvent(
    val action: TaxAuditAction,
    val actorUserId: String?,
    val organizationId: String?,
    val targetId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface TaxAuditLogger {
    fun log(event: TaxAuditEvent)
}

object NoopTaxAuditLogger : TaxAuditLogger {
    override fun log(event: TaxAuditEvent) = Unit
}
