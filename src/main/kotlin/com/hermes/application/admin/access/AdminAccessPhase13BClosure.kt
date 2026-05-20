package com.hermes.application.admin.access

import java.time.LocalDate

/**
 * Human-readable closure snapshot for Fase 13B.
 *
 * This object is intentionally static and deterministic: it documents what the
 * codebase considers "closed" for Users/Roles/Invitations Admin API and gives
 * tests a small executable artifact to guard against accidental contract drift.
 */
object AdminAccessPhase13BClosure {
    val report: AdminAccessPhaseClosureReport = AdminAccessPhaseClosureReport(
        phase = "13B",
        title = "Users, Roles & Invitations Admin API",
        closedAt = LocalDate.parse("2026-05-20"),
        routeCount = AdminAccessApiContract.routes.size,
        readRouteCount = AdminAccessSecurityContract.readEndpoints.size,
        mutationRouteCount = AdminAccessSecurityContract.mutationEndpoints.size,
        requiredSurfaces = AdminAccessSurface.entries.toSet(),
        completedCapabilities = listOf(
            "Users list/detail/update",
            "User block/unblock",
            "User session revocation",
            "Temporary users",
            "Admin password reset",
            "Invitations create/list/detail/resend/revoke integrated with existing flow",
            "Roles list/detail/create/update/activate/deactivate",
            "Permissions catalog response",
            "Executable endpoint contract",
            "Executable security matrix",
        ),
        safetyRules = listOf(
            "Every admin access route is scoped to the active organization.",
            "Every mutation route requires a reason.",
            "Every mutation route is auditable.",
            "Custom role updates reject unknown permissions.",
            "Custom roles cannot receive wildcard platform permission.",
            "System, critical or non-editable roles cannot be mutated from organization admin.",
            "A user cannot block or administratively reset themselves.",
            "The organization cannot be left without an active administrator.",
            "The organization cannot be left without an active role manager.",
            "Invitation creation delegates to the existing auth invitation use case.",
        ),
        nextRecommendedPhase = "13C — Catalog Admin API",
    )
}

data class AdminAccessPhaseClosureReport(
    val phase: String,
    val title: String,
    val closedAt: LocalDate,
    val routeCount: Int,
    val readRouteCount: Int,
    val mutationRouteCount: Int,
    val requiredSurfaces: Set<AdminAccessSurface>,
    val completedCapabilities: List<String>,
    val safetyRules: List<String>,
    val nextRecommendedPhase: String,
) {
    val totalRouteCountMatchesContract: Boolean
        get() = routeCount == readRouteCount + mutationRouteCount

    val coversAllSurfaces: Boolean
        get() = requiredSurfaces == AdminAccessSurface.entries.toSet()
}
