package com.hermes.application.admin.access

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User

interface AdminAccessRepository {
    fun listUserAccess(
        organizationId: String,
        query: String? = null,
        status: String? = null,
        limit: Int = 100,
    ): List<AdminUserAccessRecord>

    fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord?

    fun findUserById(userId: String): User?
    fun updateUser(user: User)

    fun findMembership(organizationId: String, userId: String): OrganizationMembership?
    fun updateMembership(membership: OrganizationMembership)

    fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition>
    fun listRoles(organizationId: String, includeSystemTemplates: Boolean = true): List<RoleDefinition>
    fun findRole(organizationId: String, roleId: String): RoleDefinition?
    fun existsRoleCode(organizationId: String, code: String, excludeRoleId: String? = null): Boolean
    fun createRole(role: RoleDefinition)
    fun updateRole(role: RoleDefinition)

    fun listInvitations(organizationId: String, status: String? = null, limit: Int = 100): List<Invitation>
    fun findInvitation(organizationId: String, invitationId: String): Invitation?
    fun updateInvitation(invitation: Invitation)

    fun listPermissionDefinitions(includeReserved: Boolean = false): List<PermissionDefinition>

    fun findActiveSessionsByUserId(userId: String): List<UserSession>
    fun updateSession(session: UserSession)
    fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: java.time.Instant): Int

    fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String? = null,
        adminPermissionKeys: Set<String>,
    ): Int
}

data class AdminUserAccessRecord(
    val user: User,
    val membership: OrganizationMembership,
    val roles: List<RoleDefinition>,
    val activeSessionCount: Int,
)
