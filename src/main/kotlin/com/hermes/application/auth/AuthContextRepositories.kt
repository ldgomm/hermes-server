package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User

interface AuthContextRepository {
    fun findUserById(userId: String): User?
    fun findSessionById(sessionId: String): UserSession?
    fun findMembershipsByUserId(userId: String): List<OrganizationMembership>
    fun findOrganizationById(organizationId: String): Organization?
    fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition>
}
