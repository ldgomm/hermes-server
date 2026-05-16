package com.hermes.infrastructure.mongo.auth

import com.hermes.domain.credential.CredentialStatus
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.organization.OrganizationStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import com.hermes.domain.user.UserStatus
import org.bson.Document
import java.time.Instant
import java.util.Date

object MongoAuthMappers {
    fun userToDocument(user: User): Document = Document("_id", user.id)
        .append("email", user.email)
        .append("phone", user.phone)
        .append("displayName", user.displayName)
        .append("status", user.status.toDb())
        .append("auth", Document())
        .append("profile", Document())
        .append("blockedAt", user.blockedAt?.toDate())
        .append("blockedReason", user.blockedReason)
        .append("archivedAt", user.archivedAt?.toDate())
        .append("createdAt", user.createdAt.toDate())
        .append("updatedAt", user.updatedAt.toDate())
        .append("version", user.version)
        .append("schemaVersion", 1)

    fun userFromDocument(document: Document): User = User(
        id = document.requiredString("_id"),
        email = document.requiredString("email").trim().lowercase(),
        displayName = document.requiredString("displayName"),
        phone = document.getNullableString("phone"),
        status = document.getEnum("status", UserStatus.ACTIVE) { raw ->
            when (raw.normalizedEnumToken()) {
                "ACTIVE" -> UserStatus.ACTIVE
                "INVITED" -> UserStatus.INVITED
                "SUSPENDED" -> UserStatus.SUSPENDED
                "BLOCKED" -> UserStatus.BLOCKED
                "DISABLED" -> UserStatus.DISABLED
                "ARCHIVED" -> UserStatus.ARCHIVED
                else -> UserStatus.valueOf(raw.normalizedEnumToken())
            }
        },
        createdAt = document.requiredInstant("createdAt"),
        updatedAt = document.requiredInstant("updatedAt"),
        blockedAt = document.getInstantOrNull("blockedAt"),
        blockedReason = document.getNullableString("blockedReason"),
        archivedAt = document.getInstantOrNull("archivedAt"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun credentialToDocument(credential: UserCredential): Document = Document("id", credential.id)
        .append("userId", credential.userId)
        .append("passwordHash", credential.passwordHash)
        .append("status", credential.status.toDb())
        .append("mustChangePassword", credential.mustChangePassword)
        .append("temporaryPassword", credential.temporaryPassword)
        .append("createdAt", credential.createdAt.toDate())
        .append("updatedAt", credential.updatedAt.toDate())
        .append("lastPasswordChangedAt", credential.lastPasswordChangedAt?.toDate())
        .append("revokedAt", credential.revokedAt?.toDate())
        .append("failedAttempts", credential.failedAttempts)
        .append("lockedUntil", credential.lockedUntil?.toDate())
        .append("version", credential.version)

    fun credentialFromDocument(document: Document, fallbackUserId: String? = null): UserCredential = UserCredential(
        id = document.getString("id") ?: document.getString("_id") ?: throw DomainRuleViolation("Credential id is missing."),
        userId = document.getString("userId") ?: fallbackUserId ?: throw DomainRuleViolation("Credential userId is missing."),
        passwordHash = document.requiredString("passwordHash"),
        status = document.getEnum("status", CredentialStatus.ACTIVE) { raw ->
            CredentialStatus.valueOf(raw.normalizedEnumToken())
        },
        mustChangePassword = document.getBooleanFlexible("mustChangePassword") ?: false,
        temporaryPassword = document.getBooleanFlexible("temporaryPassword") ?: false,
        createdAt = document.requiredInstant("createdAt"),
        updatedAt = document.requiredInstant("updatedAt"),
        lastPasswordChangedAt = document.getInstantOrNull("lastPasswordChangedAt"),
        revokedAt = document.getInstantOrNull("revokedAt"),
        failedAttempts = document.getIntegerFlexible("failedAttempts") ?: 0,
        lockedUntil = document.getInstantOrNull("lockedUntil"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun organizationToDocument(organization: Organization): Document = Document("_id", organization.id)
        .append("countryCode", organization.countryCode)
        .append("taxId", organization.taxId)
        .append("legalName", organization.legalName)
        .append("commercialName", organization.commercialName)
        .append("status", organization.status.toDb())
        .append("ownerUserId", organization.ownerUserId)
        .append("timezone", "America/Guayaquil")
        .append("defaultCurrency", "USD")
        .append("createdBy", organization.ownerUserId)
        .append("updatedBy", organization.ownerUserId)
        .append("createdAt", organization.createdAt.toDate())
        .append("updatedAt", organization.updatedAt.toDate())
        .append("version", organization.version)
        .append("schemaVersion", 1)

    fun organizationFromDocument(document: Document): Organization = Organization(
        id = document.requiredString("_id"),
        countryCode = document.getString("countryCode") ?: "EC",
        taxId = document.requiredString("taxId"),
        legalName = document.requiredString("legalName"),
        commercialName = document.getString("commercialName") ?: document.requiredString("legalName"),
        status = document.getEnum("status", OrganizationStatus.ACTIVE) { raw ->
            when (raw.normalizedEnumToken()) {
                "ONBOARDING" -> OrganizationStatus.DRAFT
                else -> OrganizationStatus.valueOf(raw.normalizedEnumToken())
            }
        },
        ownerUserId = document.getString("ownerUserId")
            ?: document.getString("createdBy")
            ?: throw DomainRuleViolation("Organization ownerUserId is missing."),
        createdAt = document.requiredInstant("createdAt"),
        updatedAt = document.requiredInstant("updatedAt"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun membershipToDocument(membership: OrganizationMembership): Document {
        val primaryRoleId = membership.roleIds.sorted().first()
        return Document("_id", membership.id)
            .append("organizationId", membership.organizationId)
            .append("userId", membership.userId)
            .append("roleId", primaryRoleId)
            .append("roleIds", membership.roleIds.sorted())
            .append("status", membership.status.toDb())
            .append("effectivePermissions", emptyList<String>())
            .append("invitedBy", membership.invitedBy)
            .append("acceptedAt", membership.acceptedAt?.toDate())
            .append("joinedAt", membership.acceptedAt?.toDate())
            .append("revokedAt", membership.revokedAt?.toDate())
            .append("createdAt", membership.createdAt.toDate())
            .append("updatedAt", membership.updatedAt.toDate())
            .append("version", membership.version)
            .append("schemaVersion", 1)
    }

    fun membershipFromDocument(document: Document): OrganizationMembership {
        val roleIds = document.stringList("roleIds").ifEmpty {
            listOfNotNull(document.getString("roleId"))
        }.toSet()

        return OrganizationMembership(
            id = document.requiredString("_id"),
            organizationId = document.requiredString("organizationId"),
            userId = document.requiredString("userId"),
            roleIds = roleIds,
            status = document.getEnum("status", MembershipStatus.ACTIVE) { raw ->
                when (raw.normalizedEnumToken()) {
                    "INVITED" -> MembershipStatus.PENDING_INVITATION
                    "PENDING" -> MembershipStatus.PENDING_INVITATION
                    "PENDING_INVITATION" -> MembershipStatus.PENDING_INVITATION
                    "REMOVED" -> MembershipStatus.ARCHIVED
                    else -> MembershipStatus.valueOf(raw.normalizedEnumToken())
                }
            },
            createdAt = document.requiredInstant("createdAt"),
            updatedAt = document.requiredInstant("updatedAt"),
            invitedBy = document.getNullableString("invitedBy"),
            acceptedAt = document.getInstantOrNull("acceptedAt") ?: document.getInstantOrNull("joinedAt"),
            revokedAt = document.getInstantOrNull("revokedAt"),
            version = document.getLongFlexible("version") ?: 1L,
        )
    }

    fun roleFromDocument(document: Document): RoleDefinition {
        val scope = document.getEnum("scope", RoleScope.ORGANIZATION) { raw ->
            RoleScope.valueOf(raw.normalizedEnumToken())
        }
        val type = document.getEnum("type", if (scope == RoleScope.PLATFORM) RoleType.SYSTEM else RoleType.ORGANIZATION) { raw ->
            RoleType.valueOf(raw.normalizedEnumToken())
        }
        val permissions = document.stringList("permissionKeys").ifEmpty {
            document.stringList("permissions")
        }.toSet().ifEmpty {
            setOf(PermissionCatalog.ALL).takeIf { document.getString("code") == "platform_super_admin" }.orEmpty()
        }

        return RoleDefinition(
            id = document.requiredString("_id"),
            code = document.requiredString("code"),
            organizationId = document.getNullableString("organizationId"),
            scope = scope,
            type = type,
            name = document.getString("name") ?: document.requiredString("code"),
            description = document.getString("description") ?: "System role ${document.requiredString("code")}",
            permissionKeys = permissions,
            systemRole = document.getBooleanFlexible("systemRole") ?: (type != RoleType.CUSTOM),
            critical = document.getBooleanFlexible("critical") ?: false,
            editable = document.getBooleanFlexible("editable") ?: false,
            status = document.getEnum("status", RoleStatus.ACTIVE) { raw -> RoleStatus.valueOf(raw.normalizedEnumToken()) },
            schemaVersion = document.getIntegerFlexible("schemaVersion") ?: 1,
        )
    }

    fun sessionToDocument(session: UserSession): Document = Document("_id", session.id)
        .append("userId", session.userId)
        .append("status", session.status.toDb())
        .append("createdAt", session.createdAt.toDate())
        .append("expiresAt", session.expiresAt.toDate())
        .append("lastSeenAt", session.lastSeenAt?.toDate())
        .append("revokedAt", session.revokedAt?.toDate())
        .append("revokedReason", session.revokedReason)
        .append("userAgent", session.userAgent)
        .append("ipAddress", session.ipAddress)
        .append("version", session.version)
        .append("schemaVersion", 1)

    fun sessionFromDocument(document: Document): UserSession = UserSession(
        id = document.requiredString("_id"),
        userId = document.requiredString("userId"),
        status = document.getEnum("status", UserSessionStatus.ACTIVE) { raw ->
            UserSessionStatus.valueOf(raw.normalizedEnumToken())
        },
        createdAt = document.requiredInstant("createdAt"),
        expiresAt = document.requiredInstant("expiresAt"),
        lastSeenAt = document.getInstantOrNull("lastSeenAt"),
        revokedAt = document.getInstantOrNull("revokedAt"),
        revokedReason = document.getNullableString("revokedReason"),
        userAgent = document.getNullableString("userAgent"),
        ipAddress = document.getNullableString("ipAddress"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun refreshTokenToDocument(refreshToken: RefreshToken): Document = Document("_id", refreshToken.id)
        .append("sessionId", refreshToken.sessionId)
        .append("userId", refreshToken.userId)
        .append("tokenHash", refreshToken.tokenHash)
        .append("createdAt", refreshToken.createdAt.toDate())
        .append("expiresAt", refreshToken.expiresAt.toDate())
        .append("usedAt", refreshToken.usedAt?.toDate())
        .append("revokedAt", refreshToken.revokedAt?.toDate())
        .append("replacedByTokenId", refreshToken.replacedByTokenId)
        .append("reuseDetectedAt", refreshToken.reuseDetectedAt?.toDate())
        .append("version", refreshToken.version)
        .append("schemaVersion", 1)

    fun refreshTokenFromDocument(document: Document): RefreshToken = RefreshToken(
        id = document.requiredString("_id"),
        sessionId = document.requiredString("sessionId"),
        userId = document.requiredString("userId"),
        tokenHash = document.requiredString("tokenHash"),
        createdAt = document.requiredInstant("createdAt"),
        expiresAt = document.requiredInstant("expiresAt"),
        usedAt = document.getInstantOrNull("usedAt"),
        revokedAt = document.getInstantOrNull("revokedAt"),
        replacedByTokenId = document.getNullableString("replacedByTokenId"),
        reuseDetectedAt = document.getInstantOrNull("reuseDetectedAt"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun auditEventToDocument(
        action: String,
        actorUserId: String?,
        targetUserId: String?,
        organizationId: String?,
        sessionId: String?,
        ipAddress: String?,
        userAgent: String?,
        message: String?,
        createdAt: Instant,
    ): Document = Document("_id", "cred_evt_${createdAt.toEpochMilli()}_${java.util.UUID.randomUUID().toString().replace("-", "")}")
        .append("action", action)
        .append("actorUserId", actorUserId)
        .append("targetUserId", targetUserId)
        .append("organizationId", organizationId)
        .append("sessionId", sessionId)
        .append("ipAddress", ipAddress)
        .append("userAgent", userAgent)
        .append("message", message)
        .append("createdAt", createdAt.toDate())
        .append("updatedAt", createdAt.toDate())
        .append("version", 1L)
        .append("schemaVersion", 1)

    private fun Instant.toDate(): Date = Date.from(this)

    private fun Enum<*>.toDb(): String = name.lowercase()

    private fun String.normalizedEnumToken(): String = trim()
        .replace('-', '_')
        .replace('.', '_')
        .uppercase()

    private fun Document.requiredString(key: String): String =
        getString(key)?.takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("Mongo document field $key is required.")

    private fun Document.getNullableString(key: String): String? =
        getString(key)?.trim()?.takeIf { it.isNotBlank() }

    private fun Document.requiredInstant(key: String): Instant =
        getInstantOrNull(key) ?: throw DomainRuleViolation("Mongo document date field $key is required.")

    private fun Document.getInstantOrNull(key: String): Instant? = when (val value = get(key)) {
        null -> null
        is Date -> value.toInstant()
        is Instant -> value
        is String -> runCatching { Instant.parse(value) }.getOrNull()
        else -> null
    }

    private fun Document.getLongFlexible(key: String): Long? = when (val value = get(key)) {
        is Long -> value
        is Int -> value.toLong()
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun Document.getIntegerFlexible(key: String): Int? = when (val value = get(key)) {
        is Int -> value
        is Long -> value.toInt()
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun Document.getBooleanFlexible(key: String): Boolean? = when (val value = get(key)) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull()
        else -> null
    }

    private fun <T> Document.getEnum(key: String, defaultValue: T, mapper: (String) -> T): T {
        val raw = getString(key) ?: return defaultValue
        return runCatching { mapper(raw) }.getOrElse {
            throw DomainRuleViolation("Invalid enum value for Mongo field $key: $raw.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Document.stringList(key: String): List<String> = when (val value = get(key)) {
        is List<*> -> value.filterIsInstance<String>()
        is String -> listOf(value)
        else -> emptyList()
    }
}
