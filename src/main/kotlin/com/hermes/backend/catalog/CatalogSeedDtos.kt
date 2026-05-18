package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogInitialSeedCommand
import com.hermes.application.catalog.CatalogInitialSeedResult
import com.hermes.application.catalog.CatalogInitialSeedVertical
import com.hermes.application.catalog.CatalogSeedItemResult
import kotlinx.serialization.Serializable

@Serializable
data class CatalogInitialSeedRequest(
    val verticals: Set<String> = emptySet(),
    val reason: String? = "Initial pilot catalog seed",
)

@Serializable
data class CatalogInitialSeedResponse(
    val verticals: Set<String>,
    val total: Int,
    val created: Int,
    val updated: Int,
    val unchanged: Int,
    val skipped: Int,
    val items: List<CatalogSeedItemResponse>,
)

@Serializable
data class CatalogSeedItemResponse(
    val entityType: String,
    val code: String,
    val id: String,
    val outcome: String,
    val message: String? = null,
)

fun CatalogInitialSeedRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogInitialSeedCommand = CatalogInitialSeedCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    verticals = verticals.map { raw ->
        runCatching { enumValueOf<CatalogInitialSeedVertical>(raw.trim().uppercase()) }
            .getOrElse { throw IllegalArgumentException("Invalid catalog seed vertical: $raw.") }
    }.toSet(),
    reason = reason?.trim()?.takeIf { it.isNotBlank() },
)

fun CatalogInitialSeedResult.toResponse(): CatalogInitialSeedResponse = CatalogInitialSeedResponse(
    verticals = verticals.map { it.name }.toSet(),
    total = total,
    created = created,
    updated = updated,
    unchanged = unchanged,
    skipped = skipped,
    items = items.map { it.toResponse() },
)

private fun CatalogSeedItemResult.toResponse(): CatalogSeedItemResponse = CatalogSeedItemResponse(
    entityType = entityType.name,
    code = code,
    id = id,
    outcome = outcome.name,
    message = message,
)
