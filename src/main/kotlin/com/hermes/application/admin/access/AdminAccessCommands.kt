package com.hermes.application.admin.access

data class CreateAdminTemporaryUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val temporaryPassword: String? = null,
    val phone: String? = null,
    val reason: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class ListAdminUsersCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val status: String? = null,
    val limit: Int = 100,
)

data class GetAdminUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
)

data class UpdateAdminUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
    val displayName: String? = null,
    val phone: String? = null,
    val clearPhone: Boolean = false,
    val roleIds: Set<String>? = null,
    val reason: String,
)



data class BlockAdminUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
    val reason: String,
)

data class UnblockAdminUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
    val reason: String,
)

data class RevokeAdminUserSessionsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
    val reason: String,
)

data class AdminResetUserPasswordCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val userId: String,
    val temporaryPassword: String? = null,
    val revokeSessions: Boolean = true,
    val reason: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class ListAdminInvitationsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val status: String? = null,
    val limit: Int = 100,
)

data class GetAdminInvitationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val invitationId: String,
)

data class ResendAdminInvitationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val invitationId: String,
    val reason: String,
)

data class RevokeAdminInvitationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val invitationId: String,
    val reason: String,
)

data class ListAdminRolesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val includeSystemTemplates: Boolean = true,
)

data class GetAdminRoleCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val roleId: String,
)

data class CreateAdminRoleCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val code: String,
    val name: String,
    val description: String,
    val permissionKeys: Set<String>,
    val reason: String,
)

data class UpdateAdminRoleCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val roleId: String,
    val name: String? = null,
    val description: String? = null,
    val permissionKeys: Set<String>? = null,
    val reason: String,
)

data class ChangeAdminRoleStatusCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val roleId: String,
    val targetStatus: String,
    val reason: String,
)

data class ListAdminPermissionsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val includeReserved: Boolean = false,
)
