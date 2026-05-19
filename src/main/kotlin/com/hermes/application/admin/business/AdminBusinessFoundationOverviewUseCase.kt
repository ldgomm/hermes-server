package com.hermes.application.admin.business

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

/**
 * Fase 13A.6 — Closure/read model for Admin Business Foundation.
 *
 * This use case gives Admin iOS/Web Admin a single screen-friendly snapshot of
 * the configured business foundation without mixing responsibilities with SRI
 * issuance, tax administration, catalog, sales or cash modules.
 */
data class GetAdminBusinessFoundationOverviewCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

class GetAdminBusinessFoundationOverviewUseCase(
    private val repository: AdminBusinessRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetAdminBusinessFoundationOverviewCommand): AdminBusinessFoundationOverviewResult {
        assertCanViewBusiness(command.actorEffectivePermissions)

        val organizationId = command.organizationId.required("Organization id")
        val business =
            repository.findBusiness(organizationId) ?: throw DomainRuleViolation("Organization does not exist.")

        val activities = repository.listActivities(organizationId).sortedWith(compareBy({ it.sortOrder }, { it.name }))
        val branches =
            repository.listBranches(organizationId).sortedWith(compareBy({ it.code ?: it.name }, { it.name }))
        val emissionPoints = repository.listEmissionPoints(organizationId)
            .sortedWith(compareBy({ it.establishmentCode }, { it.emissionPointCode }, { it.displayName }))

        val readiness = GetAdminBusinessReadinessUseCase(repository, clock).execute(
            GetAdminBusinessReadinessCommand(
                organizationId = organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
            )
        )

        return AdminBusinessFoundationOverviewResult(
            organizationId = organizationId,
            business = business,
            readiness = readiness,
            counts = AdminBusinessFoundationCounts.from(
                activities = activities,
                branches = branches,
                emissionPoints = emissionPoints,
                readiness = readiness,
            ),
            activities = activities,
            branches = branches,
            emissionPoints = emissionPoints,
            nextActions = readiness.checks.filter { it.status != AdminBusinessReadinessStatus.READY && it.action != null }
                .map {
                    AdminBusinessFoundationNextAction(
                        code = it.code.name,
                        status = it.status.name,
                        required = it.required,
                        action = it.action.orEmpty(),
                    )
                },
            generatedAt = Instant.now(clock),
        )
    }
}

data class AdminBusinessFoundationOverviewResult(
    val organizationId: String,
    val business: AdminBusinessProfile,
    val readiness: AdminBusinessReadinessResult,
    val counts: AdminBusinessFoundationCounts,
    val activities: List<AdminBusinessActivitySummary>,
    val branches: List<AdminBusinessBranchSummary>,
    val emissionPoints: List<AdminBusinessEmissionPointSummary>,
    val nextActions: List<AdminBusinessFoundationNextAction>,
    val generatedAt: Instant,
) {
    val ready: Boolean get() = readiness.ready
    val overallStatus: AdminBusinessReadinessStatus get() = readiness.overallStatus
}

data class AdminBusinessFoundationCounts(
    val totalActivities: Int,
    val activeActivities: Int,
    val pausedActivities: Int,
    val archivedActivities: Int,
    val totalBranches: Int,
    val activeBranches: Int,
    val inactiveBranches: Int,
    val archivedBranches: Int,
    val totalEmissionPoints: Int,
    val activeEmissionPoints: Int,
    val inactiveEmissionPoints: Int,
    val archivedEmissionPoints: Int,
    val readinessChecks: Int,
    val readyChecks: Int,
    val warningChecks: Int,
    val blockedChecks: Int,
) {
    companion object {
        fun from(
            activities: List<AdminBusinessActivitySummary>,
            branches: List<AdminBusinessBranchSummary>,
            emissionPoints: List<AdminBusinessEmissionPointSummary>,
            readiness: AdminBusinessReadinessResult,
        ): AdminBusinessFoundationCounts = AdminBusinessFoundationCounts(
            totalActivities = activities.size,
            activeActivities = activities.count { it.active },
            pausedActivities = activities.count { it.status.equals("paused", ignoreCase = true) },
            archivedActivities = activities.count { it.archived },
            totalBranches = branches.size,
            activeBranches = branches.count { it.active },
            inactiveBranches = branches.count { it.inactive },
            archivedBranches = branches.count { it.archived },
            totalEmissionPoints = emissionPoints.size,
            activeEmissionPoints = emissionPoints.count { it.active },
            inactiveEmissionPoints = emissionPoints.count { it.inactive },
            archivedEmissionPoints = emissionPoints.count { it.archived },
            readinessChecks = readiness.checks.size,
            readyChecks = readiness.checks.count { it.status == AdminBusinessReadinessStatus.READY },
            warningChecks = readiness.checks.count { it.status == AdminBusinessReadinessStatus.WARNING },
            blockedChecks = readiness.checks.count { it.status == AdminBusinessReadinessStatus.BLOCKED },
        )
    }
}

data class AdminBusinessFoundationNextAction(
    val code: String,
    val status: String,
    val required: Boolean,
    val action: String,
)
