package com.hermes.application.auth

data class RegisterOwnerCommand(
    val email: String,
    val displayName: String,
    val password: String,
    val phone: String? = null,
)

data class CreateOrganizationCommand(
    val ownerUserId: String,
    val legalName: String,
    val commercialName: String,
    val taxId: String,
    val countryCode: String = "EC",
)

data class CreateOwnerMembershipCommand(
    val userId: String,
    val organizationId: String,
)

data class RegisterOwnerWorkspaceCommand(
    val ownerEmail: String,
    val ownerDisplayName: String,
    val ownerPassword: String,
    val ownerPhone: String? = null,
    val organizationLegalName: String,
    val organizationCommercialName: String,
    val organizationTaxId: String,
    val organizationCountryCode: String = "EC",
)

data class LoginCommand(
    val email: String,
    val password: String,
    val userAgent: String? = null,
    val ipAddress: String? = null,
)

data class RefreshSessionCommand(
    val refreshToken: String,
)

data class RevokeSessionCommand(
    val sessionId: String,
    val actorUserId: String,
    val reason: String = "User logout",
)

data class RevokeAllUserSessionsCommand(
    val targetUserId: String,
    val actorUserId: String,
    val reason: String,
    val organizationId: String? = null,
    val actorEffectivePermissions: Set<String> = emptySet(),
)
