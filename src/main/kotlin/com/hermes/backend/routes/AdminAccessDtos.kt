package com.hermes.backend.routes

import com.hermes.application.admin.access.AdminInvitationCreatedResult
import com.hermes.application.admin.access.AdminInvitationResult
import com.hermes.application.admin.access.AdminInvitationResendResult
import com.hermes.application.admin.access.AdminInvitationsResult
import com.hermes.application.admin.access.AdminPermissionSummary
import com.hermes.application.admin.access.AdminPermissionsResult
import com.hermes.application.admin.access.AdminResetUserPasswordResult
import com.hermes.application.admin.access.AdminRoleResult
import com.hermes.application.admin.access.AdminRoleSummary
import com.hermes.application.admin.access.AdminTemporaryUserResult
import com.hermes.application.admin.access.AdminRolesResult
import com.hermes.application.admin.access.AdminUserAccessDetail
import com.hermes.application.admin.access.AdminUserAccessSummary
import com.hermes.application.admin.access.AdminUserResult
import com.hermes.application.admin.access.AdminUsersResult
import com.hermes.application.admin.access.AdminUserSessionRevocationResult
import kotlinx.serialization.Serializable

@Serializable
data class AdminUsersResponse(val users: List<AdminUserResponse>)

@Serializable
data class CreateAdminTemporaryUserRequest(
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val temporaryPassword: String? = null,
    val phone: String? = null,
    val reason: String = "Create temporary admin user",
)

@Serializable
data class AdminTemporaryUserResponse(
    val user: AdminUserResponse,
    val credentialId: String,
    val membershipId: String,
    val temporaryPassword: String,
    val mustChangePassword: Boolean,
    val createdAt: String,
)

@Serializable
data class AdminUserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val phone: String? = null,
    val status: String,
    val membershipId: String,
    val membershipStatus: String,
    val roleIds: Set<String>,
    val roleNames: List<String>,
    val effectivePermissions: Set<String> = emptySet(),
    val activeSessionCount: Int,
    val invitedBy: String? = null,
    val acceptedAt: String? = null,
    val blockedAt: String? = null,
    val blockedReason: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val version: Long? = null,
)

@Serializable
data class UpdateAdminUserRequest(
    val displayName: String? = null,
    val phone: String? = null,
    val clearPhone: Boolean = false,
    val roleIds: Set<String>? = null,
    val reason: String,
)

@Serializable
data class AdminUserActionRequest(
    val reason: String,
)

@Serializable
data class AdminRevokeUserSessionsRequest(
    val reason: String,
)

@Serializable
data class AdminUserSessionRevocationResponse(
    val userId: String,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
    val revokedAt: String,
    val reason: String,
)

@Serializable
data class AdminResetUserPasswordRequest(
    val temporaryPassword: String? = null,
    val revokeSessions: Boolean = true,
    val reason: String,
)

@Serializable
data class AdminResetUserPasswordResponse(
    val userId: String,
    val credentialId: String,
    val temporaryPassword: String,
    val mustChangePassword: Boolean,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
    val changedAt: String,
)


@Serializable
data class CreateAdminInvitationRequest(
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val reason: String = "Create admin invitation",
)

@Serializable
data class AdminInvitationCreatedResponse(
    val invitation: AdminInvitationResponse,
    val user: AdminUserResponse,
    val membershipId: String,
    val rawInvitationToken: String,
    val invitationUrl: String? = null,
    val createdAt: String,
)

@Serializable
data class AdminInvitationsResponse(val invitations: List<AdminInvitationResponse>)

@Serializable
data class AdminInvitationResponse(
    val id: String,
    val organizationId: String,
    val email: String,
    val invitedByUserId: String,
    val roleIds: Set<String>,
    val roleNames: List<String>,
    val status: String,
    val createdAt: String,
    val expiresAt: String,
    val acceptedAt: String? = null,
    val revokedAt: String? = null,
    val acceptedUserId: String? = null,
    val version: Long,
)

@Serializable
data class AdminInvitationActionRequest(
    val reason: String,
)

@Serializable
data class AdminInvitationResendResponse(
    val invitation: AdminInvitationResponse,
    val rawInvitationToken: String,
    val invitationUrl: String? = null,
)

@Serializable
data class AdminRolesResponse(val roles: List<AdminRoleResponse>)

@Serializable
data class AdminRoleResponse(
    val id: String,
    val code: String,
    val organizationId: String? = null,
    val scope: String,
    val type: String,
    val name: String,
    val description: String,
    val permissionKeys: Set<String>,
    val systemRole: Boolean,
    val critical: Boolean,
    val editable: Boolean,
    val status: String,
    val schemaVersion: Int,
)

@Serializable
data class CreateAdminRoleRequest(
    val code: String,
    val name: String,
    val description: String,
    val permissionKeys: Set<String>,
    val reason: String,
)

@Serializable
data class UpdateAdminRoleRequest(
    val name: String? = null,
    val description: String? = null,
    val permissionKeys: Set<String>? = null,
    val reason: String,
)

@Serializable
data class AdminRoleActionRequest(
    val reason: String,
)

@Serializable
data class AdminPermissionsResponse(val permissions: List<AdminPermissionResponse>)

