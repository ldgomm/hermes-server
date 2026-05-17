package com.hermes.backend.auth

import com.hermes.application.auth.*
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.user.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterOwnerRequest(
    val email: String,
    val displayName: String,
    val password: String,
    val phone: String? = null,
)

@Serializable
data class RegisterOwnerWorkspaceRequest(
    val ownerEmail: String,
    val ownerDisplayName: String,
    val ownerPassword: String,
    val ownerPhone: String? = null,
    val organizationLegalName: String,
    val organizationCommercialName: String,
    val organizationTaxId: String,
    val organizationCountryCode: String = "EC",
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class RevokeSessionRequest(
    val sessionId: String? = null,
    val reason: String = "User logout",
)

@Serializable
data class RevokeAllUserSessionsRequest(
    val targetUserId: String? = null,
    val reason: String = "User logout from all sessions",
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val status: String,
    val phone: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CredentialResponse(
    val id: String,
    val userId: String,
    val status: String,
    val mustChangePassword: Boolean,
    val temporaryPassword: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RegisterOwnerResponse(
    val user: UserResponse,
    val credential: CredentialResponse,
)

@Serializable
data class RegisterOwnerWorkspaceResponse(
    val user: UserResponse,
    val credential: CredentialResponse,
    val organization: OrganizationResponse,
    val membership: OrganizationMembershipResponse,
)

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
    val sessionId: String,
    val userId: String,
    val mustChangePassword: Boolean,
)

@Serializable
data class RefreshSessionResponse(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String,
    val sessionId: String,
    val userId: String,
)

@Serializable
data class RevokeSessionResponse(
    val revokedSessions: Int,
    val revokedRefreshTokens: Int,
)

fun RegisterOwnerResult.toResponse(): RegisterOwnerResponse = RegisterOwnerResponse(
    user = user.toResponse(),
    credential = credential.toResponse(),
)

fun RegisterOwnerWorkspaceResult.toResponse(): RegisterOwnerWorkspaceResponse = RegisterOwnerWorkspaceResponse(
    user = user.toResponse(),
    credential = credential.toResponse(),
    organization = organization.toResponse(),
    membership = membership.toResponse(),
)

fun AuthTokenResult.toResponse(): AuthTokenResponse = AuthTokenResponse(
    accessToken = accessToken,
    accessTokenExpiresAt = accessTokenExpiresAt.toString(),
    refreshToken = refreshToken,
    refreshTokenExpiresAt = refreshTokenExpiresAt.toString(),
    sessionId = sessionId,
    userId = userId,
    mustChangePassword = mustChangePassword,
)

fun RefreshSessionResult.toResponse(): RefreshSessionResponse = RefreshSessionResponse(
    accessToken = accessToken,
    accessTokenExpiresAt = accessTokenExpiresAt.toString(),
    refreshToken = refreshToken,
    refreshTokenExpiresAt = refreshTokenExpiresAt.toString(),
    sessionId = sessionId,
    userId = userId,
)

fun RevokeSessionResult.toResponse(): RevokeSessionResponse = RevokeSessionResponse(
    revokedSessions = revokedSessions,
    revokedRefreshTokens = revokedRefreshTokens,
)

fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    email = email,
    displayName = displayName,
    status = status.name,
    phone = phone,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun UserCredential.toResponse(): CredentialResponse = CredentialResponse(
    id = id,
    userId = userId,
    status = status.name,
    mustChangePassword = mustChangePassword,
    temporaryPassword = temporaryPassword,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)