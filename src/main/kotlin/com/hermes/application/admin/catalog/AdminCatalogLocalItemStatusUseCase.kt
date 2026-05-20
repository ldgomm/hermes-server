package com.hermes.application.admin.catalog

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogAuditEvent
import com.hermes.application.catalog.CatalogAuditLogger
import com.hermes.application.catalog.NoopCatalogAuditLogger
import com.hermes.application.catalog.OrganizationCatalogItemRepository
import com.hermes.application.catalog.OrganizationCatalogItemResult
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

/**
 * Admin-specific status transition for local catalog items.
 *
 * Core CatalogUpdateLocalItemUseCase intentionally checks update/price/tax field
 * permissions. The Admin API needs two explicit operational transitions:
 * activate and deactivate. This use case keeps those permissions aligned with
 * the public Admin route contract.
 */
data class ChangeAdminCatalogLocalItemStatusCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val catalogItemId: String,
    val targetStatus: CatalogItemStatus,
    val reason: String,
)

class ChangeAdminCatalogLocalItemStatusUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ChangeAdminCatalogLocalItemStatusCommand): OrganizationCatalogItemResult {
        val requiredPermission = when (command.targetStatus) {
            CatalogItemStatus.ACTIVE -> PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY
            CatalogItemStatus.PAUSED -> PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY
            else -> throw DomainRuleViolation("Unsupported admin catalog item status transition: ${command.targetStatus}.")
        }
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, requiredPermission)

        val organizationId = command.organizationId.requiredAdminCatalog("Organization id")
        val actorUserId = command.actorUserId.requiredAdminCatalog("Actor user id")
        val catalogItemId = command.catalogItemId.requiredAdminCatalog("Catalog item id")
        val reason = command.reason.requiredAdminCatalog("Catalog item status change reason")

        val current = itemRepository.findById(organizationId, catalogItemId)
            ?: throw DomainRuleViolation("Organization catalog item does not exist.")
        if (current.status == CatalogItemStatus.REMOVED_FROM_ACCOUNT) {
            throw DomainRuleViolation("Removed organization catalog item cannot be reactivated or paused.")
        }
        if (current.status == command.targetStatus) {
            throw DomainRuleViolation("Organization catalog item is already ${command.targetStatus}.")
        }

        val now = Instant.now(clock)
        val updated = current.copy(status = command.targetStatus)
        itemRepository.update(updated)

        auditLogger.log(
            CatalogAuditEvent(
                action = if (command.targetStatus == CatalogItemStatus.PAUSED) {
                    CatalogAuditAction.LOCAL_ITEM_DISABLED
                } else {
                    CatalogAuditAction.LOCAL_ITEM_UPDATED
                },
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = catalogItemId,
                before = mapOf("status" to current.status.name),
                after = mapOf("status" to updated.status.name, "operation" to "admin_status_change"),
                reason = reason,
                createdAt = now,
            ),
        )

        return OrganizationCatalogItemResult(updated)
    }
}
