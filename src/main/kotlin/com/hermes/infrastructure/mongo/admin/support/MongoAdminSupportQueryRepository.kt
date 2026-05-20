package com.hermes.infrastructure.mongo.admin.support

import com.hermes.application.admin.support.AdminAuditLogRecord
import com.hermes.application.admin.support.AdminAuditTimelineItem
import com.hermes.application.admin.support.AdminSupportCounter
import com.hermes.application.admin.support.AdminSupportDiagnosticCheck
import com.hermes.application.admin.support.AdminSupportDiagnosticsReport
import com.hermes.application.admin.support.AdminSupportModuleStatus
import com.hermes.application.admin.support.AdminSupportModulesReport
import com.hermes.application.admin.support.AdminSupportPermissionsReport
import com.hermes.application.admin.support.AdminSupportQueryRepository
import com.hermes.application.admin.support.GetAdminAuditTimelineCommand
import com.hermes.application.admin.support.GetAdminSupportDiagnosticsCommand
import com.hermes.application.admin.support.GetAdminSupportModulesCommand
import com.hermes.application.admin.support.GetAdminSupportPermissionsCommand
import com.hermes.application.admin.support.SearchAdminAuditLogsCommand
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson
import java.time.Instant
import java.util.Date

class MongoAdminSupportQueryRepository(
    private val database: MongoDatabase,
) : AdminSupportQueryRepository {
    private val auditLogs: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)
    private val domainEvents: MongoCollection<Document> = database.getCollection(MongoCollectionNames.DOMAIN_EVENTS)
    private val credentialEvents: MongoCollection<Document> = database.getCollection(MongoCollectionNames.CREDENTIAL_EVENTS)
    private val organizations: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATIONS)
    private val users: MongoCollection<Document> = database.getCollection(MongoCollectionNames.USERS)
    private val memberships: MongoCollection<Document> = database.getCollection(MongoCollectionNames.MEMBERSHIPS)
    private val roles: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ROLES)

    override fun searchAuditLogs(command: SearchAdminAuditLogsCommand): List<AdminAuditLogRecord> {
        val filters = auditFilters(
            organizationId = command.organizationId,
            sources = command.sources,
            surfaces = command.surfaces,
            actions = command.actions,
            severities = command.severities,
            auditedActorUserId = command.auditedActorUserId,
            targetType = command.targetType,
            targetId = command.targetId,
            from = command.from,
            to = command.to,
        )

        val primaryLogs = auditLogs.find(Filters.and(filters))
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .limit(command.limit.coerceIn(1, 500))
            .into(mutableListOf())
            .map(::auditLogFromDocument)

        val credentialLogs = if (shouldIncludeCredentialEvents(command)) {
            credentialEvents.find(Filters.and(credentialFilters(command)))
                .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
                .limit(command.limit.coerceIn(1, 500))
                .into(mutableListOf())
                .map(::credentialEventFromDocument)
        } else {
            emptyList()
        }

        return (primaryLogs + credentialLogs)
            .sortedByDescending { it.createdAt }
            .take(command.limit.coerceIn(1, 500))
    }

    override fun auditTimeline(command: GetAdminAuditTimelineCommand): List<AdminAuditTimelineItem> =
        searchAuditLogs(
            SearchAdminAuditLogsCommand(
                organizationId = command.organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                auditedActorUserId = command.auditedActorUserId,
                targetType = command.targetType,
                targetId = command.targetId,
                from = command.from,
                to = command.to,
                limit = command.limit,
            ),
        ).map { it.toTimelineItem() }

    override fun diagnostics(command: GetAdminSupportDiagnosticsCommand): AdminSupportDiagnosticsReport {
        val organizationId = command.organizationId.trim()
        val existingCollections = database.listCollectionNames().into(mutableListOf()).toSet()
        val counters = listOf(
            counter("audit_logs", "Audit logs", countSafe(MongoCollectionNames.AUDIT_LOGS, organizationId)),
            counter("domain_events", "Domain events", countSafe(MongoCollectionNames.DOMAIN_EVENTS, organizationId)),
            counter("credential_events", "Credential events", countSafe(MongoCollectionNames.CREDENTIAL_EVENTS, organizationId)),
            counter("users", "Users", countSafe(MongoCollectionNames.USERS, organizationId)),
            counter("memberships", "Memberships", countSafe(MongoCollectionNames.MEMBERSHIPS, organizationId)),
            counter("roles", "Roles", countSafe(MongoCollectionNames.ROLES, organizationId)),
        )

        val checks = buildList {
            add(
                AdminSupportDiagnosticCheck(
                    code = "organization_exists",
                    status = if (organizationExists(organizationId)) "ok" else "error",
                    message = if (organizationExists(organizationId)) {
                        "Organization document exists."
                    } else {
                        "Organization document was not found."
                    },
                    actionHint = if (organizationExists(organizationId)) null else "Verify active organization resolver and seed data.",
                ),
            )
            add(
                AdminSupportDiagnosticCheck(
                    code = "audit_collection_available",
                    status = if (MongoCollectionNames.AUDIT_LOGS in existingCollections) "ok" else "warning",
                    message = if (MongoCollectionNames.AUDIT_LOGS in existingCollections) {
                        "audit_logs collection is available."
                    } else {
                        "audit_logs collection does not exist yet. It may be empty in fresh environments."
                    },
                    actionHint = "Run migrations/index setup if this is not a fresh environment.",
                ),
            )
            add(
                AdminSupportDiagnosticCheck(
                    code = "credential_audit_available",
                    status = if (MongoCollectionNames.CREDENTIAL_EVENTS in existingCollections) "ok" else "warning",
                    message = if (MongoCollectionNames.CREDENTIAL_EVENTS in existingCollections) {
                        "credential_events collection is available."
                    } else {
                        "credential_events collection does not exist yet."
                    },
                ),
            )
            add(
                AdminSupportDiagnosticCheck(
                    code = "secret_redaction_enabled",
                    status = "ok",
                    message = "Support API applies server-side redaction to secret-like keys.",
                    actionHint = "Keep token/password/certificate/private-key data out of audit payloads whenever possible.",
                ),
            )
            add(
                AdminSupportDiagnosticCheck(
                    code = "append_only_policy",
                    status = "ok",
                    message = "This API is read-only and does not modify audit/domain event collections.",
                ),
            )
        }

        val warnings = checks.filter { it.status != "ok" }.map { it.message }
        val status = when {
            checks.any { it.status == "error" } -> "error"
            checks.any { it.status == "warning" } -> "warning"
            else -> "ok"
        }

        return AdminSupportDiagnosticsReport(
            organizationId = organizationId,
            generatedAt = Instant.now(),
            status = status,
            checks = checks,
            counters = counters,
            warnings = warnings,
        )
    }

    override fun permissions(command: GetAdminSupportPermissionsCommand): AdminSupportPermissionsReport {
        val permissions = command.actorEffectivePermissions.sorted()
        val hasWildcard = PermissionCatalog.ALL in command.actorEffectivePermissions
        val risky = permissions.filter { it.isRiskyPermission() }
        val missingRecommended = recommendedAdminPermissions
            .filterNot { hasWildcard || it in command.actorEffectivePermissions }
            .sorted()

        return AdminSupportPermissionsReport(
            organizationId = command.organizationId.trim(),
            actorUserId = command.actorUserId.trim(),
            generatedAt = Instant.now(),
            hasWildcard = hasWildcard,
            permissionCount = permissions.size,
            permissions = permissions,
            riskyPermissions = risky,
            missingRecommendedAdminPermissions = missingRecommended,
            canViewAudit = hasWildcard || PermissionCatalog.AUDIT_VIEW in command.actorEffectivePermissions,
            canViewSupportDiagnostics = hasWildcard || command.actorEffectivePermissions.any {
                it in setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW)
            },
        )
    }

    override fun modules(command: GetAdminSupportModulesCommand): AdminSupportModulesReport {
        val actorPermissions = command.actorEffectivePermissions
        val hasWildcard = PermissionCatalog.ALL in actorPermissions
        val modules = adminModules.map { definition ->
            val missing = if (hasWildcard) emptyList() else definition.requiredPermissions.filterNot { it in actorPermissions }
            AdminSupportModuleStatus(
                code = definition.code,
                name = definition.name,
                phase = definition.phase,
                status = if (missing.isEmpty()) "available" else "restricted",
                enabled = missing.isEmpty(),
                requiredPermissions = definition.requiredPermissions.sorted(),
                missingPermissions = missing.sorted(),
                dependencies = definition.dependencies,
                notes = definition.notes,
            )
        }
        return AdminSupportModulesReport(
            organizationId = command.organizationId.trim(),
            actorUserId = command.actorUserId.trim(),
            generatedAt = Instant.now(),
            modules = modules,
        )
    }

    private fun auditFilters(
        organizationId: String,
        sources: Set<String>,
        surfaces: Set<String>,
        actions: Set<String>,
        severities: Set<String>,
        auditedActorUserId: String?,
        targetType: String?,
        targetId: String?,
        from: Instant?,
        to: Instant?,
    ): MutableList<Bson> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()))
        if (sources.isNotEmpty()) filters += Filters.`in`("source", sources.normalizedStorageValues())
        if (surfaces.isNotEmpty()) filters += Filters.`in`("surface", surfaces.normalizedStorageValues())
        if (actions.isNotEmpty()) filters += Filters.`in`("action", actions.normalizedActionValues())
        if (severities.isNotEmpty()) filters += Filters.`in`("severity", severities.normalizedStorageValues())
        auditedActorUserId?.takeIfNotBlank()?.let { filters += Filters.eq("actorUserId", it) }
        targetType?.takeIfNotBlank()?.let { filters += Filters.eq("targetType", it) }
        targetId?.takeIfNotBlank()?.let { filters += Filters.eq("targetId", it) }
        from?.let { filters += Filters.gte(MongoDocumentFields.CREATED_AT, it.toDate()) }
        to?.let { filters += Filters.lt(MongoDocumentFields.CREATED_AT, it.toDate()) }
        return filters
    }

    private fun credentialFilters(command: SearchAdminAuditLogsCommand): MutableList<Bson> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, command.organizationId.trim()))
        if (command.actions.isNotEmpty()) filters += Filters.`in`("action", command.actions.normalizedActionValues())
        command.auditedActorUserId?.takeIfNotBlank()?.let { filters += Filters.eq("actorUserId", it) }
        command.targetId?.takeIfNotBlank()?.let { filters += Filters.eq("targetUserId", it) }
        command.from?.let { filters += Filters.gte(MongoDocumentFields.CREATED_AT, it.toDate()) }
        command.to?.let { filters += Filters.lt(MongoDocumentFields.CREATED_AT, it.toDate()) }
        return filters
    }

    private fun shouldIncludeCredentialEvents(command: SearchAdminAuditLogsCommand): Boolean {
        if (command.sources.isEmpty() && command.surfaces.isEmpty()) return true
        return command.sources.any { it.equals("auth", ignoreCase = true) || it.equals("credentials", ignoreCase = true) } ||
            command.surfaces.any { it.equals("credentials", ignoreCase = true) || it.equals("users", ignoreCase = true) }
    }

    private fun auditLogFromDocument(document: Document): AdminAuditLogRecord = AdminAuditLogRecord(
        id = document.idValue(),
        organizationId = document.stringOrNull(MongoDocumentFields.ORGANIZATION_ID) ?: "",
        source = document.stringOrNull("source") ?: document.stringOrNull("module") ?: "audit",
        surface = document.stringOrNull("surface") ?: document.stringOrNull("targetType") ?: "global",
        action = document.stringOrNull("action") ?: "UNKNOWN",
        actorUserId = document.stringOrNull("actorUserId"),
        targetType = document.stringOrNull("targetType"),
        targetId = document.stringOrNull("targetId"),
        reason = document.stringOrNull("reason"),
        message = document.stringOrNull("message"),
        severity = document.stringOrNull("severity") ?: "info",
        correlationId = document.stringOrNull("correlationId"),
        before = document.mapField("before").redacted(),
        after = document.mapField("after").redacted(),
        metadata = document.mapField("metadata").redacted(),
        createdAt = document.instantOrNow(MongoDocumentFields.CREATED_AT),
    )

    private fun credentialEventFromDocument(document: Document): AdminAuditLogRecord = AdminAuditLogRecord(
        id = document.idValue(),
        organizationId = document.stringOrNull(MongoDocumentFields.ORGANIZATION_ID) ?: "",
        source = "auth",
        surface = "credentials",
        action = document.stringOrNull("action") ?: "UNKNOWN",
        actorUserId = document.stringOrNull("actorUserId"),
        targetType = "user",
        targetId = document.stringOrNull("targetUserId"),
        reason = document.stringOrNull("message"),
        message = document.stringOrNull("message"),
        severity = "info",
        correlationId = document.stringOrNull("sessionId"),
        before = emptyMap(),
        after = emptyMap(),
        metadata = mapOf(
            "sessionId" to document.stringOrNull("sessionId"),
            "ipAddress" to document.stringOrNull("ipAddress"),
            "userAgent" to document.stringOrNull("userAgent"),
        ).redacted(),
        createdAt = document.instantOrNow(MongoDocumentFields.CREATED_AT),
    )

    private fun AdminAuditLogRecord.toTimelineItem(): AdminAuditTimelineItem = AdminAuditTimelineItem(
        id = id,
        organizationId = organizationId,
        occurredAt = createdAt,
        source = source,
        surface = surface,
        title = action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
        description = listOfNotNull(
            actorUserId?.let { "Actor $it" },
            targetType?.let { type -> targetId?.let { id -> "$type $id" } ?: type },
            reason?.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
            message?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifBlank { action },
        actorUserId = actorUserId,
        targetType = targetType,
        targetId = targetId,
        severity = severity,
        reason = reason,
        metadata = metadata,
    )

    private fun organizationExists(organizationId: String): Boolean = organizations.find(
        Filters.eq(MongoDocumentFields.ID, organizationId),
    ).firstOrNull() != null

    private fun countSafe(collectionName: String, organizationId: String): Long = runCatching {
        database.getCollection(collectionName).countDocuments(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()))
    }.getOrDefault(0L)

    private fun counter(code: String, label: String, value: Long): AdminSupportCounter = AdminSupportCounter(
        code = code,
        label = label,
        value = value,
    )
}

