package com.hermes.application.admin.support

import java.time.LocalDate

/**
 * Human-readable closure snapshot for Fase 13F.
 *
 * This object is static and deterministic: it documents what the codebase
 * considers closed for Global Audit & Support API and gives tests a small
 * executable artifact to guard against accidental contract drift.
 */
object AdminSupportPhase13FClosure {
    val report: AdminSupportPhaseClosureReport = AdminSupportPhaseClosureReport(
        phase = "13F",
        title = "Global Audit & Support API",
        closedAt = LocalDate.parse("2026-05-20"),
        routeCount = AdminSupportApiContract.routes.size,
        readRouteCount = AdminSupportSecurityContract.readEndpoints.size,
        mutationRouteCount = AdminSupportSecurityContract.mutationEndpoints.size,
        requiredSurfaces = AdminSupportSurface.entries.toSet(),
        completedCapabilities = listOf(
            "Global audit logs search",
            "Organization audit timeline",
            "Operational support diagnostics",
            "Effective permission diagnostics",
            "Admin module diagnostics",
            "Executable endpoint contract",
            "Executable security matrix",
            "Secret redaction policy for support payloads",
            "Mongo read-side repository",
            "Ktor routes for Admin iOS/Web Admin",
        ),
        safetyRules = listOf(
            "Every route is organization-scoped.",
            "All endpoints are read-only.",
            "Audit logs remain append-only; this API never mutates them.",
            "Support payloads never expose passwords, tokens, private keys, certificate material or internal object keys.",
            "Timeline answers who did what, when and over which resource.",
            "Permission diagnostics explain missing access without granting access.",
        ),
        nextRecommendedPhase = "Fase 13 closure — run full clean test and update Admin iOS plan",
    )
}

data class AdminSupportPhaseClosureReport(
    val phase: String,
    val title: String,
    val closedAt: LocalDate,
    val routeCount: Int,
    val readRouteCount: Int,
    val mutationRouteCount: Int,
    val requiredSurfaces: Set<AdminSupportSurface>,
    val completedCapabilities: List<String>,
    val safetyRules: List<String>,
    val nextRecommendedPhase: String,
) {
    val totalRouteCountMatchesContract: Boolean
        get() = routeCount == readRouteCount + mutationRouteCount

    val coversAllSurfaces: Boolean
        get() = requiredSurfaces == AdminSupportSurface.entries.toSet()
}
