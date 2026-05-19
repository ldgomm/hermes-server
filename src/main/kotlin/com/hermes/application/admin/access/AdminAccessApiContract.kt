package com.hermes.application.admin.access

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
        AdminAccessRouteContract(
            "POST", "/api/v1/admin/invitations/{invitationId}/resend", "Resend invitation placeholder"
        ),
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
)
