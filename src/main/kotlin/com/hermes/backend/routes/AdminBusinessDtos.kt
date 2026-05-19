package com.hermes.backend.routes

import com.hermes.application.admin.business.AdminBranchLocation
import com.hermes.application.admin.business.AdminBranchLocationCommand
import com.hermes.application.admin.business.AdminBusinessActivityResult
import com.hermes.application.admin.business.AdminBusinessActivitySummary
import com.hermes.application.admin.business.AdminBusinessActivitiesResult
import com.hermes.application.admin.business.AdminBusinessBranchResult
import com.hermes.application.admin.business.AdminBusinessBranchSummary
import com.hermes.application.admin.business.AdminBusinessBranchesResult
import com.hermes.application.admin.business.AdminBusinessEmissionPointSummary
import com.hermes.application.admin.business.AdminBusinessEmissionPointsResult
import com.hermes.application.admin.business.AdminBusinessProfile
import com.hermes.application.admin.business.AdminBusinessReadinessCheck
import com.hermes.application.admin.business.AdminBusinessReadinessResult
import com.hermes.application.admin.business.AdminBusinessResult
import com.hermes.application.admin.business.ChangeAdminActivityStatusCommand
import com.hermes.application.admin.business.ChangeAdminBranchStatusCommand
import com.hermes.application.admin.business.CreateAdminActivityCommand
import com.hermes.application.admin.business.CreateAdminBranchCommand
import com.hermes.application.admin.business.UpdateAdminActivityCommand
import com.hermes.application.admin.business.UpdateAdminBranchCommand
import com.hermes.application.admin.business.UpdateAdminBusinessCommand
import kotlinx.serialization.Serializable

@Serializable
data class AdminBusinessResponse(
    val id: String,
    val countryCode: String,
    val taxId: String,
    val legalName: String,
    val commercialName: String,
    val status: String,
    val ownerUserId: String,
    val defaultCurrency: String? = null,
    val timezone: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val version: Long,
)

@Serializable
data class AdminBusinessEnvelope(
    val business: AdminBusinessResponse,
)

@Serializable
data class UpdateAdminBusinessRequest(
    val countryCode: String? = null,
    val taxId: String? = null,
    val legalName: String? = null,
    val commercialName: String? = null,
    val defaultCurrency: String? = null,
    val timezone: String? = null,
    val reason: String,
)

@Serializable
data class AdminActivityResponse(
    val id: String,
    val organizationId: String,
    val code: String? = null,
    val name: String,
    val description: String? = null,
    val activityType: String,
    val workflowMode: String,
    val status: String,
    val requiresScheduling: Boolean,
    val tracksInventory: Boolean,
    val allowsReceivables: Boolean,
    val sortOrder: Int,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class AdminActivityEnvelope(
    val activity: AdminActivityResponse,
)

@Serializable
data class AdminActivitiesResponse(
    val activities: List<AdminActivityResponse>,
)

@Serializable
data class CreateAdminActivityRequest(
    val code: String,
    val name: String,
    val description: String? = null,
    val activityType: String,
    val workflowMode: String,
    val status: String = "active",
    val requiresScheduling: Boolean = false,
    val tracksInventory: Boolean = false,
    val allowsReceivables: Boolean = true,
    val sortOrder: Int = 0,
    val reason: String,
)

@Serializable
data class UpdateAdminActivityRequest(
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
    val clearDescription: Boolean = false,
    val activityType: String? = null,
    val workflowMode: String? = null,
    val requiresScheduling: Boolean? = null,
    val tracksInventory: Boolean? = null,
    val allowsReceivables: Boolean? = null,
    val sortOrder: Int? = null,
    val reason: String,
)

@Serializable
data class ChangeAdminActivityStatusRequest(
    val reason: String,
)

@Serializable
data class AdminBranchLocationRequest(
    val countryCode: String? = null,
    val province: String? = null,
    val city: String? = null,
    val sector: String? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val privacyMode: String? = null,
)

@Serializable
data class AdminBranchLocationResponse(
    val countryCode: String? = null,
    val province: String? = null,
    val city: String? = null,
    val sector: String? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val privacyMode: String? = null,
)

@Serializable
data class AdminBranchResponse(
    val id: String,
    val organizationId: String,
    val code: String? = null,
    val name: String,
    val type: String,
    val status: String,
    val location: AdminBranchLocationResponse? = null,
    val businessHoursId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class AdminBranchEnvelope(
    val branch: AdminBranchResponse,
)

@Serializable
data class AdminBranchesResponse(
    val branches: List<AdminBranchResponse>,
)

@Serializable
data class CreateAdminBranchRequest(
    val code: String,
    val name: String,
    val type: String = "branch",
    val status: String = "active",
    val location: AdminBranchLocationRequest? = null,
    val businessHoursId: String? = null,
    val reason: String,
)

@Serializable
data class UpdateAdminBranchRequest(
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val location: AdminBranchLocationRequest? = null,
    val clearLocation: Boolean = false,
    val businessHoursId: String? = null,
    val clearBusinessHoursId: Boolean = false,
    val reason: String,
)

@Serializable
data class ChangeAdminBranchStatusRequest(
    val reason: String,
)

@Serializable
data class AdminEmissionPointResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val fullCode: String,
    val displayName: String,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class AdminEmissionPointsResponse(
    val emissionPoints: List<AdminEmissionPointResponse>,
)

@Serializable
data class AdminBusinessReadinessResponse(
    val organizationId: String,
    val overallStatus: String,
    val ready: Boolean,
    val generatedAt: String,
    val checks: List<AdminBusinessReadinessCheckResponse>,
)

@Serializable
data class AdminBusinessReadinessCheckResponse(
    val code: String,
    val status: String,
    val required: Boolean,
    val message: String,
    val action: String? = null,
)

fun UpdateAdminBusinessRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): UpdateAdminBusinessCommand = UpdateAdminBusinessCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    countryCode = countryCode,
    taxId = taxId,
    legalName = legalName,
    commercialName = commercialName,
    defaultCurrency = defaultCurrency,
    timezone = timezone,
    reason = reason,
)

