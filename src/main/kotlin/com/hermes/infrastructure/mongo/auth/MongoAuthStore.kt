package com.hermes.infrastructure.mongo.auth

import com.hermes.application.auth.*
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import org.bson.Document
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

class MongoAuthStore(
    private val database: MongoDatabase,
    private val client: MongoClient? = null,
) : UserRepository,
    UserCredentialRepository,
    OrganizationRepository,
    OrganizationMembershipRepository,
    AuthRoleLookupRepository,
    UserSessionRepository,
    RefreshTokenRepository,
    OwnerWorkspaceRepository,
    AuthContextRepository {

    private val users: MongoCollection<Document> = database.getCollection(MongoCollectionNames.USERS)
    private val organizations: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATIONS)
    private val memberships: MongoCollection<Document> = database.getCollection(MongoCollectionNames.MEMBERSHIPS)
    private val roles: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ROLES)
    private val sessions: MongoCollection<Document> = database.getCollection(MongoCollectionNames.USER_SESSIONS)
    private val refreshTokens: MongoCollection<Document> =
        database.getCollection(AuthMongoCollectionNames.REFRESH_TOKENS)

    override fun existsUserByEmail(email: String): Boolean =
        users.countDocuments(eq("email", email.trim().lowercase())) > 0

    override fun emailExists(email: String): Boolean = existsUserByEmail(email)

    override fun findUserByEmail(email: String): User? = users
        .find(regex("email", "^${Pattern.quote(email.trim().lowercase())}$", "i"))
        .firstOrNull()
        ?.let(MongoAuthMappers::userFromDocument)

    override fun findUserById(userId: String): User? = users
        .find(eq("_id", userId.trim()))
        .firstOrNull()
        ?.let(MongoAuthMappers::userFromDocument)

    override fun create(user: User) {
        users.insertOne(MongoAuthMappers.userToDocument(user))
    }

    override fun update(user: User) {
        users.replaceOne(
            eq("_id", user.id),
            MongoAuthMappers.userToDocument(user),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findByUserId(userId: String): UserCredential? {
        val document = users.find(eq("_id", userId.trim())).firstOrNull() ?: return null
        val auth = document.get("auth", Document::class.java) ?: return null
        val credential = auth.get("credential", Document::class.java) ?: return null
        return MongoAuthMappers.credentialFromDocument(credential, fallbackUserId = userId)
    }

    override fun create(credential: UserCredential) {
        users.updateOne(
            eq("_id", credential.userId),
            combine(
                set("auth.credential", MongoAuthMappers.credentialToDocument(credential)),
                set("updatedAt", Date.from(credential.updatedAt)),
            ),
        )
    }

    override fun update(credential: UserCredential) {
        create(credential)
    }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.countDocuments(
            and(
                eq("countryCode", countryCode.trim().uppercase()),
                eq("taxId", taxId.trim()),
            ),
        ) > 0

    override fun organizationTaxIdExists(countryCode: String, taxId: String): Boolean =
        existsByTaxId(countryCode, taxId)

    override fun findOrganizationById(organizationId: String): Organization? = organizations
        .find(eq("_id", organizationId.trim()))
        .firstOrNull()
        ?.let(MongoAuthMappers::organizationFromDocument)

    override fun create(organization: Organization) {
        organizations.insertOne(MongoAuthMappers.organizationToDocument(organization))
    }

    override fun existsByOrganizationIdAndUserId(organizationId: String, userId: String): Boolean =
        memberships.countDocuments(
            and(
                eq("organizationId", organizationId.trim()),
                eq("userId", userId.trim()),
            ),
        ) > 0

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        memberships.find(
            and(
                eq("organizationId", organizationId.trim()),
                eq("userId", userId.trim()),
            ),
        ).firstOrNull()?.let(MongoAuthMappers::membershipFromDocument)

    override fun create(membership: OrganizationMembership) {
        memberships.insertOne(MongoAuthMappers.membershipToDocument(membership))
    }

    override fun findSystemRoleByCode(code: String): RoleDefinition? = roles
        .find(eq("code", code.trim()))
        .firstOrNull()
        ?.let(MongoAuthMappers::roleFromDocument)

    override fun create(session: UserSession) {
        sessions.insertOne(MongoAuthMappers.sessionToDocument(session))
    }

    override fun findSessionById(sessionId: String): UserSession? = sessions
        .find(eq("_id", sessionId.trim()))
        .firstOrNull()
        ?.let(MongoAuthMappers::sessionFromDocument)

    override fun findActiveByUserId(userId: String): List<UserSession> = sessions
        .find(and(eq("userId", userId.trim()), eq("status", "active")))
        .into(mutableListOf())
        .map(MongoAuthMappers::sessionFromDocument)

    override fun update(session: UserSession) {
        sessions.replaceOne(
            eq("_id", session.id),
            MongoAuthMappers.sessionToDocument(session),
            ReplaceOptions().upsert(false),
        )
    }

    override fun create(refreshToken: RefreshToken) {
        refreshTokens.insertOne(MongoAuthMappers.refreshTokenToDocument(refreshToken))
    }

    override fun findRefreshTokenByHash(tokenHash: String): RefreshToken? = refreshTokens
        .find(eq("tokenHash", tokenHash.trim()))
        .firstOrNull()
        ?.let(MongoAuthMappers::refreshTokenFromDocument)

    override fun findActiveBySessionId(sessionId: String): List<RefreshToken> = refreshTokens
        .find(
            and(
                eq("sessionId", sessionId.trim()),
                exists("usedAt", false),
                exists("revokedAt", false),
            ),
        )
        .into(mutableListOf())
        .map(MongoAuthMappers::refreshTokenFromDocument)

    override fun update(refreshToken: RefreshToken) {
        refreshTokens.replaceOne(
            eq("_id", refreshToken.id),
            MongoAuthMappers.refreshTokenToDocument(refreshToken),
            ReplaceOptions().upsert(false),
        )
    }

    override fun revokeActiveBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        if (sessionIds.isEmpty()) return 0
        val result = refreshTokens.updateMany(
            and(
                `in`("sessionId", sessionIds),
                exists("revokedAt", false),
            ),
            set("revokedAt", Date.from(revokedAt)),
        )
        return result.modifiedCount.toInt()
    }

    override fun rotate(oldToken: RefreshToken, newToken: RefreshToken) {
        update(oldToken)
        create(newToken)
    }

    override fun findMembershipsByUserId(userId: String): List<OrganizationMembership> = memberships
        .find(eq("userId", userId.trim()))
        .into(mutableListOf())
        .map(MongoAuthMappers::membershipFromDocument)

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> {
        if (roleIds.isEmpty()) return emptyList()
        return roles
            .find(`in`("_id", roleIds))
            .into(mutableListOf())
            .map(MongoAuthMappers::roleFromDocument)
    }

    override fun createOwnerWorkspace(
        user: User,
        credential: UserCredential,
        organization: Organization,
        membership: OrganizationMembership,
    ) {
        val userDocument = MongoAuthMappers.userToDocument(user)
        userDocument["auth"] = Document("credential", MongoAuthMappers.credentialToDocument(credential))
        val organizationDocument = MongoAuthMappers.organizationToDocument(organization)
        val membershipDocument = MongoAuthMappers.membershipToDocument(membership)

        val mongoClient = client
        if (mongoClient == null) {
            users.insertOne(userDocument)
            organizations.insertOne(organizationDocument)
            memberships.insertOne(membershipDocument)
            return
        }

        mongoClient.startSession().use { session ->
            session.startTransaction()
            try {
                users.insertOne(session, userDocument)
                organizations.insertOne(session, organizationDocument)
                memberships.insertOne(session, membershipDocument)
                session.commitTransaction()
            } catch (error: Throwable) {
                session.abortTransaction()
                throw error
            }
        }
    }
}

private fun <T> com.mongodb.client.FindIterable<T>.firstOrNull(): T? = first()
