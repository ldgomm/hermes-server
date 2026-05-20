package com.hermes.application.admin.access

import com.hermes.application.auth.*
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminTemporaryUsersAndPasswordResetUseCasesTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `creates temporary user with admin response and audit event`() {
        val repository = InMemoryAdminAccessRepository13B4()
        val accessAuditLogger = RecordingAdminAccessAuditLogger13B4()
        val credentialAuditLogger = RecordingCredentialAuditLogger13B4()
        val delegate = temporaryUserDelegate(repository, credentialAuditLogger)
        val useCase = CreateAdminTemporaryUserUseCase(
            delegate = delegate,
            accessRepository = repository,
            auditLogger = accessAuditLogger,
            clock = fixedClock,
        )

        val result = useCase.execute(
            CreateAdminTemporaryUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                email = "operator@hermes.local",
                displayName = "Operador Caja",
                phone = "0991112222",
                roleIds = setOf("role_cashier"),
                temporaryPassword = "StrongTmp123!",
                reason = "Alta operativa de caja",
            )
        )

        assertEquals("usr_001", result.user.id)
        assertEquals("cred_001", result.credentialId)
        assertEquals("mem_001", result.membershipId)
        assertEquals("StrongTmp123!", result.temporaryPassword)
        assertTrue(result.mustChangePassword)
        assertEquals(setOf("role_cashier"), repository.memberships.first { it.userId == "usr_001" }.roleIds)
        assertEquals(AdminAccessAuditAction.TEMPORARY_USER_CREATED, accessAuditLogger.events.single().action)
        assertTrue(credentialAuditLogger.events.any { it.targetUserId == "usr_001" })
    }

    @Test
    fun `rejects temporary user with platform role`() {
        val repository = InMemoryAdminAccessRepository13B4()
        val useCase = CreateAdminTemporaryUserUseCase(
            delegate = temporaryUserDelegate(repository),
            accessRepository = repository,
            clock = fixedClock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminTemporaryUserCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                    email = "platform@hermes.local",
                    displayName = "Platform User",
                    roleIds = setOf("role_platform_support"),
                    temporaryPassword = "StrongTmp123!",
                    reason = "No permitido",
                )
            )
        }
    }

    @Test
    fun `admin reset password creates temporary password forces change and revokes sessions`() {
        val repository = InMemoryAdminAccessRepository13B4()
        val accessAuditLogger = RecordingAdminAccessAuditLogger13B4()
        val credentialAuditLogger = RecordingCredentialAuditLogger13B4()
        val useCase = AdminResetUserPasswordUseCase(
            accessRepository = repository,
            credentialRepository = repository,
            passwordPolicy = PasswordPolicy(),
            passwordHasher = PlainPasswordHasher13B4,
            tokenGenerator = SecureTokenGenerator(),
            auditLogger = credentialAuditLogger,
            adminAccessAuditLogger = accessAuditLogger,
            clock = fixedClock,
        )

        val result = useCase.execute(
            AdminResetUserPasswordCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD),
                userId = "usr_cashier",
                temporaryPassword = "ResetTmp123!",
                revokeSessions = true,
                reason = "Olvido de contraseña",
            )
        )

        assertEquals("usr_cashier", result.userId)
        assertEquals("cred_cashier", result.credentialId)
        assertEquals("ResetTmp123!", result.temporaryPassword)
        assertTrue(result.mustChangePassword)
        assertEquals(2, result.revokedSessions)
        assertEquals(2, result.revokedRefreshTokens)
        assertTrue(repository.sessions.filter { it.userId == "usr_cashier" }
            .all { it.status == UserSessionStatus.REVOKED })
        assertEquals(AdminAccessAuditAction.USER_PASSWORD_RESET, accessAuditLogger.events.single().action)
        assertTrue(credentialAuditLogger.events.any { it.targetUserId == "usr_cashier" })
    }

    @Test
    fun `admin reset password rejects acting user`() {
        val repository = InMemoryAdminAccessRepository13B4()
        val useCase = AdminResetUserPasswordUseCase(
            accessRepository = repository,
            credentialRepository = repository,
            passwordPolicy = PasswordPolicy(),
            passwordHasher = PlainPasswordHasher13B4,
            tokenGenerator = SecureTokenGenerator(),
            clock = fixedClock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                AdminResetUserPasswordCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD),
                    userId = "usr_owner",
                    temporaryPassword = "ResetTmp123!",
                    reason = "No permitido",
                )
            )
        }
    }

    private fun temporaryUserDelegate(
        repository: InMemoryAdminAccessRepository13B4,
        credentialAuditLogger: CredentialAuditLogger = RecordingCredentialAuditLogger13B4(),
    ): CreateTemporaryUserUseCase = CreateTemporaryUserUseCase(
        userRepository = repository,
        credentialRepository = repository,
        organizationRepository = repository,
        membershipRepository = repository,
        roleRepository = repository,
        idGenerator = SequentialAuthIdGenerator13B4(),
        passwordPolicy = PasswordPolicy(),
        passwordHasher = PlainPasswordHasher13B4,
        tokenGenerator = SecureTokenGenerator(),
        auditLogger = credentialAuditLogger,
        clock = fixedClock,
    )
}

