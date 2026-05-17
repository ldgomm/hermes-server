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
    TAX_SALE_VALIDATED,
    TAX_DOCUMENT_EMISSION_VALIDATED,
    TAX_AUDIT_VIEWED,
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

data class TaxAuditQuery(
    val organizationId: String,
    val actions: Set<TaxAuditAction> = emptySet(),
    val actorUserId: String? = null,
    val targetId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(organizationId.isNotBlank()) { "Organization id is required for tax audit query." }
        require(limit in 1..MAX_LIMIT) { "Tax audit query limit must be between 1 and $MAX_LIMIT." }
        if (from != null && to != null) require(!from.isAfter(to)) { "Tax audit query from cannot be after to." }
    }

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}

data class TaxAuditRecord(
    val id: String,
    val action: TaxAuditAction,
    val actorUserId: String?,
    val organizationId: String,
    val targetId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface TaxAuditQueryRepository {
    fun search(query: TaxAuditQuery): List<TaxAuditRecord>
}

object EmptyTaxAuditQueryRepository : TaxAuditQueryRepository {
    override fun search(query: TaxAuditQuery): List<TaxAuditRecord> = emptyList()
}
