package com.hermes.application.admin.tax

import java.time.LocalDate

object AdminTaxPhase13DClosure {
    val report: AdminTaxPhaseClosureReport = AdminTaxPhaseClosureReport(
        phase = "13D",
        title = "Tax Admin API",
        closedAt = LocalDate.parse("2026-05-20"),
        routeCount = AdminTaxApiContract.routes.size,
        readRouteCount = AdminTaxSecurityContract.readEndpoints.size,
        mutationRouteCount = AdminTaxSecurityContract.mutationEndpoints.size,
        requiredSurfaces = AdminTaxSurface.entries.toSet(),
        completedCapabilities = listOf(
            "Tax profiles search/detail/create/update",
            "Tax rates search/detail/create/update",
            "Catalog local item tax profile assignment",
            "Tax readiness for Admin iOS/Web Admin",
            "Executable route contract",
            "Executable security matrix",
            "Mongo query adapters for admin search",
            "Ktor route layer with DTOs",
            "Docs and wiring notes",
        ),
        safetyRules = listOf(
            "No tax rate is hardcoded by Admin Tax routes.",
            "Every tax mutation requires a business reason.",
            "Every tax mutation is delegated to audited application use cases.",
            "Local catalog tax assignment validates organization settings and enabled profile codes.",
            "Historic sales/documents keep their captured tax snapshots and are not recalculated retroactively.",
            "Every admin route is scoped to the active organization context.",
        ),
        nextRecommendedPhase = "13E — Sales, Cash & Reports Admin API",
    )
}

data class AdminTaxPhaseClosureReport(
    val phase: String,
    val title: String,
    val closedAt: LocalDate,
    val routeCount: Int,
    val readRouteCount: Int,
    val mutationRouteCount: Int,
    val requiredSurfaces: Set<AdminTaxSurface>,
    val completedCapabilities: List<String>,
    val safetyRules: List<String>,
    val nextRecommendedPhase: String,
) {
    val totalRouteCountMatchesContract: Boolean
        get() = routeCount == readRouteCount + mutationRouteCount

    val coversAllSurfaces: Boolean
        get() = requiredSurfaces == AdminTaxSurface.entries.toSet()
}