fun CreateAdminActivityRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CreateAdminActivityCommand = CreateAdminActivityCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
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
    reason = reason,
)

fun UpdateAdminActivityRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    activityId: String,
): UpdateAdminActivityCommand = UpdateAdminActivityCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    activityId = activityId,
    code = code,
    name = name,
    description = description,
    clearDescription = clearDescription,
    activityType = activityType,
    workflowMode = workflowMode,
    requiresScheduling = requiresScheduling,
    tracksInventory = tracksInventory,
    allowsReceivables = allowsReceivables,
    sortOrder = sortOrder,
    reason = reason,
)

fun ChangeAdminActivityStatusRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    activityId: String,
    targetStatus: String,
): ChangeAdminActivityStatusCommand = ChangeAdminActivityStatusCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    activityId = activityId,
    targetStatus = targetStatus,
    reason = reason,
)

fun CreateAdminBranchRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CreateAdminBranchCommand = CreateAdminBranchCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    code = code,
    name = name,
    type = type,
    status = status,
    location = location?.toCommand(),
    businessHoursId = businessHoursId,
    reason = reason,
)

fun UpdateAdminBranchRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    branchId: String,
): UpdateAdminBranchCommand = UpdateAdminBranchCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    branchId = branchId,
    code = code,
    name = name,
    type = type,
    location = location?.toCommand(),
    clearLocation = clearLocation,
    businessHoursId = businessHoursId,
    clearBusinessHoursId = clearBusinessHoursId,
    reason = reason,
)

fun ChangeAdminBranchStatusRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    branchId: String,
    targetStatus: String,
): ChangeAdminBranchStatusCommand = ChangeAdminBranchStatusCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    branchId = branchId,
    targetStatus = targetStatus,
    reason = reason,
)

private fun AdminBranchLocationRequest.toCommand(): AdminBranchLocationCommand = AdminBranchLocationCommand(
    countryCode = countryCode,
    province = province,
    city = city,
    sector = sector,
    addressLine = addressLine,
    latitude = latitude,
    longitude = longitude,
    privacyMode = privacyMode,
)

fun AdminBusinessResult.toResponse(): AdminBusinessEnvelope = AdminBusinessEnvelope(business.toResponse())

fun AdminBusinessActivityResult.toResponse(): AdminActivityEnvelope = AdminActivityEnvelope(activity.toResponse())

fun AdminBusinessActivitiesResult.toResponse(): AdminActivitiesResponse =
    AdminActivitiesResponse(activities.map { it.toResponse() })

fun AdminBusinessBranchResult.toResponse(): AdminBranchEnvelope = AdminBranchEnvelope(branch.toResponse())

fun AdminBusinessBranchesResult.toResponse(): AdminBranchesResponse =
    AdminBranchesResponse(branches.map { it.toResponse() })

fun AdminBusinessEmissionPointsResult.toResponse(): AdminEmissionPointsResponse =
    AdminEmissionPointsResponse(emissionPoints.map { it.toResponse() })

fun AdminBusinessReadinessResult.toResponse(): AdminBusinessReadinessResponse = AdminBusinessReadinessResponse(
    organizationId = organizationId,
    overallStatus = overallStatus.name,
    ready = ready,
    generatedAt = generatedAt.toString(),
    checks = checks.map { it.toResponse() },
)

private fun AdminBusinessProfile.toResponse(): AdminBusinessResponse = AdminBusinessResponse(
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

private fun AdminBusinessActivitySummary.toResponse(): AdminActivityResponse = AdminActivityResponse(
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

private fun AdminBusinessBranchSummary.toResponse(): AdminBranchResponse = AdminBranchResponse(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = type,
    status = status,
    location = location?.toResponse(),
    businessHoursId = businessHoursId,
    createdAt = createdAt?.toString(),
    updatedAt = updatedAt?.toString(),
)

private fun AdminBranchLocation.toResponse(): AdminBranchLocationResponse = AdminBranchLocationResponse(
    countryCode = countryCode,
    province = province,
    city = city,
    sector = sector,
    addressLine = addressLine,
    latitude = latitude,
    longitude = longitude,
    privacyMode = privacyMode,
)

private fun AdminBusinessEmissionPointSummary.toResponse(): AdminEmissionPointResponse = AdminEmissionPointResponse(
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

private fun AdminBusinessReadinessCheck.toResponse(): AdminBusinessReadinessCheckResponse =
    AdminBusinessReadinessCheckResponse(
        code = code.name,
        status = status.name,
        required = required,
        message = message,
        action = action,
    )
