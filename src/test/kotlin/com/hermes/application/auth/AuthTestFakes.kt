package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
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
import java.util.ArrayDeque

fun fixedClock(instant: Instant = Instant.parse("2026-05-16T00:00:00Z")): Clock =
    Clock.fixed(instant, ZoneOffset.UTC)

class FakePasswordHasher : PasswordHasher {
    override fun hash(password: CharArray): String = "hash:${String(password)}"

    override fun verify(password: CharArray, encodedHash: String): Boolean =
        encodedHash == hash(password)
}

class FixedAuthIdGenerator : AuthIdGenerator {
    private val counters = mutableMapOf<String, Int>()

    override fun newId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "${prefix}_$next"
    }
}

class RecordingCredentialAuditLogger : CredentialAuditLogger {
    val events: MutableList<CredentialAuditEvent> = mutableListOf()

    override fun log(event: CredentialAuditEvent) {
        events += event
    }
}

class FakeOwnerWorkspaceRepository(
    private val existingEmails: MutableSet<String> = mutableSetOf(),
    private val existingTaxIds: MutableSet<Pair<String, String>> = mutableSetOf(),
) : OwnerWorkspaceRepository {
    val users: MutableMap<String, User> = mutableMapOf()
    val credentials: MutableMap<String, UserCredential> = mutableMapOf()
    val organizations: MutableMap<String, Organization> = mutableMapOf()
    val memberships: MutableMap<String, OrganizationMembership> = mutableMapOf()
    val auditLogger = RecordingCredentialAuditLogger()

    override fun emailExists(email: String): Boolean = email in existingEmails

    override fun organizationTaxIdExists(countryCode: String, taxId: String): Boolean =
        countryCode to taxId in existingTaxIds

    override fun findSystemRoleByCode(code: String): RoleDefinition? = RoleSeed.byCode[code]

    override fun createOwnerWorkspace(
        user: User,
        credential: UserCredential,
        organization: Organization,
        membership: OrganizationMembership,
    ) {
        existingEmails += user.email
        existingTaxIds += organization.countryCode to organization.taxId
        users[user.id] = user
        credentials[credential.userId] = credential
        organizations[organization.id] = organization
        memberships[membership.id] = membership
    }
}

class FakeAuthState(
    val users: MutableMap<String, User> = mutableMapOf(),
    val credentials: MutableMap<String, UserCredential> = mutableMapOf(),
    val organizations: MutableMap<String, Organization> = mutableMapOf(),
    val memberships: MutableMap<String, OrganizationMembership> = mutableMapOf(),
    val sessions: MutableMap<String, UserSession> = mutableMapOf(),
    val refreshTokens: MutableMap<String, RefreshToken> = mutableMapOf(),
) : UserRepository,
    UserCredentialRepository,
    OrganizationRepository,
    OrganizationMembershipRepository,
    AuthRoleLookupRepository,
    UserSessionRepository,
    RefreshTokenRepository {

    val auditLogger = RecordingCredentialAuditLogger()
    private val idGenerator = FixedAuthIdGenerator()
    private val jwtTokenService = HmacJwtTokenService(
        secret = "01234567890123456789012345678901",
        issuer = "hermes-test",
        accessTokenTtlSeconds = 900,
    )
    private val securityPolicy = AuthSecurityPolicy(
        accessTokenTtl = java.time.Duration.ofMinutes(15),
        refreshTokenTtl = java.time.Duration.ofDays(30),
        sessionTtl = java.time.Duration.ofDays(30),
        maxFailedLoginAttempts = 5,
        credentialLockDuration = java.time.Duration.ofMinutes(15),
    )

    fun loginUseCase(): LoginUseCase = LoginUseCase(
        userRepository = this,
        credentialRepository = this,
        sessionRepository = this,
        refreshTokenRepository = this,
        passwordHasher = FakePasswordHasher(),
        sessionFactory = AuthSessionFactory(
            idGenerator = idGenerator,
            tokenGenerator = SecureTokenGenerator(),
            jwtTokenService = jwtTokenService,
            policy = securityPolicy,
        ),
        securityPolicy = securityPolicy,
        auditLogger = auditLogger,
        clock = fixedClock(),
    )

    fun refreshUseCase(): RefreshSessionUseCase = RefreshSessionUseCase(
        userRepository = this,
        sessionRepository = this,
        refreshTokenRepository = this,
        jwtTokenService = jwtTokenService,
        tokenGenerator = SecureTokenGenerator(),
        idGenerator = idGenerator,
        securityPolicy = securityPolicy,
        auditLogger = auditLogger,
        clock = fixedClock(),
    )

    fun revokeUseCase(): RevokeSessionUseCase = RevokeSessionUseCase(
        sessionRepository = this,
        refreshTokenRepository = this,
        auditLogger = auditLogger,
        clock = fixedClock(),
    )

    override fun existsUserByEmail(email: String): Boolean = users.values.any { it.email == email }

    override fun findUserByEmail(email: String): User? = users.values.firstOrNull { it.email == email }

    override fun findUserById(userId: String): User? = users[userId]

    override fun create(user: User) {
        users[user.id] = user
    }

    override fun update(user: User) {
        users[user.id] = user
    }

    override fun findByUserId(userId: String): UserCredential? = credentials[userId]

    override fun create(credential: UserCredential) {
        credentials[credential.userId] = credential
    }

    override fun update(credential: UserCredential) {
        credentials[credential.userId] = credential
    }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.values.any { it.countryCode == countryCode && it.taxId == taxId }

    override fun findOrganizationById(organizationId: String): Organization? = organizations[organizationId]

    override fun create(organization: Organization) {
        organizations[organization.id] = organization
    }

    override fun existsByOrganizationIdAndUserId(organizationId: String, userId: String): Boolean =
        memberships.values.any { it.organizationId == organizationId && it.userId == userId }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        memberships.values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun create(membership: OrganizationMembership) {
        memberships[membership.id] = membership
    }

    override fun findSystemRoleByCode(code: String): RoleDefinition? = RoleSeed.byCode[code]

    override fun create(session: UserSession) {
        sessions[session.id] = session
    }

    override fun findSessionById(sessionId: String): UserSession? = sessions[sessionId]

    override fun findActiveByUserId(userId: String): List<UserSession> =
        sessions.values.filter { it.userId == userId && it.status == UserSessionStatus.ACTIVE }

    override fun update(session: UserSession) {
        sessions[session.id] = session
    }

    override fun create(refreshToken: RefreshToken) {
        refreshTokens[refreshToken.id] = refreshToken
    }

    override fun findRefreshTokenByHash(tokenHash: String): RefreshToken? =
        refreshTokens.values.firstOrNull { it.tokenHash == tokenHash }

    override fun findActiveBySessionId(sessionId: String): List<RefreshToken> =
        refreshTokens.values.filter { it.sessionId == sessionId && !it.isRevoked && !it.isUsed }

    override fun update(refreshToken: RefreshToken) {
        refreshTokens[refreshToken.id] = refreshToken
    }

    override fun revokeActiveBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        val tokens = refreshTokens.values.filter { it.sessionId in sessionIds && !it.isRevoked }
        tokens.forEach { token ->
            refreshTokens[token.id] = token.copy(revokedAt = revokedAt, version = token.version + 1)
        }
        return tokens.size
    }

    override fun rotate(oldToken: RefreshToken, newToken: RefreshToken) {
        refreshTokens[oldToken.id] = oldToken
        refreshTokens[newToken.id] = newToken
    }
}
