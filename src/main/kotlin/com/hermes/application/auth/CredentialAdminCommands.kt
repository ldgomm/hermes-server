package com.hermes.application.auth

data class InviteUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class AcceptInvitationCommand(
    val invitationToken: String,
    val password: String,
    val displayName: String? = null,
    val phone: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class CreateTemporaryUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val temporaryPassword: String? = null,
    val phone: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class ChangePasswordCommand(
    val userId: String,
    val currentPassword: String,
    val newPassword: String,
    val sessionId: String? = null,
    val revokeOtherSessions: Boolean = true,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class RequestPasswordResetCommand(
    val email: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class ConfirmPasswordResetCommand(
    val resetToken: String,
    val newPassword: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class BlockUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val targetUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

data class UnblockUserCommand(
    val organizationId: String,
    val actorUserId: String,
    val targetUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)
