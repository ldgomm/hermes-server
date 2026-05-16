package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation

object PermissionRules {
    fun validateRole(role: Role) {
        val unknown = role.permissions.filterNot { it in PermissionCatalog.known }
        if (unknown.isNotEmpty()) {
            throw DomainRuleViolation("Unknown permissions: ${unknown.joinToString()}.")
        }
        if (PermissionCatalog.SIGNATURE_USE_FOR_INVOICING in role.permissions &&
            PermissionCatalog.SIGNATURE_VIEW_METADATA !in role.permissions &&
            PermissionCatalog.ALL !in role.permissions
        ) {
            throw DomainRuleViolation("Using signature for invoicing requires signature metadata permission.")
        }
        if (!role.systemRole && PermissionCatalog.ALL in role.permissions) {
            throw DomainRuleViolation("Wildcard permission is allowed only for system roles.")
        }
    }

    fun effectivePermissions(roles: List<Role>): Set<String> {
        roles.forEach(::validateRole)
        if (roles.any { PermissionCatalog.ALL in it.permissions }) return setOf(PermissionCatalog.ALL)
        return roles.flatMap { it.permissions }.toSet()
    }

    fun canPerform(effectivePermissions: Set<String>, permission: String): Boolean =
        PermissionCatalog.ALL in effectivePermissions || permission in effectivePermissions

    fun assertCanPerform(effectivePermissions: Set<String>, permission: String) {
        if (!canPerform(effectivePermissions, permission)) {
            throw DomainRuleViolation("Missing required permission: $permission.")
        }
    }
}
