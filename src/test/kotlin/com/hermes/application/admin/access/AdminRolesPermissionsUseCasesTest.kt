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

class AdminRolesPermissionsUseCasesTest {
    @Test
    fun `rejects removing last role manager permission even when another admin permission remains`() {
        val repository = InMemoryAdminRolesRepository13B6()

        assertFailsWith<DomainRuleViolation> {
            UpdateAdminRoleUseCase(repository).execute(
                UpdateAdminRoleCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    roleId = "role_owner",
                    permissionKeys = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                    reason = "No debe perderse la administración de roles",
                )
            )
        }
    }

    @Test
    fun `allows removing role manager permission when another active role manager remains`() {
        val repository = InMemoryAdminRolesRepository13B6()
        repository.roles += adminRole13B6(
            id = "role_role_manager_backup",
            code = "role_manager_backup",
            name = "Role Manager Backup",
        )
        repository.memberships.replaceAll {
            if (it.userId == "usr_owner") it.copy(roleIds = setOf("role_owner", "role_role_manager_backup")) else it
        }

        val result = UpdateAdminRoleUseCase(repository).execute(
            UpdateAdminRoleCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                roleId = "role_owner",
                permissionKeys = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                reason = "Delegar administración de roles",
            )
        )

        assertEquals(setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE), result.role.permissionKeys)
    }

    @Test
    fun `rejects deactivating last role manager`() {
        val repository = InMemoryAdminRolesRepository13B6()

        assertFailsWith<DomainRuleViolation> {
            ChangeAdminRoleStatusUseCase(repository).deactivate(
                ChangeAdminRoleStatusCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_MANAGE),
                    roleId = "role_owner",
                    targetStatus = "INACTIVE",
                    reason = "No debe quedar sin gestor de roles",
                )
            )
        }
    }
}

private class InMemoryAdminRolesRepository13B6 : AdminAccessRepository {
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
        adminRole13B6(
            id = "role_owner",
            code = "owner",
            name = "Owner",
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
    ): List<AdminUserAccessRecord> = memberships.filter { it.organizationId == organizationId }
        .filter { status.isNullOrBlank() || it.status.name.equals(status, ignoreCase = true) }
        .mapNotNull { membership ->
            val user = users.firstOrNull { it.id == membership.userId } ?: return@mapNotNull null
            AdminUserAccessRecord(
                user = user,
                membership = membership,
                roles = findRolesByIds(membership.roleIds),
                activeSessionCount = 0,
            )
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

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> = emptyList()
    override fun updateSession(session: UserSession) = Unit
    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int = 0

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

private fun adminRole13B6(
    id: String,
    code: String,
    name: String,
): RoleDefinition = RoleDefinition(
    id = id,
    code = code,
    organizationId = "org_1",
    scope = RoleScope.ORGANIZATION,
    type = RoleType.CUSTOM,
    name = name,
    description = "Admin role",
    permissionKeys = setOf(
        PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
        PermissionCatalog.CREDENTIALS_USERS_CREATE,
    ),
    systemRole = false,
    critical = false,
    editable = true,
    status = RoleStatus.ACTIVE,
)
