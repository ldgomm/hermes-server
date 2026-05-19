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
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminAccessUseCasesTest {
    @Test
    fun `creates organization custom role`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = CreateAdminRoleUseCase(
            repository = repository,
            idGenerator = AdminAccessIdGenerator { "role_custom_supervisor" },
        )

        val result = useCase.execute(
            CreateAdminRoleCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                code = "Supervisor",
                name = "Supervisor",
                description = "Puede revisar ventas y cobros",
                permissionKeys = setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.PAYMENTS_COLLECT),
                reason = "Crear rol operativo",
            )
        )

        assertEquals("role_custom_supervisor", result.role.id)
        assertEquals("supervisor", result.role.code)
        assertEquals("org_1", result.role.organizationId)
        assertEquals("CUSTOM", result.role.type)
        assertTrue(repository.roles.any { it.id == "role_custom_supervisor" })
    }

    @Test
    fun `logs audit when creating organization custom role`() {
        val repository = InMemoryAdminAccessRepository13B()
        val auditLogger = RecordingAdminAccessAuditLogger13B()
        val useCase = CreateAdminRoleUseCase(
            repository = repository,
            idGenerator = AdminAccessIdGenerator { "role_custom_audited" },
            auditLogger = auditLogger,
        )

        useCase.execute(
            CreateAdminRoleCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                code = "Audited",
                name = "Audited Role",
                description = "Audited role",
                permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
                reason = "Audit coverage",
            )
        )

        assertEquals(1, auditLogger.events.size)
        assertEquals(AdminAccessAuditAction.ROLE_CREATED, auditLogger.events.single().action)
        assertEquals("org_1", auditLogger.events.single().organizationId)
        assertEquals("usr_owner", auditLogger.events.single().actorUserId)
        assertEquals("role_custom_audited", auditLogger.events.single().targetId)
        assertEquals("Audit coverage", auditLogger.events.single().reason)
    }

    @Test
    fun `rejects duplicate role code when creating role`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = CreateAdminRoleUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminRoleCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    code = "Cashier",
                    name = "Cajero duplicado",
                    description = "No debe permitirse duplicar el código",
                    permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
                    reason = "Test duplicado",
                )
            )
        }
    }

    @Test
    fun `rejects unknown permission when creating role`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = CreateAdminRoleUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminRoleCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    code = "bad",
                    name = "Bad role",
                    description = "Invalid",
                    permissionKeys = setOf("unknown.permission"),
                    reason = "Test",
                )
            )
        }
    }

    @Test
    fun `rejects removing last organization administrator`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = UpdateAdminUserUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                UpdateAdminUserCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                    userId = "usr_owner",
                    roleIds = setOf("role_cashier"),
                    reason = "No debe quedar sin admin",
                )
            )
        }
    }

    @Test
    fun `rejects deactivating role that would remove last administrator`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = ChangeAdminRoleStatusUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.deactivate(
                ChangeAdminRoleStatusCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    roleId = "role_owner",
                    targetStatus = "INACTIVE",
                    reason = "No debe quedar sin admin",
                )
            )
        }
    }

    @Test
    fun `allows deactivating admin role when another active admin role remains`() {
        val repository = InMemoryAdminAccessRepository13B()
        repository.roles += adminRole13B(
            id = "role_admin_backup",
            code = "admin_backup",
            name = "Admin Backup",
        )
        repository.memberships.replaceAll {
            if (it.userId == "usr_owner") it.copy(roleIds = setOf("role_owner", "role_admin_backup")) else it
        }
        val auditLogger = RecordingAdminAccessAuditLogger13B()
        val useCase = ChangeAdminRoleStatusUseCase(repository, auditLogger = auditLogger)

        val result = useCase.deactivate(
            ChangeAdminRoleStatusCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                roleId = "role_owner",
                targetStatus = "INACTIVE",
                reason = "Rotar rol admin",
            )
        )

        assertEquals("INACTIVE", result.role.status)
        assertEquals(AdminAccessAuditAction.ROLE_DEACTIVATED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects updating role permissions when it would remove last administrator`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = UpdateAdminRoleUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                UpdateAdminRoleCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    roleId = "role_owner",
                    permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
                    reason = "No debe perder permisos admin",
                )
            )
        }
    }

    @Test
    fun `lists users with role names and sessions`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = ListAdminUsersUseCase(repository)

        val result = useCase.execute(
            ListAdminUsersCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_VIEW),
            )
        )

        assertEquals(1, result.users.size)
        assertEquals("Owner", result.users.first().displayName)
        assertTrue(result.users.first().roleNames.contains("Owner"))
    }

    @Test
    fun `lists active permissions`() {
        val repository = InMemoryAdminAccessRepository13B()
        val useCase = ListAdminPermissionsUseCase(repository)

        val result = useCase.execute(
            ListAdminPermissionsCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW),
            )
        )

        assertTrue(result.permissions.any { it.code == PermissionCatalog.CREDENTIALS_USERS_VIEW })
    }
}

private class InMemoryAdminAccessRepository13B : AdminAccessRepository {
    private val now = Instant.parse("2026-05-19T00:00:00Z")

    val users = mutableListOf(
        User.createOwner(
            id = "usr_owner",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
    )

    val memberships = mutableListOf(
        OrganizationMembership.owner(
            id = "mem_owner",
            organizationId = "org_1",
            userId = "usr_owner",
            ownerRoleId = "role_owner",
            now = now,
        )
    )

    val roles = mutableListOf(
        adminRole13B(
            id = "role_owner",
            code = "owner",
            name = "Owner",
            description = "Admin owner role",
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
    )

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> = memberships
        .filter { it.organizationId == organizationId }
        .filter { membership -> status == null || membership.status.name.equals(status, ignoreCase = true) }
        .mapNotNull { membership ->
            val user = users.firstOrNull { it.id == membership.userId } ?: return@mapNotNull null
            AdminUserAccessRecord(
                user = user,
                membership = membership,
                roles = findRolesByIds(membership.roleIds),
                activeSessionCount = 1,
            )
        }
        .take(limit)

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

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> =
        roles.filter { it.id in roleIds }

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

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> = emptyList()
    override fun updateSession(session: UserSession) = Unit
    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int = 0

    override fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String?,
        adminPermissionKeys: Set<String>,
    ): Int = memberships
        .filter { it.organizationId == organizationId && it.status == MembershipStatus.ACTIVE && it.userId != excludingUserId }
        .count { membership ->
            findRolesByIds(membership.roleIds).any { role ->
                role.status == RoleStatus.ACTIVE && role.permissionKeys.any { it in adminPermissionKeys }
            }
        }
}

private class RecordingAdminAccessAuditLogger13B : AdminAccessAuditLogger {
    val events = mutableListOf<AdminAccessAuditEvent>()

    override fun log(event: AdminAccessAuditEvent) {
        events += event
    }
}

private fun adminRole13B(
    id: String,
    code: String,
    name: String,
    description: String = "Admin role",
): RoleDefinition = RoleDefinition(
    id = id,
    code = code,
    organizationId = "org_1",
    scope = RoleScope.ORGANIZATION,
    type = RoleType.CUSTOM,
    name = name,
    description = description,
    permissionKeys = setOf(
        PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
        PermissionCatalog.CREDENTIALS_USERS_CREATE,
    ),
    systemRole = false,
    critical = false,
    editable = true,
    status = RoleStatus.ACTIVE,
)
