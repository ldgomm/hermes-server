package com.hermes.application.catalog

/**
 * Fase 7.4 — Operational local catalog commands.
 *
 * These commands are intentionally small because sales/reservations will depend on
 * this lookup layer. They must remain stable and predictable.
 */
data class CatalogGetOrganizationItemCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val catalogItemId: String,
)

data class CatalogLookupOrganizationItemByCodeCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val code: String,
    val includeInactive: Boolean = false,
)

data class CatalogRemoveLocalItemCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val catalogItemId: String,
    val reason: String,
)
