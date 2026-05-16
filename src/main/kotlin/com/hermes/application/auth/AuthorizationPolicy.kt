package com.hermes.application.auth

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation

object AuthorizationPolicy {
    fun canPerform(effectivePermissions: Set<String>, requiredPermission: String): Boolean {
        if (requiredPermission.isBlank()) return false
        return PermissionCatalog.ALL in effectivePermissions || requiredPermission in effectivePermissions
    }

    fun canPerformAny(effectivePermissions: Set<String>, requiredPermissions: Set<String>): Boolean {
        if (requiredPermissions.isEmpty()) return false
        return PermissionCatalog.ALL in effectivePermissions || requiredPermissions.any { it in effectivePermissions }
    }

    fun requirePermission(effectivePermissions: Set<String>, requiredPermission: String) {
        if (!canPerform(effectivePermissions, requiredPermission)) {
            throw DomainRuleViolation("Missing required permission: $requiredPermission.")
        }
    }

    fun requireAny(effectivePermissions: Set<String>, requiredPermissions: Set<String>) {
        if (!canPerformAny(effectivePermissions, requiredPermissions)) {
            throw DomainRuleViolation(
                "Missing any required permission: ${
                    requiredPermissions.sorted().joinToString()
                }."
            )
        }
    }
}
