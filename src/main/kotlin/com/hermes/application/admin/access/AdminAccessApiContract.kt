package com.hermes.application.admin.access

import com.hermes.domain.permission.PermissionCatalog

/**
 * Executable REST contract for Fase 13B — Users, Roles & Invitations Admin API.
 *
 * Keep this file intentionally framework-free so it can be used by:
 * - unit tests,
 * - Admin iOS planning,
 * - Web Admin planning,
 * - lightweight generated docs,
 * - smoke-test checklists.
 */
object AdminAccessApiContract {
    val routes: List<AdminAccessRouteContract> = listOf(
        AdminAccessRouteContract("GET", "/api/v1/admin/users", "List organization users"),
        AdminAccessRouteContract("POST", "/api/v1/admin/users/temporary", "Create temporary user"),
        AdminAccessRouteContract("GET", "/api/v1/admin/users/{userId}", "Get organization user detail"),
        AdminAccessRouteContract("PUT", "/api/v1/admin/users/{userId}", "Update user profile and roles"),
        AdminAccessRouteContract("POST", "/api/v1/admin/users/{userId}/block", "Block organization user"),
        AdminAccessRouteContract("POST", "/api/v1/admin/users/{userId}/unblock", "Unblock organization user"),
        AdminAccessRouteContract("POST", "/api/v1/admin/users/{userId}/reset-password", "Create temporary password"),
        AdminAccessRouteContract("POST", "/api/v1/admin/users/{userId}/revoke-sessions", "Revoke user sessions"),

        AdminAccessRouteContract("POST", "/api/v1/admin/invitations", "Create invitation"),
        AdminAccessRouteContract("GET", "/api/v1/admin/invitations", "List invitations"),
        AdminAccessRouteContract("GET", "/api/v1/admin/invitations/{invitationId}", "Get invitation"),
        AdminAccessRouteContract("POST", "/api/v1/admin/invitations/{invitationId}/resend", "Resend invitation"),
        AdminAccessRouteContract("POST", "/api/v1/admin/invitations/{invitationId}/revoke", "Revoke invitation"),

        AdminAccessRouteContract("GET", "/api/v1/admin/roles", "List roles"),
        AdminAccessRouteContract("POST", "/api/v1/admin/roles", "Create custom role"),
        AdminAccessRouteContract("GET", "/api/v1/admin/roles/{roleId}", "Get role"),
        AdminAccessRouteContract("PUT", "/api/v1/admin/roles/{roleId}", "Update custom role"),
        AdminAccessRouteContract("POST", "/api/v1/admin/roles/{roleId}/activate", "Activate custom role"),
        AdminAccessRouteContract("POST", "/api/v1/admin/roles/{roleId}/deactivate", "Deactivate custom role"),

        AdminAccessRouteContract("GET", "/api/v1/admin/permissions", "List available permissions"),
    )
}

data class AdminAccessRouteContract(
    val method: String,
    val path: String,
    val description: String,
) {
    val key: String get() = "$method $path"
}

/**
 * Security matrix for the same endpoints exposed by [AdminAccessApiContract].
 *
 * This is deliberately separated from Ktor route implementation. The goal is to
 * make authorization, audit and reason requirements easy to review and test
 * without booting the server.
 */
object AdminAccessSecurityContract {
    val endpoints: List<AdminAccessEndpointSecurityContract> = listOf(
        // Users.
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/users",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Read-only user directory for the active organization.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/users/temporary",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates a user with temporary credentials through the existing credential flow.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/users/{userId}",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Read-only detail for one organization user.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "PUT",
            path = "/api/v1/admin/users/{userId}",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Updates profile and role assignment without allowing the last admin to be removed.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/users/{userId}/block",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_BLOCK),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Blocks organization access, rejects self-block and last-admin removal.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/users/{userId}/unblock",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_UNBLOCK),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Restores organization access for a blocked user.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/users/{userId}/reset-password",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Generates a temporary password and may revoke active sessions.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/users/{userId}/revoke-sessions",
            surface = AdminAccessSurface.USERS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Revokes user sessions and active refresh tokens.",
        ),

        // Invitations.
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/invitations",
            surface = AdminAccessSurface.INVITATIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Delegates to existing InviteUserUseCase to avoid duplicated credential logic.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/invitations",
            surface = AdminAccessSurface.INVITATIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists invitations scoped to the active organization.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/invitations/{invitationId}",
            surface = AdminAccessSurface.INVITATIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Gets invitation detail scoped to the active organization.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/invitations/{invitationId}/resend",
            surface = AdminAccessSurface.INVITATIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Resends only pending invitations.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/invitations/{invitationId}/revoke",
            surface = AdminAccessSurface.INVITATIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Revokes only pending invitations.",
        ),

        // Roles.
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/roles",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists organization roles and allowed system templates.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/roles",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates only organization custom roles with known permissions.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/roles/{roleId}",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Gets one organization role.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "PUT",
            path = "/api/v1/admin/roles/{roleId}",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Updates only editable custom roles and preserves at least one admin and role manager.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/roles/{roleId}/activate",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Activates editable custom role.",
        ),
        AdminAccessEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/roles/{roleId}/deactivate",
            surface = AdminAccessSurface.ROLES,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
            mutation = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Deactivates editable custom role without removing last admin or role manager.",
        ),

        // Permissions.
        AdminAccessEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/permissions",
            surface = AdminAccessSurface.PERMISSIONS,
            requiredPermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW),
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Returns public permission definitions for Admin clients.",
        ),
    )

    val mutationEndpoints: List<AdminAccessEndpointSecurityContract>
        get() = endpoints.filter { it.mutation }

    val readEndpoints: List<AdminAccessEndpointSecurityContract>
        get() = endpoints.filterNot { it.mutation }

    fun find(method: String, path: String): AdminAccessEndpointSecurityContract? =
        endpoints.firstOrNull { it.method == method.uppercase() && it.path == path }
}

data class AdminAccessEndpointSecurityContract(
    val method: String,
    val path: String,
    val surface: AdminAccessSurface,
    val requiredPermissions: Set<String>,
    val mutation: Boolean,
    val requiresReason: Boolean,
    val audited: Boolean,
    val critical: Boolean,
    val note: String,
) {
    val key: String get() = "$method $path"
}

enum class AdminAccessSurface {
    USERS,
    INVITATIONS,
    ROLES,
    PERMISSIONS,
}
