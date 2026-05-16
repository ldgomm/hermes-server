package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User

interface UserRepository {
    fun existsUserByEmail(email: String): Boolean
    fun findUserByEmail(email: String): User?
    fun findUserById(userId: String): User?
    fun create(user: User)
    fun update(user: User)
}

interface UserCredentialRepository {
    fun findByUserId(userId: String): UserCredential?
    fun create(credential: UserCredential)
    fun update(credential: UserCredential)
}

interface OrganizationRepository {
    fun existsByTaxId(countryCode: String, taxId: String): Boolean
    fun findOrganizationById(organizationId: String): Organization?
    fun create(organization: Organization)
}

interface OrganizationMembershipRepository {
    fun existsByOrganizationIdAndUserId(organizationId: String, userId: String): Boolean
    fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership?
    fun create(membership: OrganizationMembership)
}

interface AuthRoleLookupRepository {
    fun findSystemRoleByCode(code: String): RoleDefinition?
}

interface UserSessionRepository {
    fun create(session: UserSession)
    fun findSessionById(sessionId: String): UserSession?
    fun findActiveByUserId(userId: String): List<UserSession>
    fun update(session: UserSession)
}

interface RefreshTokenRepository {
    fun create(refreshToken: RefreshToken)
    fun findRefreshTokenByHash(tokenHash: String): RefreshToken?
    fun findActiveBySessionId(sessionId: String): List<RefreshToken>
    fun update(refreshToken: RefreshToken)
    fun revokeActiveBySessionIds(sessionIds: Set<String>, revokedAt: java.time.Instant): Int
    fun rotate(oldToken: RefreshToken, newToken: RefreshToken)
}

interface OwnerWorkspaceRepository {
    fun emailExists(email: String): Boolean
    fun organizationTaxIdExists(countryCode: String, taxId: String): Boolean
    fun findSystemRoleByCode(code: String): RoleDefinition?
    fun createOwnerWorkspace(
        user: User,
        credential: UserCredential,
        organization: Organization,
        membership: OrganizationMembership,
    )
}
