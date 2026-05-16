package com.hermes.infrastructure.mongo.auth

import com.hermes.application.auth.InvitationRepository
import com.hermes.application.auth.MembershipMutationRepository
import com.hermes.application.auth.PasswordResetTokenRepository
import com.hermes.application.auth.RoleQueryRepository
import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import java.time.Instant

class MongoCredentialAdminStore(
    private val database: MongoDatabase,
) : InvitationRepository,
    PasswordResetTokenRepository,
    MembershipMutationRepository,
    RoleQueryRepository {

    private val invitations get() = database.getCollection(CredentialAdminMongoCollectionNames.INVITATIONS)
    private val resetTokens get() = database.getCollection(CredentialAdminMongoCollectionNames.PASSWORD_RESET_TOKENS)
    private val memberships get() = database.getCollection(CredentialAdminMongoCollectionNames.MEMBERSHIPS)
    private val roles get() = database.getCollection(CredentialAdminMongoCollectionNames.ROLES)

    override fun create(invitation: Invitation) {
        invitations.insertOne(invitation.toDocument())
    }

    override fun findInvitationByTokenHash(tokenHash: String): Invitation? =
        invitations.find(Filters.eq("tokenHash", tokenHash)).firstOrNull()?.toInvitation()

    override fun findPendingByOrganizationAndEmail(organizationId: String, email: String): Invitation? =
        invitations.find(
            Filters.and(
                Filters.eq("organizationId", organizationId),
                Filters.eq("email", email),
                Filters.eq("status", InvitationStatus.PENDING.name),
            )
        ).firstOrNull()?.toInvitation()

    override fun update(invitation: Invitation) {
        invitations.replaceOne(Filters.eq("_id", invitation.id), invitation.toDocument(), ReplaceOptions().upsert(false))
    }

    override fun create(token: PasswordResetToken) {
        resetTokens.insertOne(token.toDocument())
    }

    override fun findPasswordResetTokenByHash(tokenHash: String): PasswordResetToken? =
        resetTokens.find(Filters.eq("tokenHash", tokenHash)).firstOrNull()?.toPasswordResetToken()

    override fun revokeActiveForUser(userId: String, revokedAt: Instant): Int {
        val active = resetTokens.find(
            Filters.and(
                Filters.eq("userId", userId),
                Filters.exists("usedAt", false),
                Filters.exists("revokedAt", false),
            )
        ).toList()

        active.forEach { document ->
            val token = document.toPasswordResetToken().revoke(revokedAt)
            resetTokens.replaceOne(Filters.eq("_id", token.id), token.toDocument())
        }

        return active.size
    }

    override fun update(token: PasswordResetToken) {
        resetTokens.replaceOne(Filters.eq("_id", token.id), token.toDocument(), ReplaceOptions().upsert(false))
    }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        memberships.find(
            Filters.and(
                Filters.eq("organizationId", organizationId),
                Filters.eq("userId", userId),
            )
        ).firstOrNull()?.toMembership()

    override fun create(membership: OrganizationMembership) {
        memberships.insertOne(membership.toDocument())
    }

    override fun update(membership: OrganizationMembership) {
        memberships.replaceOne(Filters.eq("_id", membership.id), membership.toDocument(), ReplaceOptions().upsert(false))
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> {
        if (roleIds.isEmpty()) return emptyList()
        return roles.find(Filters.`in`("_id", roleIds)).map { it.toRoleDefinition() }.toList()
    }

    override fun findRoleById(roleId: String): RoleDefinition? =
        roles.find(Filters.eq("_id", roleId)).firstOrNull()?.toRoleDefinition()

    private fun Invitation.toDocument(): Document = Document("_id", id)
        .append("organizationId", organizationId)
        .append("email", email)
        .append("invitedByUserId", invitedByUserId)
        .append("roleIds", roleIds.toList())
        .append("tokenHash", tokenHash)
        .append("status", status.name)
        .append("createdAt", createdAt.toString())
        .append("expiresAt", expiresAt.toString())
        .append("acceptedAt", acceptedAt?.toString())
        .append("revokedAt", revokedAt?.toString())
        .append("acceptedUserId", acceptedUserId)
        .append("version", version)

    private fun Document.toInvitation(): Invitation = Invitation(
        id = getString("_id"),
        organizationId = getString("organizationId"),
        email = getString("email"),
        invitedByUserId = getString("invitedByUserId"),
        roleIds = getList("roleIds", String::class.java).toSet(),
        tokenHash = getString("tokenHash"),
        status = InvitationStatus.valueOf(getString("status")),
        createdAt = Instant.parse(getString("createdAt")),
        expiresAt = Instant.parse(getString("expiresAt")),
        acceptedAt = getString("acceptedAt")?.let(Instant::parse),
        revokedAt = getString("revokedAt")?.let(Instant::parse),
        acceptedUserId = getString("acceptedUserId"),
        version = getLong("version") ?: 1L,
    )

    private fun PasswordResetToken.toDocument(): Document = Document("_id", id)
        .append("userId", userId)
        .append("tokenHash", tokenHash)
        .append("createdAt", createdAt.toString())
        .append("expiresAt", expiresAt.toString())
        .append("usedAt", usedAt?.toString())
        .append("revokedAt", revokedAt?.toString())
        .append("requestedByIp", requestedByIp)
        .append("requestedByUserAgent", requestedByUserAgent)
        .append("version", version)

    private fun Document.toPasswordResetToken(): PasswordResetToken = PasswordResetToken(
        id = getString("_id"),
        userId = getString("userId"),
        tokenHash = getString("tokenHash"),
        createdAt = Instant.parse(getString("createdAt")),
        expiresAt = Instant.parse(getString("expiresAt")),
        usedAt = getString("usedAt")?.let(Instant::parse),
        revokedAt = getString("revokedAt")?.let(Instant::parse),
        requestedByIp = getString("requestedByIp"),
        requestedByUserAgent = getString("requestedByUserAgent"),
        version = getLong("version") ?: 1L,
    )

    private fun OrganizationMembership.toDocument(): Document = Document("_id", id)
        .append("organizationId", organizationId)
        .append("userId", userId)
        .append("roleIds", roleIds.toList())
        .append("status", status.name)
        .append("createdAt", createdAt.toString())
        .append("updatedAt", updatedAt.toString())
        .append("invitedBy", invitedBy)
        .append("acceptedAt", acceptedAt?.toString())
        .append("revokedAt", revokedAt?.toString())
        .append("version", version)

    private fun Document.toMembership(): OrganizationMembership = OrganizationMembership(
        id = getString("_id"),
        organizationId = getString("organizationId"),
        userId = getString("userId"),
        roleIds = getList("roleIds", String::class.java).toSet(),
        status = MembershipStatus.valueOf(getString("status")),
        createdAt = Instant.parse(getString("createdAt")),
        updatedAt = Instant.parse(getString("updatedAt")),
        invitedBy = getString("invitedBy"),
        acceptedAt = getString("acceptedAt")?.let(Instant::parse),
        revokedAt = getString("revokedAt")?.let(Instant::parse),
        version = getLong("version") ?: 1L,
    )

    private fun Document.toRoleDefinition(): RoleDefinition = RoleDefinition(
        id = getString("_id"),
        code = getString("code"),
        organizationId = getString("organizationId"),
        scope = RoleScope.valueOf(getString("scope")),
        type = RoleType.valueOf(getString("type")),
        name = getString("name"),
        description = getString("description"),
        permissionKeys = getList("permissionKeys", String::class.java).toSet(),
        systemRole = getBoolean("systemRole", false),
        critical = getBoolean("critical", false),
        editable = getBoolean("editable", false),
        status = RoleStatus.valueOf(getString("status")),
        schemaVersion = getInteger("schemaVersion", 1),
    )
}
