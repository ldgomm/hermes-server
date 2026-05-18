package com.hermes.application.catalog

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

data class CatalogListAuditEventsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val actions: Set<CatalogAuditAction> = emptySet(),
    val targetId: String? = null,
    val auditedActorUserId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = CatalogAuditQuery.DEFAULT_LIMIT,
)

data class CatalogAuditEventsResult(
    val events: List<CatalogAuditRecord>,
)

class CatalogListAuditEventsUseCase(
    private val auditRepository: CatalogAuditQueryRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogListAuditEventsCommand): CatalogAuditEventsResult {
        assertCanViewCatalogAudit(command.actorEffectivePermissions)

        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")

        val query = CatalogAuditQuery(
            organizationId = organizationId,
            actions = command.actions,
            actorUserId = command.auditedActorUserId.normalizedNullable(),
            targetId = command.targetId.normalizedNullable(),
            from = command.from,
            to = command.to,
            limit = command.limit,
        )

        val events = auditRepository.search(query)

        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_AUDIT_VIEWED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = null,
                after = mapOf(
                    "actions" to query.actions.joinToString(",") { it.name },
                    "targetId" to query.targetId,
                    "auditedActorUserId" to query.actorUserId,
                    "from" to query.from?.toString(),
                    "to" to query.to?.toString(),
                    "limit" to query.limit.toString(),
                    "resultCount" to events.size.toString(),
                ),
                createdAt = Instant.now(clock),
            )
        )

        return CatalogAuditEventsResult(events)
    }

    private fun assertCanViewCatalogAudit(effectivePermissions: Set<String>) {
        val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.AUDIT_VIEW) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)

        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.AUDIT_VIEW}, ${PermissionCatalog.CATALOG_MANAGE_MASTER}."
            )
        }
    }
}

private fun String?.normalizedNullable(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
