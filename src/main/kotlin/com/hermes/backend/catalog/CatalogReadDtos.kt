package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogAuditEventsResult
import com.hermes.application.catalog.CatalogAuditRecord
import com.hermes.application.catalog.CatalogPriceHistoryResult
import com.hermes.domain.catalog.CatalogPriceHistory
import kotlinx.serialization.Serializable

@Serializable
data class CatalogAuditEventsResponse(
    val events: List<CatalogAuditRecordResponse>,
)

@Serializable
data class CatalogAuditRecordResponse(
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

@Serializable
data class CatalogPriceHistoryResponse(
    val history: List<CatalogPriceHistoryItemResponse>,
)

@Serializable
data class CatalogPriceHistoryItemResponse(
    val id: String,
    val organizationId: String,
    val catalogItemId: String,
    val oldPrice: MoneyResponse,
    val newPrice: MoneyResponse,
    val changedByUserId: String,
    val reason: String,
    val changedAt: String,
)

fun CatalogAuditEventsResult.toResponse(): CatalogAuditEventsResponse =
    CatalogAuditEventsResponse(events = events.map { it.toResponse() })

fun CatalogAuditRecord.toResponse(): CatalogAuditRecordResponse = CatalogAuditRecordResponse(
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

fun CatalogPriceHistoryResult.toResponse(): CatalogPriceHistoryResponse =
    CatalogPriceHistoryResponse(history = history.map { it.toResponse() })

fun CatalogPriceHistory.toResponse(): CatalogPriceHistoryItemResponse = CatalogPriceHistoryItemResponse(
    id = id,
    organizationId = organizationId,
    catalogItemId = catalogItemId,
    oldPrice = oldPrice.toResponse(),
    newPrice = newPrice.toResponse(),
    changedByUserId = changedByUserId,
    reason = reason,
    changedAt = changedAt.toString(),
)