@Serializable
data class AdminPermissionResponse(
    val code: String,
    val name: String,
    val description: String,
    val category: String,
    val scope: String,
    val riskLevel: String,
    val status: String,
    val systemManaged: Boolean,
    val requiresAudit: Boolean,
    val requiresReason: Boolean,
    val requiresStepUp: Boolean,
    val featureFlag: String? = null,
)

fun AdminUsersResult.toResponse(): AdminUsersResponse =
    AdminUsersResponse(users.map { it.toResponse() })

fun AdminUserResult.toResponse(): AdminUserResponse = user.toResponse()

fun AdminTemporaryUserResult.toResponse(): AdminTemporaryUserResponse = AdminTemporaryUserResponse(
    user = user.toResponse(),
    credentialId = credentialId,
    membershipId = membershipId,
    temporaryPassword = temporaryPassword,
    mustChangePassword = mustChangePassword,
    createdAt = createdAt.toString(),
)

fun AdminUserAccessSummary.toResponse(): AdminUserResponse = AdminUserResponse(
    id = id,
    email = email,
    displayName = displayName,
    phone = phone,
    status = status,
    membershipId = membershipId,
    membershipStatus = membershipStatus,
    roleIds = roleIds,
    roleNames = roleNames,
    activeSessionCount = activeSessionCount,
    invitedBy = invitedBy,
    acceptedAt = acceptedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun AdminUserAccessDetail.toResponse(): AdminUserResponse = AdminUserResponse(
    id = id,
    email = email,
    displayName = displayName,
    phone = phone,
    status = status,
    membershipId = membershipId,
    membershipStatus = membershipStatus,
    roleIds = roles.map { it.id }.toSet(),
    roleNames = roles.map { it.name },
    effectivePermissions = effectivePermissions,
    activeSessionCount = activeSessionCount,
    invitedBy = invitedBy,
    acceptedAt = acceptedAt?.toString(),
    blockedAt = blockedAt?.toString(),
    blockedReason = blockedReason,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun AdminResetUserPasswordResult.toResponse(): AdminResetUserPasswordResponse = AdminResetUserPasswordResponse(
    userId = userId,
    credentialId = credentialId,
    temporaryPassword = temporaryPassword,
    mustChangePassword = mustChangePassword,
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
    changedAt = changedAt.toString(),
)

fun AdminUserSessionRevocationResult.toResponse(): AdminUserSessionRevocationResponse = AdminUserSessionRevocationResponse(
    userId = userId,
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
    revokedAt = revokedAt.toString(),
    reason = reason,
)


fun AdminInvitationCreatedResult.toResponse(): AdminInvitationCreatedResponse = AdminInvitationCreatedResponse(
    invitation = invitation.toResponse(),
    user = user.toResponse(),
    membershipId = membershipId,
    rawInvitationToken = rawInvitationToken,
    invitationUrl = invitationUrl,
    createdAt = createdAt.toString(),
)

fun AdminInvitationsResult.toResponse(): AdminInvitationsResponse =
    AdminInvitationsResponse(invitations.map { it.toResponse() })

fun AdminInvitationResult.toResponse(): AdminInvitationResponse = invitation.toResponse()

fun AdminInvitationResendResult.toResponse(): AdminInvitationResendResponse = AdminInvitationResendResponse(
    invitation = invitation.toResponse(),
    rawInvitationToken = rawInvitationToken,
    invitationUrl = invitationUrl,
)

fun com.hermes.application.admin.access.AdminInvitationSummary.toResponse(): AdminInvitationResponse =
    AdminInvitationResponse(
        id = id,
        organizationId = organizationId,
        email = email,
        invitedByUserId = invitedByUserId,
        roleIds = roleIds,
        roleNames = roleNames,
        status = status,
        createdAt = createdAt.toString(),
        expiresAt = expiresAt.toString(),
        acceptedAt = acceptedAt?.toString(),
        revokedAt = revokedAt?.toString(),
        acceptedUserId = acceptedUserId,
        version = version,
    )

fun AdminRolesResult.toResponse(): AdminRolesResponse =
    AdminRolesResponse(roles.map { it.toResponse() })

fun AdminRoleResult.toResponse(): AdminRoleResponse = role.toResponse()

fun AdminRoleSummary.toResponse(): AdminRoleResponse = AdminRoleResponse(
    id = id,
    code = code,
    organizationId = organizationId,
    scope = scope,
    type = type,
    name = name,
    description = description,
    permissionKeys = permissionKeys,
    systemRole = systemRole,
    critical = critical,
    editable = editable,
    status = status,
    schemaVersion = schemaVersion,
)

fun AdminPermissionsResult.toResponse(): AdminPermissionsResponse =
    AdminPermissionsResponse(permissions.map { it.toResponse() })

fun AdminPermissionSummary.toResponse(): AdminPermissionResponse = AdminPermissionResponse(
    code = code,
    name = name,
    description = description,
    category = category,
    scope = scope,
    riskLevel = riskLevel,
    status = status,
    systemManaged = systemManaged,
    requiresAudit = requiresAudit,
    requiresReason = requiresReason,
    requiresStepUp = requiresStepUp,
    featureFlag = featureFlag,
)
