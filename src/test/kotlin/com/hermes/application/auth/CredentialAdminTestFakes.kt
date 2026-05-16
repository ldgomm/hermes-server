package com.hermes.application.auth

import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

internal fun phase5Clock(): Clock =
    Clock.fixed(Instant.parse("2026-05-16T00:00:00Z"), ZoneOffset.UTC)

internal class Phase5FixedIdGenerator : AuthIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "${prefix}_$next"
    }
}

internal class TestPasswordHasher : PasswordHasher {
    override fun hash(password: CharArray): String = "hashed:" + password.concatToString()
    override fun verify(password: CharArray, encodedHash: String): Boolean =
        encodedHash == hash(password)
}

internal class RecordingAuditLogger : CredentialAuditLogger {
    val events = mutableListOf<CredentialAuditEvent>()
    override fun log(event: CredentialAuditEvent) {
        events += event
    }
}

internal class CredentialAdminState :
    UserRepository,
    UserCredentialRepository,
    OrganizationRepository,
    MembershipMutationRepository,
    RoleQueryRepository,
    InvitationRepository,
    PasswordResetTokenRepository,
    UserSessionRepository,
    RefreshTokenRepository {

    val users = mutableMapOf<String, User>()
    val credentials = mutableMapOf<String, UserCredential>()
    val organizations = mutableMapOf<String, Organization>()
    val memberships = mutableMapOf<String, OrganizationMembership>()
    val invitations = mutableMapOf<String, Invitation>()
    val resetTokens = mutableMapOf<String, PasswordResetToken>()
    val sessions = mutableMapOf<String, UserSession>()
    val refreshTokens = mutableMapOf<String, RefreshToken>()

    override fun existsUserByEmail(email: String): Boolean = users.values.any { it.email == email }
    override fun findUserByEmail(email: String): User? = users.values.firstOrNull { it.email == email }
    override fun findUserById(userId: String): User? = users[userId]
    override fun create(user: User) { users[user.id] = user }
    override fun update(user: User) { users[user.id] = user }

    override fun findByUserId(userId: String): UserCredential? = credentials.values.firstOrNull { it.userId == userId }
    override fun create(credential: UserCredential) { credentials[credential.id] = credential }
    override fun update(credential: UserCredential) { credentials[credential.id] = credential }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.values.any { it.countryCode == countryCode && it.taxId == taxId }

    override fun findOrganizationById(organizationId: String): Organization? = organizations[organizationId]
    override fun create(organization: Organization) { organizations[organization.id] = organization }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        memberships.values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun create(membership: OrganizationMembership) { memberships[membership.id] = membership }
    override fun update(membership: OrganizationMembership) { memberships[membership.id] = membership }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> =
        RoleSeed.all.filter { it.id in roleIds }

    override fun findRoleById(roleId: String): RoleDefinition? =
        RoleSeed.all.firstOrNull { it.id == roleId }

    override fun create(invitation: Invitation) { invitations[invitation.id] = invitation }
    override fun findInvitationByTokenHash(tokenHash: String): Invitation? =
        invitations.values.firstOrNull { it.tokenHash == tokenHash }

    override fun findPendingByOrganizationAndEmail(organizationId: String, email: String): Invitation? =
        invitations.values.firstOrNull {
            it.organizationId == organizationId &&
                it.email == email &&
                it.status == InvitationStatus.PENDING
        }

    override fun update(invitation: Invitation) { invitations[invitation.id] = invitation }

    override fun create(token: PasswordResetToken) { resetTokens[token.id] = token }
    override fun findPasswordResetTokenByHash(tokenHash: String): PasswordResetToken? =
        resetTokens.values.firstOrNull { it.tokenHash == tokenHash }

    override fun revokeActiveForUser(userId: String, revokedAt: Instant): Int {
        val active = resetTokens.values.filter { it.userId == userId && !it.isUsed && !it.isRevoked }
        active.forEach { resetTokens[it.id] = it.revoke(revokedAt) }
        return active.size
    }

    override fun update(token: PasswordResetToken) { resetTokens[token.id] = token }

    override fun create(session: UserSession) { sessions[session.id] = session }
    override fun findSessionById(sessionId: String): UserSession? = sessions[sessionId]
    override fun findActiveByUserId(userId: String): List<UserSession> =
        sessions.values.filter { it.userId == userId && it.status == UserSessionStatus.ACTIVE }

    override fun update(session: UserSession) { sessions[session.id] = session }

    override fun create(refreshToken: RefreshToken) { refreshTokens[refreshToken.id] = refreshToken }
    override fun findRefreshTokenByHash(tokenHash: String): RefreshToken? =
        refreshTokens.values.firstOrNull { it.tokenHash == tokenHash }

    override fun findActiveBySessionId(sessionId: String): List<RefreshToken> =
        refreshTokens.values.filter { it.sessionId == sessionId && !it.isUsed && !it.isRevoked }

    override fun update(refreshToken: RefreshToken) { refreshTokens[refreshToken.id] = refreshToken }

    override fun revokeActiveBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        val active = refreshTokens.values.filter { it.sessionId in sessionIds && !it.isRevoked }
        active.forEach { refreshTokens[it.id] = it.copy(revokedAt = revokedAt, version = it.version + 1) }
        return active.size
    }

    override fun rotate(oldToken: RefreshToken, newToken: RefreshToken) {
        refreshTokens[oldToken.id] = oldToken
        refreshTokens[newToken.id] = newToken
    }
}
