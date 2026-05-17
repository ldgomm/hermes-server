package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CatalogGetOrganizationItemUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
) {
    fun execute(command: CatalogGetOrganizationItemCommand): OrganizationCatalogItemResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW)

        val organizationId = command.organizationId.requiredLocalCatalog("Organization id")
        val itemId = command.catalogItemId.requiredLocalCatalog("Catalog item id")

        val item = itemRepository.findById(organizationId = organizationId, catalogItemId = itemId)
            ?: throw DomainRuleViolation("Organization catalog item does not exist.")

        return OrganizationCatalogItemResult(item)
    }
}

class CatalogLookupOrganizationItemByCodeUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
) {
    fun execute(command: CatalogLookupOrganizationItemByCodeCommand): OrganizationCatalogItemResult {
        val canLookup = PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW) ||
            PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_IDENTIFIERS_SCAN)
        if (!canLookup) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.CATALOG_LOCAL_VIEW}, ${PermissionCatalog.CATALOG_IDENTIFIERS_SCAN}."
            )
        }

        val organizationId = command.organizationId.requiredLocalCatalog("Organization id")
        val normalizedCode = command.code.normalizeLocalCatalogCode()
        if (normalizedCode.isBlank()) throw DomainRuleViolation("Lookup code cannot be blank.")

        val statuses = if (command.includeInactive) {
            emptySet()
        } else {
            setOf(CatalogItemStatus.ACTIVE)
        }

        val matches = itemRepository.search(
            OrganizationCatalogSearchQuery(
                organizationId = organizationId,
                identifier = normalizedCode,
                statuses = statuses,
                limit = 10,
            )
        ).filterNot { it.status == CatalogItemStatus.REMOVED_FROM_ACCOUNT }

        val item = when (matches.size) {
            0 -> throw DomainRuleViolation("No organization catalog item exists for code: $normalizedCode.")
            1 -> matches.first()
            else -> throw DomainRuleViolation("Catalog lookup code is ambiguous inside this organization: $normalizedCode.")
        }

        return OrganizationCatalogItemResult(item)
    }
}

class CatalogRemoveLocalItemUseCase(
    private val itemRepository: OrganizationCatalogItemRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogRemoveLocalItemCommand): OrganizationCatalogItemResult {
        val canRemove = PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY) ||
            PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.CATALOG_MANAGE_LOCAL)
        if (!canRemove) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY}, ${PermissionCatalog.CATALOG_MANAGE_LOCAL}."
            )
        }

        val organizationId = command.organizationId.requiredLocalCatalog("Organization id")
        val itemId = command.catalogItemId.requiredLocalCatalog("Catalog item id")
        val reason = command.reason.requiredLocalCatalog("Catalog remove reason")
        val current = itemRepository.findById(organizationId = organizationId, catalogItemId = itemId)
            ?: throw DomainRuleViolation("Organization catalog item does not exist.")

        if (current.status == CatalogItemStatus.REMOVED_FROM_ACCOUNT) {
            throw DomainRuleViolation("Organization catalog item is already removed from this account.")
        }

        val now = Instant.now(clock)
        val removed = current.copy(status = CatalogItemStatus.REMOVED_FROM_ACCOUNT)
        itemRepository.update(removed)

        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.LOCAL_ITEM_DISABLED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = current.id,
                before = mapOf("status" to current.status.name),
                after = mapOf("status" to removed.status.name, "operation" to "remove_local_copy"),
                reason = reason,
                createdAt = now,
            )
        )

        return OrganizationCatalogItemResult(removed)
    }
}

private fun String.requiredLocalCatalog(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun String.normalizeLocalCatalogCode(): String =
    trim().filterNot { it == ' ' || it == '-' }.uppercase()
