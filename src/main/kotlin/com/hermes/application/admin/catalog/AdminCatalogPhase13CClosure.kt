package com.hermes.application.admin.catalog

import java.time.LocalDate

/**
 * Executable closure snapshot for Fase 13C — Catalog Admin API.
 *
 * This object is intentionally deterministic so it can be tested by the backend
 * and used by Admin iOS/Web as a compact implementation checklist.
 */
object AdminCatalogPhase13CClosure {
    val report: AdminCatalogPhaseClosureReport = AdminCatalogPhaseClosureReport(
        phase = "13C",
        title = "Catalog Admin API",
        closedAt = LocalDate.parse("2026-05-20"),
        routeCount = AdminCatalogApiContract.routes.size,
        readRouteCount = AdminCatalogSecurityContract.readEndpoints.size,
        mutationRouteCount = AdminCatalogSecurityContract.mutationEndpoints.size,
        platformOnlyRouteCount = AdminCatalogSecurityContract.platformOnlyEndpoints.size,
        requiredSurfaces = AdminCatalogSurface.entries.toSet(),
        completedCapabilities = listOf(
            "Master templates read/search/detail",
            "Master categories and families list/create foundation",
            "Local catalog list/detail/update",
            "Copy template to local catalog",
            "Activate/deactivate/remove local catalog items without destructive delete",
            "Catalog requests create/list/detail/review",
            "Mongo wiring through catalog stores and tax stores",
            "Admin route layer under /api/v1/admin/catalog",
            "Route contract tests and smoke integration tests",
            "Closure docs for Admin iOS/Web consumption",
        ),
        safetyRules = listOf(
            "All local catalog reads and mutations are scoped to the active organization.",
            "Master catalog mutations require catalog.manage_master.",
            "Local copy requires a valid activity, local price and enabled tax profile.",
            "Local updates reuse field-level permissions from core catalog use cases.",
            "Remove marks an item as REMOVED_FROM_ACCOUNT; it does not hard-delete catalog history.",
            "Catalog requests created by businesses stay organization-scoped.",
            "Review actions are platform/master-catalog operations and require explicit reason/message where applicable.",
            "DTOs never expose persistence documents directly.",
        ),
        nextRecommendedPhase = "13D — Tax Admin API / asignaciones tributarias avanzadas",
    )
}

data class AdminCatalogPhaseClosureReport(
    val phase: String,
    val title: String,
    val closedAt: LocalDate,
    val routeCount: Int,
    val readRouteCount: Int,
    val mutationRouteCount: Int,
    val platformOnlyRouteCount: Int,
    val requiredSurfaces: Set<AdminCatalogSurface>,
    val completedCapabilities: List<String>,
    val safetyRules: List<String>,
    val nextRecommendedPhase: String,
) {
    val totalRouteCountMatchesContract: Boolean
        get() = routeCount == readRouteCount + mutationRouteCount

    val coversAllSurfaces: Boolean
        get() = requiredSurfaces == AdminCatalogSurface.entries.toSet()
}
