package com.hermes.domain.role

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation

object RoleSeedRules {
    private val requiredRoleCodes = SystemRoleCode.codes

    fun validate(roles: List<RoleDefinition>, knownPermissionCodes: Set<String> = PermissionCatalog.known) {
        if (roles.isEmpty()) {
            throw DomainRuleViolation("Role seed cannot be empty.")
        }

        assertNoDuplicateIds(roles)
        assertNoDuplicateCodes(roles)
        assertRequiredRolesExist(roles)
        assertSeedContainsNoCustomRoles(roles)
        assertPlatformRolesAreNotOrganizationRoles(roles)
        assertWildcardUsageIsSafe(roles)
        assertElectronicInvoiceRolesCanUseSignature(roles)
        assertOrganizationRolesDoNotGrantPlatformPermissions(roles)
        assertAllPermissionsExist(roles, knownPermissionCodes)
    }

    fun requiredPermissions(roles: List<RoleDefinition>): Set<String> = roles
        .flatMap { it.permissionKeys }
        .filterNot { it == PermissionCatalog.ALL }
        .toSet()

    fun assertCanEdit(role: RoleDefinition) {
        if (!role.editable) {
            throw DomainRuleViolation("Role ${role.code} is system managed and cannot be edited destructively.")
        }

        if (role.critical) {
            throw DomainRuleViolation("Critical role ${role.code} cannot be edited destructively.")
        }
    }

    private fun assertNoDuplicateIds(roles: List<RoleDefinition>) {
        val duplicates = roles.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw DomainRuleViolation("Duplicated role ids: ${duplicates.sorted().joinToString()}.")
        }
    }

    private fun assertNoDuplicateCodes(roles: List<RoleDefinition>) {
        val duplicates = roles.groupBy { it.code }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw DomainRuleViolation("Duplicated role codes: ${duplicates.sorted().joinToString()}.")
        }
    }

    private fun assertRequiredRolesExist(roles: List<RoleDefinition>) {
        val existing = roles.map { it.code }.toSet()
        val missing = requiredRoleCodes - existing

        if (missing.isNotEmpty()) {
            throw DomainRuleViolation("Missing required system roles: ${missing.sorted().joinToString()}.")
        }
    }

    private fun assertSeedContainsNoCustomRoles(roles: List<RoleDefinition>) {
        val customRoles = roles.filter { it.type == RoleType.CUSTOM }
        if (customRoles.isNotEmpty()) {
            throw DomainRuleViolation("System role seed cannot contain custom roles: ${customRoles.joinToString { it.code }}.")
        }
    }

    private fun assertPlatformRolesAreNotOrganizationRoles(roles: List<RoleDefinition>) {
        val platformAsOrganization = roles.filter {
            it.code.startsWith("platform_") && it.scope != RoleScope.PLATFORM
        }

        if (platformAsOrganization.isNotEmpty()) {
            throw DomainRuleViolation(
                "Platform roles must use platform scope: ${platformAsOrganization.joinToString { it.code }}."
            )
        }

        val organizationAsPlatform = roles.filter {
            it.code.startsWith("organization_") && it.scope != RoleScope.ORGANIZATION
        }

        if (organizationAsPlatform.isNotEmpty()) {
            throw DomainRuleViolation(
                "Organization roles must use organization scope: ${organizationAsPlatform.joinToString { it.code }}."
            )
        }
    }

    private fun assertWildcardUsageIsSafe(roles: List<RoleDefinition>) {
        val wildcardRoles = roles.filter { PermissionCatalog.ALL in it.permissionKeys }
        val unsafe = wildcardRoles.filter { it.code != SystemRoleCode.PLATFORM_SUPER_ADMIN.code }

        if (unsafe.isNotEmpty()) {
            throw DomainRuleViolation(
                "Wildcard permission is allowed only for ${SystemRoleCode.PLATFORM_SUPER_ADMIN.code}."
            )
        }
    }

    private fun assertElectronicInvoiceRolesCanUseSignature(roles: List<RoleDefinition>) {
        val invalid = roles.filter { role ->
            val permissions = role.permissionKeys
            PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE in permissions &&
                    PermissionCatalog.SIGNATURE_USE_FOR_INVOICING !in permissions &&
                    PermissionCatalog.ALL !in permissions
        }

        if (invalid.isNotEmpty()) {
            throw DomainRuleViolation(
                "Roles that issue electronic invoices must also be allowed to use signature for invoicing: " +
                        invalid.joinToString { it.code }
            )
        }
    }

    private fun assertOrganizationRolesDoNotGrantPlatformPermissions(roles: List<RoleDefinition>) {
        val invalid = roles.filter { role ->
            role.scope == RoleScope.ORGANIZATION && role.permissionKeys.any { it.startsWith("platform.") }
        }

        if (invalid.isNotEmpty()) {
            throw DomainRuleViolation(
                "Organization roles cannot grant platform permissions: ${invalid.joinToString { it.code }}."
            )
        }
    }

    private fun assertAllPermissionsExist(roles: List<RoleDefinition>, knownPermissionCodes: Set<String>) {
        val required = requiredPermissions(roles)
        val missing = required - knownPermissionCodes

        if (missing.isNotEmpty()) {
            throw DomainRuleViolation("Role seed references unknown permissions: ${missing.sorted().joinToString()}.")
        }
    }
}
