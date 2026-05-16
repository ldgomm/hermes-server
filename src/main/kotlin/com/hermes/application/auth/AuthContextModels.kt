package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User

/**
 * User + session resolved from a valid access token.
 *
 * This object deliberately does not include organization data. A user can belong
 * to many organizations, so organization resolution is handled by
 * ActiveOrganizationResolverUseCase.
 */
data class AuthenticatedPrincipal(
    val user: User,
    val session: UserSession,
    val claims: JwtClaims,
)

/**
 * Active organization selected for the current request.
 */
data class ActiveOrganizationContext(
    val organization: Organization,
    val membership: OrganizationMembership,
)

/**
 * Roles and final permission set for a user inside one organization.
 */
data class EffectivePermissionContext(
    val organizationId: String,
    val membership: OrganizationMembership,
    val roles: List<RoleDefinition>,
    val permissions: Set<String>,
) {
    fun canPerform(permission: String): Boolean =
        AuthorizationPolicy.canPerform(permissions, permission)

    fun requirePermission(permission: String) {
        AuthorizationPolicy.requirePermission(permissions, permission)
    }
}

/**
 * Complete request context attached by the backend auth middleware.
 */
data class AuthenticatedRequestContext(
    val principal: AuthenticatedPrincipal,
    val activeOrganization: ActiveOrganizationContext?,
    val effectivePermissions: EffectivePermissionContext?,
) {
    val user: User get() = principal.user
    val session: UserSession get() = principal.session
    val userId: String get() = principal.user.id
    val sessionId: String get() = principal.session.id
    val organizationId: String? get() = activeOrganization?.organization?.id

    fun requireActiveOrganization(): ActiveOrganizationContext =
        activeOrganization ?: throw com.hermes.domain.shared.DomainRuleViolation("Active organization is required.")

    fun requirePermission(permission: String) {
        val permissions = effectivePermissions
            ?: throw com.hermes.domain.shared.DomainRuleViolation("Effective permissions are required.")
        permissions.requirePermission(permission)
    }
}

data class MembershipContext(
    val membership: OrganizationMembership,
    val organization: Organization?,
    val roles: List<RoleDefinition>,
)

data class MeResult(
    val principal: AuthenticatedPrincipal,
    val memberships: List<MembershipContext>,
    val activeOrganization: ActiveOrganizationContext?,
    val effectivePermissions: EffectivePermissionContext?,
)
