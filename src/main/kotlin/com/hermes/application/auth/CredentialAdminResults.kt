package com.hermes.application.auth

import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.user.User
import java.time.Instant

data class InviteUserResult(
    val invitation: Invitation,
    val user: User,
    val membership: OrganizationMembership,
    val rawInvitationToken: String,
    val invitationUrl: String? = null,
)

data class AcceptInvitationResult(
    val user: User,
    val credential: UserCredential,
    val membership: OrganizationMembership,
    val invitation: Invitation,
)

data class CreateTemporaryUserResult(
    val user: User,
    val credential: UserCredential,
    val membership: OrganizationMembership,
    val temporaryPassword: String,
)

data class ChangePasswordResult(
    val userId: String,
    val changedAt: Instant,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

data class RequestPasswordResetResult(
    val accepted: Boolean = true,
    val rawResetToken: String? = null,
    val resetUrl: String? = null,
    val expiresAt: Instant? = null,
)

data class ConfirmPasswordResetResult(
    val userId: String,
    val resetToken: PasswordResetToken,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

data class UserBlockResult(
    val user: User,
    val membership: OrganizationMembership?,
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

data class UserUnblockResult(
    val user: User,
    val membership: OrganizationMembership?,
)
