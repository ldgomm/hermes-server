package com.hermes.infrastructure.mongo.admin.access

import com.hermes.application.admin.access.AdminAccessRepository
import com.hermes.application.admin.access.AdminUserAccessRecord
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.auth.AuthMongoCollectionNames
import com.hermes.infrastructure.mongo.auth.CredentialAdminMongoCollectionNames
import com.hermes.infrastructure.mongo.auth.MongoAuthMappers
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.set
import org.bson.Document
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

class MongoAdminAccessRepository(
    private val database: MongoDatabase,
) : AdminAccessRepository {

    private val users: MongoCollection<Document> = database.getCollection(MongoCollectionNames.USERS)
    private val memberships: MongoCollection<Document> = database.getCollection(MongoCollectionNames.MEMBERSHIPS)
    private val roles: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ROLES)
    private val invitations: MongoCollection<Document> =
        database.getCollection(CredentialAdminMongoCollectionNames.INVITATIONS)
    private val sessions: MongoCollection<Document> = database.getCollection(MongoCollectionNames.USER_SESSIONS)
    private val refreshTokens: MongoCollection<Document> =
        database.getCollection(AuthMongoCollectionNames.REFRESH_TOKENS)

    init {
        invitations.createIndex(
            Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.descending("createdAt")),
            IndexOptions().name("admin_access_invitations_org_created_idx"),
        )
        roles.createIndex(
            Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.ascending("code")),
            IndexOptions().name("admin_access_roles_org_code_idx"),
        )
    }

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> {
        val membershipFilter = buildList {
            add(eq("organizationId", organizationId.trim()))
            if (!status.isNullOrBlank()) add(eq("status", status.trim().lowercase()))
        }

        return memberships.find(and(membershipFilter)).limit(limit.coerceIn(1, 250)).into(mutableListOf())
            .mapNotNull { membershipDocument ->
                val membership = MongoAuthMappers.membershipFromDocument(membershipDocument)
                val user =
                    users.find(eq("_id", membership.userId)).firstOrNull()?.let(MongoAuthMappers::userFromDocument)
                        ?: return@mapNotNull null

                if (!query.matchesUserQuery(user)) return@mapNotNull null

                val roleList = findRolesByIds(membership.roleIds)
                AdminUserAccessRecord(
                    user = user,
                    membership = membership,
                    roles = roleList,
                    activeSessionCount = activeSessionCount(user.id),
                )
            }.sortedBy { it.user.displayName.lowercase() }
    }

    override fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord? {
        val membership = findMembership(organizationId, userId) ?: return null
        val user = findUserById(userId) ?: return null
        val roleList = findRolesByIds(membership.roleIds)
        return AdminUserAccessRecord(
            user = user,
            membership = membership,
            roles = roleList,
            activeSessionCount = activeSessionCount(user.id),
        )
    }

    override fun findUserById(userId: String): User? =
        users.find(eq("_id", userId.trim())).firstOrNull()?.let(MongoAuthMappers::userFromDocument)

    override fun updateUser(user: User) {
        users.replaceOne(eq("_id", user.id), MongoAuthMappers.userToDocument(user), ReplaceOptions().upsert(false))
    }

    override fun findMembership(organizationId: String, userId: String): OrganizationMembership? =
        memberships.find(and(eq("organizationId", organizationId.trim()), eq("userId", userId.trim()))).firstOrNull()
            ?.let(MongoAuthMappers::membershipFromDocument)

    override fun updateMembership(membership: OrganizationMembership) {
        memberships.replaceOne(
            eq("_id", membership.id),
            MongoAuthMappers.membershipToDocument(membership),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> {
        if (roleIds.isEmpty()) return emptyList()
        return roles.find(`in`("_id", roleIds)).into(mutableListOf()).map(MongoAdminAccessMappers::roleFromDocument)
    }

    override fun listRoles(organizationId: String, includeSystemTemplates: Boolean): List<RoleDefinition> {
        val filters = mutableListOf(
            and(eq("organizationId", organizationId.trim()), ne("status", "archived")),
        )
        if (includeSystemTemplates) {
            filters += and(
                eq("scope", RoleScope.ORGANIZATION.name.lowercase()), Filters.exists("organizationId", false)
            )
            filters += and(eq("scope", RoleScope.ORGANIZATION.name.lowercase()), eq("organizationId", null))
        }

        return roles.find(or(filters)).sort(Sorts.ascending("type", "name")).into(mutableListOf())
            .map(MongoAdminAccessMappers::roleFromDocument).distinctBy { it.id }
    }

    override fun findRole(organizationId: String, roleId: String): RoleDefinition? {
        val role = roles.find(eq("_id", roleId.trim())).firstOrNull()?.let(MongoAdminAccessMappers::roleFromDocument)
            ?: return null

        val visible =
            role.organizationId == organizationId.trim() || (role.scope == RoleScope.ORGANIZATION && role.organizationId == null)

        return role.takeIf { visible }
    }

    override fun existsRoleCode(organizationId: String, code: String, excludeRoleId: String?): Boolean {
        val filter = and(
            or(
                eq("organizationId", organizationId.trim()),
                Filters.exists("organizationId", false),
                eq("organizationId", null),
            ),
            eq("code", code.trim()),
        )
        return roles.find(filter).into(mutableListOf()).map(MongoAdminAccessMappers::roleFromDocument)
            .any { it.id != excludeRoleId }
    }

    override fun createRole(role: RoleDefinition) {
        roles.insertOne(MongoAdminAccessMappers.roleToDocument(role))
    }

    override fun updateRole(role: RoleDefinition) {
        roles.replaceOne(
            eq("_id", role.id), MongoAdminAccessMappers.roleToDocument(role), ReplaceOptions().upsert(false)
        )
    }

    override fun listInvitations(organizationId: String, status: String?, limit: Int): List<Invitation> {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            if (!status.isNullOrBlank()) add(eq("status", status.trim().uppercase()))
        }
        return invitations.find(and(filters)).sort(Sorts.descending("createdAt")).limit(limit.coerceIn(1, 250))
            .into(mutableListOf()).map(MongoAdminAccessMappers::invitationFromDocument)
    }

    override fun findInvitation(organizationId: String, invitationId: String): Invitation? =
        invitations.find(and(eq("_id", invitationId.trim()), eq("organizationId", organizationId.trim()))).firstOrNull()
            ?.let(MongoAdminAccessMappers::invitationFromDocument)

    override fun updateInvitation(invitation: Invitation) {
        invitations.replaceOne(
            eq("_id", invitation.id),
            MongoAdminAccessMappers.invitationToDocument(invitation),
            ReplaceOptions().upsert(false),
        )
    }

    override fun listPermissionDefinitions(includeReserved: Boolean): List<PermissionDefinition> =
        if (includeReserved) PermissionCatalog.definitions else PermissionCatalog.active

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> =
        sessions.find(and(eq("userId", userId.trim()), eq("status", "active"))).into(mutableListOf())
            .map(MongoAuthMappers::sessionFromDocument)

    override fun updateSession(session: UserSession) {
        sessions.replaceOne(
            eq("_id", session.id), MongoAuthMappers.sessionToDocument(session), ReplaceOptions().upsert(false)
        )
    }

    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        if (sessionIds.isEmpty()) return 0
        val result = refreshTokens.updateMany(
            and(
                `in`("sessionId", sessionIds),
                Filters.exists("revokedAt", false),
            ),
            set("revokedAt", Date.from(revokedAt)),
        )
        return result.modifiedCount.toInt()
    }

    override fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String?,
        adminPermissionKeys: Set<String>,
    ): Int =
        memberships.find(and(eq("organizationId", organizationId.trim()), eq("status", "active"))).into(mutableListOf())
            .map(MongoAuthMappers::membershipFromDocument).filterNot { it.userId == excludingUserId }
            .count { membership ->
                findRolesByIds(membership.roleIds).any { role ->
                    role.status == RoleStatus.ACTIVE && role.permissionKeys.any { it in adminPermissionKeys }
                }
            }

    private fun activeSessionCount(userId: String): Int =
        sessions.countDocuments(and(eq("userId", userId.trim()), eq("status", "active"))).toInt()

    private fun String?.matchesUserQuery(user: User): Boolean {
        val q = this?.trim()?.takeIf { it.isNotBlank() } ?: return true
        val pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE)
        return pattern.matcher(user.email).find() || pattern.matcher(user.displayName).find()
    }
}

private fun <T> com.mongodb.client.FindIterable<T>.firstOrNull(): T? = first()
