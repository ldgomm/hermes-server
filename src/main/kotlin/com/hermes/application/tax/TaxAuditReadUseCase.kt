package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class TaxListAuditEventsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val actions: Set<TaxAuditAction> = emptySet(),
    val targetId: String? = null,
    val auditedActorUserId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = TaxAuditQuery.DEFAULT_LIMIT,
)

data class TaxAuditEventsResult(
    val events: List<TaxAuditRecord>,
)

class TaxListAuditEventsUseCase(
    private val auditRepository: TaxAuditQueryRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
) {
    fun execute(command: TaxListAuditEventsCommand): TaxAuditEventsResult {
        assertCanViewTaxAudit(command.actorEffectivePermissions)

        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")

        val query = TaxAuditQuery(
            organizationId = organizationId,
            actions = command.actions,
            actorUserId = command.auditedActorUserId?.trim()?.takeIf { it.isNotBlank() },
            targetId = command.targetId?.trim()?.takeIf { it.isNotBlank() },
            from = command.from,
            to = command.to,
            limit = command.limit,
        )

        val events = auditRepository.search(query)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_AUDIT_VIEWED,
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

        return TaxAuditEventsResult(events)
    }

    private fun assertCanViewTaxAudit(effectivePermissions: Set<String>) {
        val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.AUDIT_VIEW) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.AUDIT_VIEW}, ${PermissionCatalog.TAX_SETTINGS_VIEW}."
            )
        }
    }
}
