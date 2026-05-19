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

data class ListAdminBranchesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class ListAdminEmissionPointsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)
