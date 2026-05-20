package com.hermes.application.admin.access

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.MembershipStatus
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
import com.hermes.domain.user.UserStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminUsersUseCasesTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `lists users with role names and active session counts`() {
        val repository = InMemoryAdminUsersRepository13B3()
        val result = ListAdminUsersUseCase(repository).execute(
            ListAdminUsersCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            )
        )

        assertEquals(2, result.users.size)
        val cashier = result.users.first { it.id == "usr_cashier" }
        assertEquals(listOf("Cashier"), cashier.roleNames)
        assertEquals(2, cashier.activeSessionCount)
    }

    @Test
    fun `gets user detail with effective permissions`() {
        val repository = InMemoryAdminUsersRepository13B3()
        val result = GetAdminUserUseCase(repository).execute(
            GetAdminUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
                userId = "usr_cashier",
            )
        )

        assertEquals("usr_cashier", result.user.id)
        assertTrue(PermissionCatalog.SALES_VIEW in result.user.effectivePermissions)
        assertEquals(2, result.user.activeSessionCount)
    }

    @Test
    fun `updates user profile and role assignment`() {
        val repository = InMemoryAdminUsersRepository13B3()
        val auditLogger = RecordingAdminAccessAuditLogger13B3()
        val result = UpdateAdminUserUseCase(repository, fixedClock, auditLogger).execute(
            UpdateAdminUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                userId = "usr_cashier",
                displayName = "Caja Principal",
                phone = "0999999999",
                roleIds = setOf("role_manager"),
                reason = "Promover a encargado de caja",
            )
        )

        assertEquals("Caja Principal", result.user.displayName)
        assertEquals("0999999999", result.user.phone)
        assertEquals(setOf("role_manager"), repository.memberships.first { it.userId == "usr_cashier" }.roleIds)
        assertEquals(AdminAccessAuditAction.USER_ACCESS_UPDATED, auditLogger.events.single().action)
    }

    @Test
    fun `blocks user suspends membership and revokes active sessions`() {
        val repository = InMemoryAdminUsersRepository13B3()
        val auditLogger = RecordingAdminAccessAuditLogger13B3()
        val result = BlockAdminUserUseCase(repository, fixedClock, auditLogger).execute(
            BlockAdminUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_BLOCK),
                userId = "usr_cashier",
                reason = "Salida del colaborador",
            )
        )

        assertEquals(UserStatus.BLOCKED.name, result.user.status)
        assertEquals(MembershipStatus.SUSPENDED.name, result.user.membershipStatus)
        assertEquals(0, result.user.activeSessionCount)
        assertTrue(repository.sessions.filter { it.userId == "usr_cashier" }
            .all { it.status == UserSessionStatus.REVOKED })
        assertEquals(2, repository.revokedRefreshTokens)
        assertEquals(AdminAccessAuditAction.USER_BLOCKED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects blocking yourself`() {
        val repository = InMemoryAdminUsersRepository13B3()

        assertFailsWith<DomainRuleViolation> {
            BlockAdminUserUseCase(repository, fixedClock).execute(
                BlockAdminUserCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_BLOCK),
                    userId = "usr_owner",
                    reason = "No permitido",
                )
            )
        }
    }

    @Test
    fun `rejects blocking last organization administrator`() {
        val repository = InMemoryAdminUsersRepository13B3()

        assertFailsWith<DomainRuleViolation> {
            BlockAdminUserUseCase(repository, fixedClock).execute(
                BlockAdminUserCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_support",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_BLOCK),
                    userId = "usr_owner",
                    reason = "Debe quedar al menos un admin",
                )
            )
        }
    }

    @Test
    fun `unblocks user and restores suspended membership`() {
        val repository = InMemoryAdminUsersRepository13B3().apply { blockCashier() }
        val result = UnblockAdminUserUseCase(repository, fixedClock).execute(
            UnblockAdminUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_UNBLOCK),
                userId = "usr_cashier",
                reason = "Reingreso autorizado",
            )
        )

        assertEquals(UserStatus.ACTIVE.name, result.user.status)
        assertEquals(MembershipStatus.ACTIVE.name, result.user.membershipStatus)
    }

    @Test
    fun `revokes user sessions without changing membership`() {
        val repository = InMemoryAdminUsersRepository13B3()
        val auditLogger = RecordingAdminAccessAuditLogger13B3()
        val result = RevokeAdminUserSessionsUseCase(repository, fixedClock, auditLogger).execute(
            RevokeAdminUserSessionsCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE),
                userId = "usr_cashier",
                reason = "Rotación preventiva de sesiones",
            )
        )

        assertEquals("usr_cashier", result.userId)
        assertEquals(2, result.revokedSessions)
        assertEquals(2, result.revokedRefreshTokens)
        assertEquals(MembershipStatus.ACTIVE, repository.memberships.first { it.userId == "usr_cashier" }.status)
        assertEquals(AdminAccessAuditAction.USER_SESSIONS_REVOKED, auditLogger.events.single().action)
    }
}

private class RecordingAdminAccessAuditLogger13B3 : AdminAccessAuditLogger {
    val events = mutableListOf<AdminAccessAuditEvent>()
    override fun log(event: AdminAccessAuditEvent) {
        events += event
    }
}

private class InMemoryAdminUsersRepository13B3 : AdminAccessRepository {
    private val now = Instant.parse("2026-05-19T00:00:00Z")
    var revokedRefreshTokens: Int = 0

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
                PermissionCatalog.CREDENTIALS_USERS_BLOCK,
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
            permissionKeys = setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.PAYMENTS_COLLECT),
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        ),
        RoleDefinition(
            id = "role_manager",
            code = "manager",
            organizationId = "org_1",
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Manager",
            description = "Manager role",
            permissionKeys = setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.PAYMENTS_VIEW),
            systemRole = false,
            critical = false,
            editable = true,
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
        UserSession.create(
            id = "ses_owner_1",
            userId = "usr_owner",
            now = now,
            expiresAt = now.plusSeconds(7200),
        ),
    )

    fun blockCashier() {
        val blockedAt = Instant.parse("2026-05-19T01:00:00Z")
        users.replaceAll { if (it.id == "usr_cashier") it.block("Fixture", blockedAt) else it }
        memberships.replaceAll {
            if (it.userId == "usr_cashier") it.copy(
                status = MembershipStatus.SUSPENDED, updatedAt = blockedAt, version = it.version + 1
            ) else it
        }
    }

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> {
        val normalizedQuery = query?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val normalizedStatus = status?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        return memberships.filter { it.organizationId == organizationId }
            .filter { normalizedStatus == null || it.status.name == normalizedStatus }.mapNotNull { membership ->
                val user = users.firstOrNull { it.id == membership.userId } ?: return@mapNotNull null
                if (normalizedQuery != null && normalizedQuery !in user.email.lowercase() && normalizedQuery !in user.displayName.lowercase()) {
                    return@mapNotNull null
                }
                AdminUserAccessRecord(
                    user = user,
                    membership = membership,
                    roles = findRolesByIds(membership.roleIds),
                    activeSessionCount = sessions.count { it.userId == user.id && it.status == UserSessionStatus.ACTIVE },
                )
            }.take(limit)
    }

    override fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord? =
        listUserAccess(organizationId, null, null, 100).firstOrNull { it.user.id == userId }

    override fun findUserById(userId: String): User? = users.firstOrNull { it.id == userId }

    override fun updateUser(user: User) {
        users.replaceAll { if (it.id == user.id) user else it }
    }

    override fun findMembership(organizationId: String, userId: String): OrganizationMembership? =
        memberships.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun updateMembership(membership: OrganizationMembership) {
        memberships.replaceAll { if (it.id == membership.id) membership else it }
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> = roles.filter { it.id in roleIds }

    override fun listRoles(organizationId: String, includeSystemTemplates: Boolean): List<RoleDefinition> =
        roles.filter { it.organizationId == organizationId }

    override fun findRole(organizationId: String, roleId: String): RoleDefinition? =
        roles.firstOrNull { it.id == roleId && it.organizationId == organizationId }

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
                findRolesByIds(membership.roleIds).any { role ->
                    role.status == RoleStatus.ACTIVE && role.permissionKeys.any { it in adminPermissionKeys }
                }
            }
}