private data class AdminModuleDefinition(
    val code: String,
    val name: String,
    val phase: String,
    val requiredPermissions: List<String>,
    val dependencies: List<String>,
    val notes: List<String> = emptyList(),
)

private val recommendedAdminPermissions = setOf(
    PermissionCatalog.ORGANIZATION_VIEW,
    PermissionCatalog.AUDIT_VIEW,
    PermissionCatalog.CREDENTIALS_USERS_VIEW,
    PermissionCatalog.CREDENTIALS_ROLES_VIEW,
    PermissionCatalog.CATALOG_LOCAL_VIEW,
    PermissionCatalog.TAX_SETTINGS_VIEW,
    PermissionCatalog.SALES_VIEW,
    PermissionCatalog.CASH_VIEW,
    PermissionCatalog.PAYMENTS_VIEW,
    PermissionCatalog.RECEIVABLES_VIEW,
    PermissionCatalog.REPORTS_DASHBOARD_VIEW,
)

private val adminModules = listOf(
    AdminModuleDefinition(
        code = "business_foundation",
        name = "Business Foundation Admin API",
        phase = "13A",
        requiredPermissions = listOf(PermissionCatalog.ORGANIZATION_VIEW, PermissionCatalog.ADMIN_BUSINESS_VIEW),
        dependencies = listOf("Auth", "Organization", "Activities", "Branches", "Emission Points"),
    ),
    AdminModuleDefinition(
        code = "users_roles_permissions",
        name = "Users, Roles & Permissions Admin API",
        phase = "13B",
        requiredPermissions = listOf(PermissionCatalog.CREDENTIALS_USERS_VIEW, PermissionCatalog.CREDENTIALS_ROLES_VIEW),
        dependencies = listOf("Auth", "Memberships", "Roles", "PermissionCatalog"),
    ),
    AdminModuleDefinition(
        code = "catalog_admin",
        name = "Catalog Admin API",
        phase = "13C",
        requiredPermissions = listOf(PermissionCatalog.CATALOG_VIEW, PermissionCatalog.CATALOG_LOCAL_VIEW),
        dependencies = listOf("Platform catalog", "Organization catalog", "Catalog requests"),
    ),
    AdminModuleDefinition(
        code = "tax_admin",
        name = "Tax Admin API",
        phase = "13D",
        requiredPermissions = listOf(PermissionCatalog.TAX_SETTINGS_VIEW),
        dependencies = listOf("Tax Engine", "Tax profiles", "Tax rates", "Catalog local items"),
        notes = listOf("Tax/legal rules must be verified against official current sources before production changes."),
    ),
    AdminModuleDefinition(
        code = "operations_reports",
        name = "Sales, Cash & Reports Admin API",
        phase = "13E",
        requiredPermissions = listOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.CASH_VIEW, PermissionCatalog.REPORTS_DASHBOARD_VIEW),
        dependencies = listOf("Sales", "Payments", "Cash Sessions", "Commercial Documents"),
    ),
    AdminModuleDefinition(
        code = "audit_support",
        name = "Global Audit & Support API",
        phase = "13F",
        requiredPermissions = listOf(PermissionCatalog.AUDIT_VIEW),
        dependencies = listOf("Audit logs", "Credential events", "Domain events"),
        notes = listOf("Read-only support surface; no secrets exposed."),
    ),
)

