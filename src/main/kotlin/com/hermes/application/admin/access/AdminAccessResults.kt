package com.hermes.application.admin.access

import java.time.Instant

data class AdminUsersResult(
    val users: List<AdminUserAccessSummary>,
)

data class AdminUserResult(
    val user: AdminUserAccessDetail,
)

data class AdminResetUserPasswordResult(
    val userId: String,
    val credentialId: String,
    val temporaryPassword: String,
    val mustChangePassword: Boolean,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
    val changedAt: Instant,
)

data class AdminInvitationsResult(
    val invitations: List<AdminInvitationSummary>,
)

data class AdminInvitationResult(
    val invitation: AdminInvitationSummary,
)

data class AdminInvitationResendResult(
    val invitation: AdminInvitationSummary,
    val rawInvitationToken: String,
    val invitationUrl: String? = null,
)

data class AdminRolesResult(
    val roles: List<AdminRoleSummary>,
)

data class AdminRoleResult(
    val role: AdminRoleSummary,
)

data class AdminPermissionsResult(
    val permissions: List<AdminPermissionSummary>,
)

data class AdminUserAccessSummary(
    val id: String,
    val email: String,
    val displayName: String,
    val phone: String?,
    val status: String,
    val membershipId: String,
    val membershipStatus: String,
    val roleIds: Set<String>,
    val roleNames: List<String>,
    val activeSessionCount: Int,
    val invitedBy: String?,
    val acceptedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AdminUserAccessDetail(
    val id: String,
    val email: String,
    val displayName: String,
    val phone: String?,
    val status: String,
    val blockedAt: Instant?,
    val blockedReason: String?,
    val membershipId: String,
    val membershipStatus: String,
    val roles: List<AdminRoleSummary>,
    val effectivePermissions: Set<String>,
    val activeSessionCount: Int,
    val invitedBy: String?,
    val acceptedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
)

data class AdminInvitationSummary(
    val id: String,
    val organizationId: String,
    val email: String,
    val invitedByUserId: String,
    val roleIds: Set<String>,
    val roleNames: List<String>,
    val status: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val acceptedAt: Instant?,
    val revokedAt: Instant?,
    val acceptedUserId: String?,
    val version: Long,
)

data class AdminRoleSummary(
    val id: String,
    val code: String,
    val organizationId: String?,
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

data class AdminPermissionSummary(
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
    val featureFlag: String?,
)
