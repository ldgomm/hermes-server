package com.hermes.application.admin.business

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

class GetAdminBusinessUseCase(
    private val repository: AdminBusinessRepository,
) {
    fun execute(command: GetAdminBusinessCommand): AdminBusinessResult {
        assertCanViewBusiness(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        val business = repository.findBusiness(organizationId)
            ?: throw DomainRuleViolation("Organization does not exist.")
        return AdminBusinessResult(business)
    }
}

class ListAdminActivitiesUseCase(
    private val repository: AdminBusinessRepository,
) {
    fun execute(command: ListAdminActivitiesCommand): AdminBusinessActivitiesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.ACTIVITIES_VIEW)
        val organizationId = command.organizationId.required("Organization id")
        return AdminBusinessActivitiesResult(repository.listActivities(organizationId))
    }
}

class ListAdminBranchesUseCase(
    private val repository: AdminBusinessRepository,
) {
    fun execute(command: ListAdminBranchesCommand): AdminBusinessBranchesResult {
        assertCanViewBranches(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        return AdminBusinessBranchesResult(repository.listBranches(organizationId))
    }
}

class ListAdminEmissionPointsUseCase(
    private val repository: AdminBusinessRepository,
) {
    fun execute(command: ListAdminEmissionPointsCommand): AdminBusinessEmissionPointsResult {
        assertCanViewEmissionPoints(command.actorEffectivePermissions)
        val organizationId = command.organizationId.required("Organization id")
        return AdminBusinessEmissionPointsResult(repository.listEmissionPoints(organizationId))
    }
}

internal fun assertCanViewBusiness(effectivePermissions: Set<String>) {
    if (!canPerformAny(
            effectivePermissions,
            setOf(PermissionCatalog.ORGANIZATION_VIEW, PermissionCatalog.ORGANIZATION_UPDATE)
        )
    ) {
        throw DomainRuleViolation(
            "Missing any required permission: ${PermissionCatalog.ORGANIZATION_VIEW}, ${PermissionCatalog.ORGANIZATION_UPDATE}."
        )
    }
}

internal fun assertCanViewBranches(effectivePermissions: Set<String>) {
    if (!canPerformAny(
            effectivePermissions,
            setOf(PermissionCatalog.BRANCHES_VIEW, PermissionCatalog.SETTINGS_BRANCHES_VIEW)
        )
    ) {
        throw DomainRuleViolation(
            "Missing any required permission: ${PermissionCatalog.BRANCHES_VIEW}, ${PermissionCatalog.SETTINGS_BRANCHES_VIEW}."
        )
    }
}

internal fun assertCanViewEmissionPoints(effectivePermissions: Set<String>) {
    if (!canPerformAny(effectivePermissions, setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW))) {
        throw DomainRuleViolation("Missing required permission: ${PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW}.")
    }
}

internal fun canPerformAny(effectivePermissions: Set<String>, required: Set<String>): Boolean =
    PermissionCatalog.ALL in effectivePermissions || required.any { it in effectivePermissions }

internal fun String.required(label: String): String = trim().takeIf { it.isNotBlank() }
    ?: throw DomainRuleViolation("$label cannot be blank.")
