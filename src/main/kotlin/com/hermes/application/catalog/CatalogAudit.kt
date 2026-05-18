package com.hermes.application.catalog

import java.time.Instant

enum class CatalogAuditAction {
    PLATFORM_TEMPLATE_CREATED,
    PLATFORM_TEMPLATE_SEARCHED,
    PLATFORM_TEMPLATE_VIEWED,
    PLATFORM_TEMPLATE_UPDATED,
    PLATFORM_TEMPLATE_PUBLISHED,
    PLATFORM_TEMPLATE_PAUSED,
    PLATFORM_TEMPLATE_ARCHIVED,
    PLATFORM_CATEGORY_CREATED,
    PLATFORM_CATEGORY_UPDATED,
    PLATFORM_CATEGORY_VIEWED,
    PLATFORM_FAMILY_CREATED,
    PLATFORM_FAMILY_UPDATED,
    PLATFORM_FAMILY_VIEWED,
    TEMPLATE_COPIED_TO_ORGANIZATION,
    LOCAL_ITEM_UPDATED,
    LOCAL_ITEM_DISABLED,
    LOCAL_ITEM_REMOVED,
    LOCAL_ITEM_VIEWED,
    LOCAL_ITEM_LOOKED_UP_BY_CODE,
    LOCAL_ITEM_TAX_PROFILE_ASSIGNED,
    CATALOG_ITEM_REQUESTED,
    CATALOG_ITEM_REQUEST_REVIEWED,
    CATALOG_ITEM_REQUEST_LISTED,
    CATALOG_ITEM_REQUEST_APPROVED,
    CATALOG_ITEM_REQUEST_REJECTED,
    CATALOG_ITEM_REQUEST_LINKED_TO_EXISTING,
    CATALOG_ITEM_REQUEST_MORE_INFO_REQUESTED,
    CATALOG_AUDIT_VIEWED,
    CATALOG_PRICE_HISTORY_VIEWED,
}

data class CatalogAuditEvent(
    val action: CatalogAuditAction,
    val actorUserId: String?,
    val organizationId: String?,
    val targetId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface CatalogAuditLogger {
    fun log(event: CatalogAuditEvent)
}

object NoopCatalogAuditLogger : CatalogAuditLogger {
    override fun log(event: CatalogAuditEvent) = Unit
}

data class CatalogAuditQuery(
    val organizationId: String,
    val actions: Set<CatalogAuditAction> = emptySet(),
    val actorUserId: String? = null,
    val targetId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(organizationId.isNotBlank()) { "Organization id is required for catalog audit query." }
        require(limit in 1..MAX_LIMIT) { "Catalog audit query limit must be between 1 and $MAX_LIMIT." }
        if (from != null && to != null) require(!from.isAfter(to)) { "Catalog audit query from cannot be after to." }
    }

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}

data class CatalogAuditRecord(
    val id: String,
    val action: CatalogAuditAction,
    val actorUserId: String?,
    val organizationId: String,
    val targetId: String?,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface CatalogAuditQueryRepository {
    fun search(query: CatalogAuditQuery): List<CatalogAuditRecord>
}
