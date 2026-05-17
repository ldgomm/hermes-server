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
