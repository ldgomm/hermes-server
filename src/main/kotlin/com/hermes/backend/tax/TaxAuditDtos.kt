package com.hermes.backend.tax

import com.hermes.application.tax.TaxAuditAction
import com.hermes.application.tax.TaxAuditRecord
import com.hermes.application.tax.TaxAuditEventsResult
import com.hermes.application.tax.TaxListAuditEventsCommand
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TaxAuditEventsResponse(
    val events: List<TaxAuditEventResponse>,
)

@Serializable
data class TaxAuditEventResponse(
    val id: String,
    val action: String,
    val actorUserId: String?,
    val organizationId: String,
    val targetId: String?,
    val before: Map<String, String?>,
    val after: Map<String, String?>,
    val reason: String?,
    val createdAt: String,
)

fun TaxAuditEventsResult.toResponse(): TaxAuditEventsResponse =
    TaxAuditEventsResponse(events = events.map { it.toResponse() })

private fun TaxAuditRecord.toResponse(): TaxAuditEventResponse =
    TaxAuditEventResponse(
        id = id,
        action = action.name,
        actorUserId = actorUserId,
        organizationId = organizationId,
        targetId = targetId,
        before = before,
        after = after,
        reason = reason,
        createdAt = createdAt.toString(),
    )

fun taxAuditCommandFromQuery(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    actions: String?,
    targetId: String?,
    auditedActorUserId: String?,
    from: String?,
    to: String?,
    limit: String?,
): TaxListAuditEventsCommand =
    TaxListAuditEventsCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        actions = actions
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.map { TaxAuditAction.valueOf(it.uppercase()) }
            ?.toSet()
            .orEmpty(),
        targetId = targetId,
        auditedActorUserId = auditedActorUserId,
        from = from?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse),
        to = to?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse),
        limit = limit?.toIntOrNull() ?: 100,
    )
