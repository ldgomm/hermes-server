package com.hermes.application.admin.operations

import java.time.LocalDate

/**
 * Deterministic closure snapshot for Fase 13E.
 *
 * It documents what the codebase considers closed for Sales, Cash & Reports Admin API
 * and lets tests detect accidental route/security drift.
 */
object AdminOperationsPhase13EClosure {
    val report: AdminOperationsPhaseClosureReport = AdminOperationsPhaseClosureReport(
        phase = "13E",
        title = "Sales, Cash & Reports Admin API",
        closedAt = LocalDate.parse("2026-05-20"),
        routeCount = AdminOperationsApiContract.routes.size,
        readRouteCount = AdminOperationsSecurityContract.readEndpoints.size,
        mutationRouteCount = AdminOperationsSecurityContract.mutationEndpoints.size,
        requiredSurfaces = AdminOperationsSurface.entries.toSet(),
        completedCapabilities = listOf(
            "Sales admin search and detail",
            "Cash sessions list/current/detail",
            "Payments admin search",
            "Receivables admin search",
            "Operational today dashboard",
            "Sales summary report",
            "Cash summary report",
            "Tax summary report from immutable snapshots",
            "Executable REST contract",
            "Executable security matrix",
            "Mongo read-side query repository",
            "Ktor route contract for Admin iOS/Web Admin",
        ),
        safetyRules = listOf(
            "All endpoints are read-only and organization-scoped.",
            "Admin responses use DTO/read models, never raw Mongo/domain internals.",
            "Report calculations use persisted sale, payment, cash and document snapshots.",
            "Historic tax snapshots are never recalculated from current tax configuration.",
            "No endpoint exposes certificate secrets, tokens, private keys or payment provider secrets.",
            "Date-range reports require a valid from/to interval.",
            "Operational dashboard is mobile-first and returns alerts/action hints.",
        ),
        nextRecommendedPhase = "13F — Global Audit & Support API",
    )
}

data class AdminOperationsPhaseClosureReport(
    val phase: String,
    val title: String,
    val closedAt: LocalDate,
    val routeCount: Int,
    val readRouteCount: Int,
    val mutationRouteCount: Int,
    val requiredSurfaces: Set<AdminOperationsSurface>,
    val completedCapabilities: List<String>,
    val safetyRules: List<String>,
    val nextRecommendedPhase: String,
) {
    val totalRouteCountMatchesContract: Boolean
        get() = routeCount == readRouteCount + mutationRouteCount

    val coversAllSurfaces: Boolean
        get() = requiredSurfaces == AdminOperationsSurface.entries.toSet()
}
