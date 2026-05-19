package com.hermes.application.admin.business

/**
 * Commands are intentionally small and route-friendly.
 * The active organization is resolved by the backend auth middleware.
 */
data class GetAdminBusinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class UpdateAdminBusinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val countryCode: String? = null,
    val taxId: String? = null,
    val legalName: String? = null,
    val commercialName: String? = null,
    val defaultCurrency: String? = null,
    val timezone: String? = null,
    val reason: String,
)

data class GetAdminBusinessReadinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class ListAdminActivitiesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class GetAdminActivityCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val activityId: String,
)

data class CreateAdminActivityCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
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

data class UpdateAdminActivityCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val activityId: String,
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

data class ChangeAdminActivityStatusCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val activityId: String,
    val targetStatus: String,
    val reason: String,
)

data class ListAdminBranchesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class GetAdminBranchCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String,
)

data class AdminBranchLocationCommand(
    val countryCode: String? = null,
    val province: String? = null,
    val city: String? = null,
    val sector: String? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val privacyMode: String? = null,
)

data class CreateAdminBranchCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val code: String,
    val name: String,
    val type: String = "branch",
    val status: String = "active",
    val location: AdminBranchLocationCommand? = null,
    val businessHoursId: String? = null,
    val reason: String,
)

data class UpdateAdminBranchCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String,
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val location: AdminBranchLocationCommand? = null,
    val clearLocation: Boolean = false,
    val businessHoursId: String? = null,
    val clearBusinessHoursId: Boolean = false,
    val reason: String,
)

data class ChangeAdminBranchStatusCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String,
    val targetStatus: String,
    val reason: String,
)

data class ListAdminEmissionPointsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)
