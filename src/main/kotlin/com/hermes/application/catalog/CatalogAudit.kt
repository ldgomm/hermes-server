package com.hermes.application.catalog

import java.time.Instant

enum class CatalogAuditAction {
    PLATFORM_TEMPLATE_CREATED,
    PLATFORM_TEMPLATE_SEARCHED,
    TEMPLATE_COPIED_TO_ORGANIZATION,
    LOCAL_ITEM_UPDATED,
    LOCAL_ITEM_DISABLED,
    LOCAL_ITEM_TAX_PROFILE_ASSIGNED,
    CATALOG_ITEM_REQUESTED,
    CATALOG_ITEM_REQUEST_REVIEWED,
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
