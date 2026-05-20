package com.hermes.application.admin.support

import com.hermes.domain.permission.PermissionCatalog

/**
 * Executable REST contract for Fase 13F — Global Audit & Support API.
 *
 * This contract is deliberately framework-free so Admin iOS/Web Admin,
 * smoke tests and lightweight generated docs can detect endpoint drift
 * without booting Ktor.
 */
object AdminSupportApiContract {
    val routes: List<AdminSupportRouteContract> = listOf(
        AdminSupportRouteContract("GET", "/api/v1/admin/audit/logs", "Search organization audit logs"),
        AdminSupportRouteContract("GET", "/api/v1/admin/audit/timeline", "Get organization audit timeline"),
        AdminSupportRouteContract("GET", "/api/v1/admin/support/diagnostics", "Get support diagnostics"),
        AdminSupportRouteContract("GET", "/api/v1/admin/support/permissions", "Get actor permission diagnostics"),
        AdminSupportRouteContract("GET", "/api/v1/admin/support/modules", "Get admin module diagnostics"),
    )
}

data class AdminSupportRouteContract(
    val method: String,
    val path: String,
    val description: String,
) {
    val key: String get() = "$method $path"
}

object AdminSupportSecurityContract {
    val endpoints: List<AdminSupportEndpointSecurityContract> = listOf(
        AdminSupportEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/audit/logs",
            surface = AdminSupportSurface.AUDIT,
            requiredPermissions = setOf(PermissionCatalog.AUDIT_VIEW),
            permissionMode = AdminSupportPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            secretsAllowed = false,
            note = "Read-only global audit search scoped to the active organization.",
        ),
        AdminSupportEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/audit/timeline",
            surface = AdminSupportSurface.AUDIT,
            requiredPermissions = setOf(PermissionCatalog.AUDIT_VIEW),
            permissionMode = AdminSupportPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            secretsAllowed = false,
            note = "Timeline optimized for 'who did what, when and over which resource'.",
        ),
        AdminSupportEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/support/diagnostics",
            surface = AdminSupportSurface.SUPPORT,
            requiredPermissions = setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW),
            permissionMode = AdminSupportPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            secretsAllowed = false,
            note = "Operational diagnostics for support without passwords, tokens, certificates or private storage keys.",
        ),
        AdminSupportEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/support/permissions",
            surface = AdminSupportSurface.PERMISSIONS,
            requiredPermissions = setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.CREDENTIALS_ROLES_VIEW),
            permissionMode = AdminSupportPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            secretsAllowed = false,
            note = "Explains the authenticated actor's effective permissions and admin-risk flags.",
        ),
        AdminSupportEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/support/modules",
            surface = AdminSupportSurface.MODULES,
            requiredPermissions = setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW),
            permissionMode = AdminSupportPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            secretsAllowed = false,
            note = "Shows Admin General API module readiness and missing permissions for the active actor.",
        ),
    )

    val readEndpoints: List<AdminSupportEndpointSecurityContract>
        get() = endpoints.filterNot { it.mutation }

    val mutationEndpoints: List<AdminSupportEndpointSecurityContract>
        get() = endpoints.filter { it.mutation }

    fun find(method: String, path: String): AdminSupportEndpointSecurityContract? =
        endpoints.firstOrNull { it.method == method.uppercase() && it.path == path }
}

data class AdminSupportEndpointSecurityContract(
    val method: String,
    val path: String,
    val surface: AdminSupportSurface,
    val requiredPermissions: Set<String>,
    val permissionMode: AdminSupportPermissionMode,
    val organizationScoped: Boolean,
    val mutation: Boolean,
    val requiresReason: Boolean,
    val audited: Boolean,
    val critical: Boolean,
    val secretsAllowed: Boolean,
    val note: String,
) {
    val key: String get() = "$method $path"
}

enum class AdminSupportSurface {
    AUDIT,
    SUPPORT,
    PERMISSIONS,
    MODULES,
}

enum class AdminSupportPermissionMode {
    ALL,
    ANY,
}
