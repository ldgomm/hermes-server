package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import java.time.Instant

data class RegisterOwnerResult(
    val user: User,
    val credential: UserCredential,
)

data class CreateOrganizationResult(
    val organization: Organization,
)

data class CreateOwnerMembershipResult(
    val membership: OrganizationMembership,
)

data class RegisterOwnerWorkspaceResult(
    val user: User,
    val credential: UserCredential,
    val organization: Organization,
    val membership: OrganizationMembership,
)

data class AuthTokenResult(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val sessionId: String,
    val userId: String,
    val mustChangePassword: Boolean,
)

data class RefreshSessionResult(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val sessionId: String,
    val userId: String,
)

data class RevokeSessionResult(
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

data class CreatedSessionBundle(
    val session: UserSession,
    val refreshToken: RefreshToken,
    val rawRefreshToken: String,
    val accessToken: JwtAccessToken,
)
