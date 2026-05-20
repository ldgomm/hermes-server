package com.hermes.infrastructure.mongo.admin.access

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.shared.DomainRuleViolation
import org.bson.Document
import java.time.Instant
import java.util.Date

internal object MongoAdminAccessMappers {
    fun invitationToDocument(invitation: Invitation): Document = Document("_id", invitation.id)
        .append("organizationId", invitation.organizationId)
        .append("email", invitation.email)
        .append("invitedByUserId", invitation.invitedByUserId)
        .append("roleIds", invitation.roleIds.sorted())
        .append("tokenHash", invitation.tokenHash)
        .append("status", invitation.status.name)
        .append("createdAt", invitation.createdAt.toDate())
        .append("expiresAt", invitation.expiresAt.toDate())
        .append("acceptedAt", invitation.acceptedAt?.toDate())
        .append("revokedAt", invitation.revokedAt?.toDate())
        .append("acceptedUserId", invitation.acceptedUserId)
        .append("version", invitation.version)

    fun invitationFromDocument(document: Document): Invitation = Invitation(
        id = document.requiredString("_id"),
        organizationId = document.requiredString("organizationId"),
        email = document.requiredString("email").lowercase(),
        invitedByUserId = document.requiredString("invitedByUserId"),
        roleIds = document.stringList("roleIds").toSet(),
        tokenHash = document.requiredString("tokenHash"),
        status = document.getEnum("status", InvitationStatus.PENDING) { raw ->
            InvitationStatus.valueOf(raw.normalizedEnumToken())
        },
        createdAt = document.requiredInstant("createdAt"),
        expiresAt = document.requiredInstant("expiresAt"),
        acceptedAt = document.getInstantOrNull("acceptedAt"),
        revokedAt = document.getInstantOrNull("revokedAt"),
        acceptedUserId = document.getNullableString("acceptedUserId"),
        version = document.getLongFlexible("version") ?: 1L,
    )

    fun roleToDocument(role: RoleDefinition): Document = Document("_id", role.id)
        .append("code", role.code)
        .append("organizationId", role.organizationId)
        .append("scope", role.scope.name.lowercase())
        .append("type", role.type.name.lowercase())
        .append("name", role.name)
        .append("description", role.description)
        .append("permissionKeys", role.permissionKeys.sorted())
        .append("permissions", role.permissionKeys.sorted())
        .append("systemRole", role.systemRole)
        .append("critical", role.critical)
        .append("editable", role.editable)
        .append("status", role.status.name.lowercase())
        .append("schemaVersion", role.schemaVersion)

    fun roleFromDocument(document: Document): RoleDefinition {
        val scope = document.getEnum("scope", RoleScope.ORGANIZATION) { raw ->
            RoleScope.valueOf(raw.normalizedEnumToken())
        }
        val type = document.getEnum("type", if (scope == RoleScope.PLATFORM) RoleType.SYSTEM else RoleType.ORGANIZATION) { raw ->
            RoleType.valueOf(raw.normalizedEnumToken())
        }
        val permissionKeys = document.stringList("permissionKeys").ifEmpty {
            document.stringList("permissions")
        }.toSet()

        return RoleDefinition(
            id = document.requiredString("_id"),
            code = document.requiredString("code"),
            organizationId = document.getNullableString("organizationId"),
            scope = scope,
            type = type,
            name = document.getString("name") ?: document.requiredString("code"),
            description = document.getString("description") ?: "Role ${document.requiredString("code")}",
            permissionKeys = permissionKeys,
            systemRole = document.getBooleanFlexible("systemRole") ?: (type != RoleType.CUSTOM),
            critical = document.getBooleanFlexible("critical") ?: false,
            editable = document.getBooleanFlexible("editable") ?: false,
            status = document.getEnum("status", RoleStatus.ACTIVE) { raw ->
                RoleStatus.valueOf(raw.normalizedEnumToken())
            },
            schemaVersion = document.getIntegerFlexible("schemaVersion") ?: 1,
        )
    }

    private fun Instant.toDate(): Date = Date.from(this)

    private fun String.normalizedEnumToken(): String = trim()
        .replace('-', '_')
        .replace('.', '_')
        .uppercase()

    private fun Document.requiredString(key: String): String =
        getString(key)?.trim()?.takeIf { it.isNotBlank() }
            ?: throw DomainRuleViolation("Mongo document field $key is required.")

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