private class RecordingAdminAccessAuditLogger13B4 : AdminAccessAuditLogger {
    val events = mutableListOf<AdminAccessAuditEvent>()
    override fun log(event: AdminAccessAuditEvent) {
        events += event
    }
}

private class RecordingCredentialAuditLogger13B4 : CredentialAuditLogger {
    val events = mutableListOf<CredentialAuditEvent>()
    override fun log(event: CredentialAuditEvent) {
        events += event
    }
}

private object PlainPasswordHasher13B4 : PasswordHasher {
    override fun hash(password: CharArray): String = "hash:" + password.concatToString()
    override fun verify(password: CharArray, encodedHash: String): Boolean = encodedHash == hash(password)
}

private class SequentialAuthIdGenerator13B4 : AuthIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "${prefix}_${next.toString().padStart(3, '0')}"
    }
}

private class InMemoryAdminAccessRepository13B4 : AdminAccessRepository, UserRepository, UserCredentialRepository,
    OrganizationRepository, MembershipMutationRepository, RoleQueryRepository {

    private val now = Instant.parse("2026-05-19T00:00:00Z")
    var revokedRefreshTokens: Int = 0

    val organizations = mutableListOf(
        Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790012345001",
            legalName = "Hermes Test S.A.",
            commercialName = "Hermes Test",
            ownerUserId = "usr_owner",
            now = now,
        )
    )

    val users = mutableListOf(
        User.createOwner(
            id = "usr_owner",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        ),
        User.createOwner(
            id = "usr_cashier",
            email = "cashier@hermes.local",
            displayName = "Cashier User",
            phone = "0990000001",
            now = now,
        ),
    )

    val credentials = mutableListOf(
        UserCredential.createPasswordCredential(
            id = "cred_owner",
            userId = "usr_owner",
            passwordHash = "hash:OwnerPass123!",
            now = now,
        ),
        UserCredential.createPasswordCredential(
            id = "cred_cashier",
            userId = "usr_cashier",
            passwordHash = "hash:CashierPass123!",
            now = now,
        ),
    )

    val memberships = mutableListOf(
        OrganizationMembership.owner(
            id = "mem_owner",
            organizationId = "org_1",
            userId = "usr_owner",
            ownerRoleId = "role_owner",
            now = now,
        ),
        OrganizationMembership.owner(
            id = "mem_cashier",
            organizationId = "org_1",
            userId = "usr_cashier",
            ownerRoleId = "role_cashier",
            now = now,
        ),
    )

    val roles = mutableListOf(
        RoleDefinition(
            id = "role_owner",
            code = "owner",
            organizationId = "org_1",
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Owner",
            description = "Admin owner role",
            permissionKeys = setOf(
                PermissionCatalog.CREDENTIALS_USERS_CREATE,
                PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD,
                PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
            ),
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        ),
        RoleDefinition(
            id = "role_cashier",
            code = "cashier",
            organizationId = "org_1",
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Cashier",
            description = "Cashier role",
            permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        ),
        RoleDefinition(
            id = "role_platform_support",
            code = "platform_support",
            organizationId = null,
            scope = RoleScope.PLATFORM,
            type = RoleType.SYSTEM,
            name = "Platform Support",
            description = "Platform support role",
            permissionKeys = setOf(PermissionCatalog.ALL),
            systemRole = true,
            critical = true,
            editable = false,
            status = RoleStatus.ACTIVE,
        ),
    )

    val sessions = mutableListOf(
        UserSession.create(
            id = "ses_cashier_1",
            userId = "usr_cashier",
            now = now,
            expiresAt = now.plusSeconds(3600),
        ),
        UserSession.create(
            id = "ses_cashier_2",
            userId = "usr_cashier",
            now = now,
            expiresAt = now.plusSeconds(7200),
        ),
    )

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> = memberships.filter { it.organizationId == organizationId }
        .filter { status.isNullOrBlank() || it.status.name.equals(status, ignoreCase = true) }
        .mapNotNull { membership ->
            val user = users.firstOrNull { it.id == membership.userId } ?: return@mapNotNull null
            AdminUserAccessRecord(
                user = user,
                membership = membership,
                roles = findRolesByIds(membership.roleIds),
                activeSessionCount = sessions.count { it.userId == user.id && it.status == UserSessionStatus.ACTIVE },
            )
        }.take(limit)

    override fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord? =
        listUserAccess(organizationId, null, null, 100).firstOrNull { it.user.id == userId }

    override fun existsUserByEmail(email: String): Boolean = users.any { it.email == email.trim().lowercase() }
    override fun findUserByEmail(email: String): User? = users.firstOrNull { it.email == email.trim().lowercase() }
    override fun findUserById(userId: String): User? = users.firstOrNull { it.id == userId }
    override fun create(user: User) {
        users += user
    }

    override fun update(user: User) {
        updateUser(user)
    }

    override fun updateUser(user: User) {
        users.replaceAll { if (it.id == user.id) user else it }
    }

    override fun findByUserId(userId: String): UserCredential? = credentials.firstOrNull { it.userId == userId }
    override fun create(credential: UserCredential) {
        credentials += credential
    }

    override fun update(credential: UserCredential) {
        credentials.replaceAll { if (it.id == credential.id) credential else it }
    }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.any { it.countryCode == countryCode && it.taxId == taxId }

    override fun findOrganizationById(organizationId: String): Organization? =
        organizations.firstOrNull { it.id == organizationId }

    override fun create(organization: Organization) {
        organizations += organization
    }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        findMembership(organizationId, userId)

    override fun findMembership(organizationId: String, userId: String): OrganizationMembership? =
        memberships.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun create(membership: OrganizationMembership) {
        memberships += membership
    }

    override fun update(membership: OrganizationMembership) {
        updateMembership(membership)
    }

    override fun updateMembership(membership: OrganizationMembership) {
        memberships.replaceAll { if (it.id == membership.id) membership else it }
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> = roles.filter { it.id in roleIds }
    override fun findRoleById(roleId: String): RoleDefinition? = roles.firstOrNull { it.id == roleId }
    override fun listRoles(organizationId: String, includeSystemTemplates: Boolean): List<RoleDefinition> =
        roles.filter { it.organizationId == organizationId || includeSystemTemplates && it.organizationId == null }

    override fun findRole(organizationId: String, roleId: String): RoleDefinition? =
        roles.firstOrNull { it.id == roleId && (it.organizationId == organizationId || it.organizationId == null) }

    override fun existsRoleCode(organizationId: String, code: String, excludeRoleId: String?): Boolean =
        roles.any { it.organizationId == organizationId && it.code == code && it.id != excludeRoleId }

    override fun createRole(role: RoleDefinition) {
        roles += role
    }

    override fun updateRole(role: RoleDefinition) {
        roles.replaceAll { if (it.id == role.id) role else it }
    }

    override fun listInvitations(organizationId: String, status: String?, limit: Int): List<Invitation> = emptyList()
    override fun findInvitation(organizationId: String, invitationId: String): Invitation? = null
    override fun updateInvitation(invitation: Invitation) = Unit

    override fun listPermissionDefinitions(includeReserved: Boolean): List<PermissionDefinition> =
        if (includeReserved) PermissionCatalog.definitions else PermissionCatalog.active

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> =
        sessions.filter { it.userId == userId && it.status == UserSessionStatus.ACTIVE }

    override fun updateSession(session: UserSession) {
        sessions.replaceAll { if (it.id == session.id) session else it }
    }

    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        revokedRefreshTokens += sessionIds.size
        return sessionIds.size
    }

    override fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String?,
        adminPermissionKeys: Set<String>,
    ): Int =
        memberships.filter { it.organizationId == organizationId && it.status == MembershipStatus.ACTIVE && it.userId != excludingUserId }
            .count { membership ->
                findRolesByIds(membership.roleIds).any { role -> role.permissionKeys.any { it in adminPermissionKeys } }
            }
}