private fun String?.takeIfNotBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }

private fun Set<String>.normalizedStorageValues(): List<String> = flatMap { raw ->
    val value = raw.trim()
    listOf(value, value.lowercase(), value.uppercase())
}.filter { it.isNotBlank() }.distinct()

private fun Set<String>.normalizedActionValues(): List<String> = flatMap { raw ->
    val value = raw.trim()
    listOf(value, value.lowercase(), value.uppercase())
}.filter { it.isNotBlank() }.distinct()

private fun Instant.toDate(): Date = Date.from(this)

private fun Document.idValue(): String = stringOrNull(MongoDocumentFields.ID) ?: getObjectId(MongoDocumentFields.ID)?.toHexString() ?: ""

private fun Document.stringOrNull(name: String): String? = when (val value = get(name)) {
    null -> null
    is String -> value.takeIf { it.isNotBlank() }
    else -> value.toString().takeIf { it.isNotBlank() }
}

private fun Document.instantOrNow(name: String): Instant = when (val value = get(name)) {
    is Date -> value.toInstant()
    is Instant -> value
    is String -> runCatching { Instant.parse(value) }.getOrDefault(Instant.EPOCH)
    else -> Instant.EPOCH
}

private fun Document.mapField(name: String): Map<String, String?> = when (val value = get(name)) {
    is Document -> value.entries.associate { (key, raw) -> key to raw?.toString() }
    is Map<*, *> -> value.entries.associate { (key, raw) -> key.toString() to raw?.toString() }
    else -> emptyMap()
}

private fun Map<String, String?>.redacted(): Map<String, String?> = mapValues { (key, value) ->
    if (key.isSensitiveSupportKey()) "***" else value
}

private fun String.isSensitiveSupportKey(): Boolean {
    val value = lowercase()
    return listOf("password", "token", "hash", "secret", "privatekey", "private_key", "certificate", "certificado", "objectkey", "object_key", "storagekey", "storage_key").any { it in value }
}

private fun String.isRiskyPermission(): Boolean {
    val value = lowercase()
    return value.contains("manage") ||
        value.contains("update") ||
        value.contains("reset") ||
        value.contains("block") ||
        value.contains("revoke") ||
        value.contains("issue") ||
        value.contains("signature") ||
        value.contains("tax") ||
        this == PermissionCatalog.ALL
}
