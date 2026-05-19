package com.hermes.backend.routes

import com.hermes.application.admin.business.AdminBranchLocation
import com.hermes.application.admin.business.AdminBusinessActivitySummary
import com.hermes.application.admin.business.AdminBusinessBranchSummary
import com.hermes.application.admin.business.AdminBusinessEmissionPointSummary
import com.hermes.application.admin.business.AdminBusinessFoundationCounts
import com.hermes.application.admin.business.AdminBusinessFoundationNextAction
import com.hermes.application.admin.business.AdminBusinessFoundationOverviewResult
import com.hermes.application.admin.business.AdminBusinessProfile
import kotlinx.serialization.Serializable

@Serializable
data class AdminBusinessFoundationOverviewResponse(
    val organizationId: String,
    val overallStatus: String,
    val ready: Boolean,
    val generatedAt: String,
    val business: AdminBusinessResponse,
    val readiness: AdminBusinessReadinessResponse,
    val counts: AdminBusinessFoundationCountsResponse,
    val nextActions: List<AdminBusinessFoundationNextActionResponse>,
    val activities: List<AdminActivityResponse>,
    val branches: List<AdminBranchResponse>,
    val emissionPoints: List<AdminEmissionPointResponse>,
)

@Serializable
data class AdminBusinessFoundationCountsResponse(
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
)

@Serializable
data class AdminBusinessFoundationNextActionResponse(
    val code: String,
    val status: String,
    val required: Boolean,
    val action: String,
)

fun AdminBusinessFoundationOverviewResult.toResponse(): AdminBusinessFoundationOverviewResponse =
    AdminBusinessFoundationOverviewResponse(
        organizationId = organizationId,
        overallStatus = overallStatus.name,
        ready = ready,
        generatedAt = generatedAt.toString(),
        business = business.toFoundationResponse(),
        readiness = readiness.toResponse(),
        counts = counts.toResponse(),
        nextActions = nextActions.map { it.toResponse() },
        activities = activities.map { it.toFoundationResponse() },
        branches = branches.map { it.toFoundationResponse() },
        emissionPoints = emissionPoints.map { it.toFoundationResponse() },
    )

private fun AdminBusinessFoundationCounts.toResponse(): AdminBusinessFoundationCountsResponse =
    AdminBusinessFoundationCountsResponse(
        totalActivities = totalActivities,
        activeActivities = activeActivities,
        pausedActivities = pausedActivities,
        archivedActivities = archivedActivities,
        totalBranches = totalBranches,
        activeBranches = activeBranches,
        inactiveBranches = inactiveBranches,
        archivedBranches = archivedBranches,
        totalEmissionPoints = totalEmissionPoints,
        activeEmissionPoints = activeEmissionPoints,
        inactiveEmissionPoints = inactiveEmissionPoints,
        archivedEmissionPoints = archivedEmissionPoints,
        readinessChecks = readinessChecks,
        readyChecks = readyChecks,
        warningChecks = warningChecks,
        blockedChecks = blockedChecks,
    )

private fun AdminBusinessFoundationNextAction.toResponse(): AdminBusinessFoundationNextActionResponse =
    AdminBusinessFoundationNextActionResponse(
        code = code,
        status = status,
        required = required,
        action = action,
    )

private fun AdminBusinessProfile.toFoundationResponse(): AdminBusinessResponse = AdminBusinessResponse(
    id = id,
    countryCode = countryCode,
    taxId = taxId,
    legalName = legalName,
    commercialName = commercialName,
    status = status,
    ownerUserId = ownerUserId,
    defaultCurrency = defaultCurrency,
    timezone = timezone,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString(),
    version = version,
)

private fun AdminBusinessActivitySummary.toFoundationResponse(): AdminActivityResponse = AdminActivityResponse(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    description = description,
    activityType = activityType,
    workflowMode = workflowMode,
    status = status,
    requiresScheduling = requiresScheduling,
    tracksInventory = tracksInventory,
    allowsReceivables = allowsReceivables,
    sortOrder = sortOrder,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString(),
)

private fun AdminBusinessBranchSummary.toFoundationResponse(): AdminBranchResponse = AdminBranchResponse(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = type,
    status = status,
    location = location?.toFoundationResponse(),
    businessHoursId = businessHoursId,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString(),
)

private fun AdminBranchLocation.toFoundationResponse(): AdminBranchLocationResponse = AdminBranchLocationResponse(
    countryCode = countryCode,
    province = province,
    city = city,
    sector = sector,
    addressLine = addressLine,
    latitude = latitude,
    longitude = longitude,
    privacyMode = privacyMode,
)

private fun AdminBusinessEmissionPointSummary.toFoundationResponse(): AdminEmissionPointResponse = AdminEmissionPointResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    establishmentCode = establishmentCode,
    emissionPointCode = emissionPointCode,
    fullCode = fullCode,
    displayName = displayName,
    status = status,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString(),
)
