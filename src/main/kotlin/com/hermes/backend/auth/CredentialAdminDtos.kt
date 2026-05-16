package com.hermes.backend.auth

import com.hermes.application.auth.*
import kotlinx.serialization.Serializable

@Serializable
data class InviteUserRequest(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
)

@Serializable
data class AcceptInvitationRequest(
    val invitationToken: String,
    val password: String,
    val displayName: String? = null,
    val phone: String? = null,
)

@Serializable
data class CreateTemporaryUserRequest(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val email: String,
    val displayName: String,
    val roleIds: Set<String>,
    val temporaryPassword: String? = null,
    val phone: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    val userId: String,
    val currentPassword: String,
    val newPassword: String,
    val sessionId: String? = null,
    val revokeOtherSessions: Boolean = true,
)

@Serializable
data class RequestPasswordResetRequest(
    val email: String,
)

@Serializable
data class ConfirmPasswordResetRequest(
    val resetToken: String,
    val newPassword: String,
)

@Serializable
data class BlockUserRequest(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

@Serializable
data class UnblockUserRequest(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

@Serializable
data class InviteUserResponse(
    val invitationId: String,
    val userId: String,
    val membershipId: String,
    val rawInvitationToken: String,
    val invitationUrl: String? = null,
    val expiresAt: String,
)

@Serializable
data class AcceptInvitationResponse(
    val userId: String,
    val credentialId: String,
    val membershipId: String,
    val invitationId: String,
)

@Serializable
data class CreateTemporaryUserResponse(
    val userId: String,
    val credentialId: String,
    val membershipId: String,
    val temporaryPassword: String,
    val mustChangePassword: Boolean,
)

@Serializable
data class ChangePasswordResponse(
    val userId: String,
    val changedAt: String,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

@Serializable
data class RequestPasswordResetResponse(
    val accepted: Boolean,
    val rawResetToken: String? = null,
    val resetUrl: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class ConfirmPasswordResetResponse(
    val userId: String,
    val resetTokenId: String,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

@Serializable
data class UserBlockResponse(
    val userId: String,
    val status: String,
    val membershipId: String?,
    val membershipStatus: String?,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

@Serializable
data class UserUnblockResponse(
    val userId: String,
    val status: String,
    val membershipId: String?,
    val membershipStatus: String?,
)

fun InviteUserResult.toCredentialResponse(): InviteUserResponse = InviteUserResponse(
    invitationId = invitation.id,
    userId = user.id,
    membershipId = membership.id,
    rawInvitationToken = rawInvitationToken,
    invitationUrl = invitationUrl,
    expiresAt = invitation.expiresAt.toString(),
)

fun AcceptInvitationResult.toCredentialResponse(): AcceptInvitationResponse = AcceptInvitationResponse(
    userId = user.id,
    credentialId = credential.id,
    membershipId = membership.id,
    invitationId = invitation.id,
)

fun CreateTemporaryUserResult.toCredentialResponse(): CreateTemporaryUserResponse = CreateTemporaryUserResponse(
    userId = user.id,
    credentialId = credential.id,
    membershipId = membership.id,
    temporaryPassword = temporaryPassword,
    mustChangePassword = credential.mustChangePassword,
)

fun ChangePasswordResult.toCredentialResponse(): ChangePasswordResponse = ChangePasswordResponse(
    userId = userId,
    changedAt = changedAt.toString(),
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
)

fun RequestPasswordResetResult.toCredentialResponse(): RequestPasswordResetResponse = RequestPasswordResetResponse(
    accepted = accepted,
    rawResetToken = rawResetToken,
    resetUrl = resetUrl,
    expiresAt = expiresAt?.toString(),
)

fun ConfirmPasswordResetResult.toCredentialResponse(): ConfirmPasswordResetResponse = ConfirmPasswordResetResponse(
    userId = userId,
    resetTokenId = resetToken.id,
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
)

fun UserBlockResult.toCredentialResponse(): UserBlockResponse = UserBlockResponse(
    userId = user.id,
    status = user.status.name,
    membershipId = membership?.id,
    membershipStatus = membership?.status?.name,
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
)

fun UserUnblockResult.toCredentialResponse(): UserUnblockResponse = UserUnblockResponse(
    userId = user.id,
    status = user.status.name,
    membershipId = membership?.id,
    membershipStatus = membership?.status?.name,
)
