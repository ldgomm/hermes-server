package com.hermes.application.admin.catalog

import com.hermes.application.catalog.CatalogItemRequestRepository
import com.hermes.application.catalog.CatalogItemRequestResult
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Admin Catalog request detail command.
 *
 * The generic catalog module already has create/list/review flows. This small
 * admin-specific use case closes the public route contract with a safe detail
 * endpoint scoped to the active organization unless the actor has platform
 * master-catalog permissions.
 */
data class GetAdminCatalogRequestCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val requestId: String,
)

class GetAdminCatalogRequestUseCase(
    private val requestRepository: CatalogItemRequestRepository,
) {
    fun execute(command: GetAdminCatalogRequestCommand): CatalogItemRequestResult {
        val canViewOrganizationRequest = PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_VIEW,
        ) || PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM,
        )
        val canViewPlatformQueue = PermissionRules.canPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CATALOG_MANAGE_MASTER,
        )

        if (!canViewOrganizationRequest && !canViewPlatformQueue) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.CATALOG_LOCAL_VIEW}, " +
                    "${PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM}, ${PermissionCatalog.CATALOG_MANAGE_MASTER}.",
            )
        }

        val organizationId = command.organizationId.requiredAdminCatalog("Organization id")
        val requestId = command.requestId.requiredAdminCatalog("Catalog request id")
        command.actorUserId.requiredAdminCatalog("Actor user id")

        val request = requestRepository.findById(requestId)
            ?: throw DomainRuleViolation("Catalog item request does not exist.")

        if (!canViewPlatformQueue && request.organizationId != organizationId) {
            throw DomainRuleViolation("Catalog item request does not belong to the active organization.")
        }

        return CatalogItemRequestResult(request)
    }
}

internal fun String.requiredAdminCatalog(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
