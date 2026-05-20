package com.hermes.application.admin.catalog

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogAuditEvent
import com.hermes.application.catalog.CatalogAuditLogger
import com.hermes.application.catalog.CatalogSearchMasterTemplatesCommand
import com.hermes.application.catalog.CatalogTemplateSearchQuery
import com.hermes.application.catalog.CatalogTemplatesResult
import com.hermes.application.catalog.NoopCatalogAuditLogger
import com.hermes.application.catalog.PlatformCatalogTemplateRepository
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

/**
 * Admin-facing master-template search.
 *
 * The core catalog search use case was designed for business users copying from
 * the master catalog and therefore requires CATALOG_LOCAL_VIEW. The Admin API
 * contract also allows platform catalog managers to search the same endpoint
 * with CATALOG_MANAGE_MASTER, so this adapter keeps route contract and use-case
 * authorization aligned without weakening the core catalog flow.
 */
class SearchAdminCatalogMasterTemplatesUseCase(
    private val templateRepository: PlatformCatalogTemplateRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogSearchMasterTemplatesCommand): CatalogTemplatesResult {
        val canSearch = PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_VIEW,
        ) || PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_MANAGE_MASTER,
        )
        if (!canSearch) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.CATALOG_LOCAL_VIEW}, ${PermissionCatalog.CATALOG_MANAGE_MASTER}.",
            )
        }

        val organizationId = command.organizationId.requiredAdminCatalog("Organization id")
        val result = templateRepository.search(
            CatalogTemplateSearchQuery(
                query = command.query,
                identifier = command.identifier?.normalizeIdentifierSearchForAdmin(),
                type = command.type,
                onlyActive = true,
                limit = command.limit.coerceIn(1, 100),
            ),
        )
        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.PLATFORM_TEMPLATE_SEARCHED,
                actorUserId = command.actorUserId.requiredAdminCatalog("Actor user id"),
                organizationId = organizationId,
                targetId = null,
                after = mapOf("resultCount" to result.size.toString(), "surface" to "admin_catalog"),
                createdAt = Instant.now(clock),
            ),
        )
        return CatalogTemplatesResult(result)
    }
}

private fun String.normalizeIdentifierSearchForAdmin(): String = trim()
    .filterNot { it == ' ' || it == '-' }
    .uppercase()
