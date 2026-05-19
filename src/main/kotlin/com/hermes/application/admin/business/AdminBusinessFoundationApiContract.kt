package com.hermes.application.admin.business

/**
 * Lightweight executable contract for the Fase 13A Admin Business Foundation API.
 * Useful for smoke tests, API documentation and Admin iOS planning.
 */
object AdminBusinessFoundationApiContract {
    val routes: List<AdminBusinessFoundationRouteContract> = listOf(
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/business", "Business profile"),
        AdminBusinessFoundationRouteContract("PUT", "/api/v1/admin/business", "Update business profile"),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/business/readiness", "Business readiness checks"),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/business/overview", "Business foundation overview"),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/activities", "List business activities"),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/activities/{activityId}", "Get business activity"),
        AdminBusinessFoundationRouteContract("POST", "/api/v1/admin/activities", "Create business activity"),
        AdminBusinessFoundationRouteContract(
            "PUT", "/api/v1/admin/activities/{activityId}", "Update business activity"
        ),
        AdminBusinessFoundationRouteContract(
            "POST", "/api/v1/admin/activities/{activityId}/activate", "Activate business activity"
        ),
        AdminBusinessFoundationRouteContract(
            "POST", "/api/v1/admin/activities/{activityId}/deactivate", "Deactivate business activity"
        ),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/branches", "List branches"),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/branches/{branchId}", "Get branch"),
        AdminBusinessFoundationRouteContract("POST", "/api/v1/admin/branches", "Create branch"),
        AdminBusinessFoundationRouteContract("PUT", "/api/v1/admin/branches/{branchId}", "Update branch"),
        AdminBusinessFoundationRouteContract("POST", "/api/v1/admin/branches/{branchId}/activate", "Activate branch"),
        AdminBusinessFoundationRouteContract(
            "POST", "/api/v1/admin/branches/{branchId}/deactivate", "Deactivate branch"
        ),
        AdminBusinessFoundationRouteContract("GET", "/api/v1/admin/emission-points", "List emission points"),
        AdminBusinessFoundationRouteContract(
            "GET", "/api/v1/admin/emission-points/{emissionPointId}", "Get emission point"
        ),
        AdminBusinessFoundationRouteContract("POST", "/api/v1/admin/emission-points", "Create emission point"),
        AdminBusinessFoundationRouteContract(
            "PUT", "/api/v1/admin/emission-points/{emissionPointId}", "Update emission point"
        ),
        AdminBusinessFoundationRouteContract(
            "POST", "/api/v1/admin/emission-points/{emissionPointId}/activate", "Activate emission point"
        ),
        AdminBusinessFoundationRouteContract(
            "POST", "/api/v1/admin/emission-points/{emissionPointId}/deactivate", "Deactivate emission point"
        ),
    )
}

data class AdminBusinessFoundationRouteContract(
    val method: String,
    val path: String,
    val description: String,
)
