package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User

class FakeAuthContextRepository : AuthContextRepository {
    val users: MutableMap<String, User> = mutableMapOf()
    val sessions: MutableMap<String, UserSession> = mutableMapOf()
    val memberships: MutableMap<String, OrganizationMembership> = mutableMapOf()
    val organizations: MutableMap<String, Organization> = mutableMapOf()
    val roles: MutableMap<String, RoleDefinition> = mutableMapOf()

    override fun findUserById(userId: String): User? = users[userId]

    override fun findSessionById(sessionId: String): UserSession? = sessions[sessionId]

    override fun findMembershipsByUserId(userId: String): List<OrganizationMembership> =
        memberships.values.filter { it.userId == userId }

    override fun findOrganizationById(organizationId: String): Organization? = organizations[organizationId]

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> =
        roleIds.mapNotNull { roles[it] }
}
