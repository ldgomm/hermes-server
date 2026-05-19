package com.hermes.application.admin.business

import java.time.Instant

data class AdminBusinessResult(
    val business: AdminBusinessProfile,
)

data class AdminBusinessActivitiesResult(
    val activities: List<AdminBusinessActivitySummary>,
)

data class AdminBusinessActivityResult(
    val activity: AdminBusinessActivitySummary,
)

data class AdminBusinessBranchesResult(
    val branches: List<AdminBusinessBranchSummary>,
)

data class AdminBusinessEmissionPointsResult(
    val emissionPoints: List<AdminBusinessEmissionPointSummary>,
)

data class AdminBusinessReadinessResult(
    val organizationId: String,
    val overallStatus: AdminBusinessReadinessStatus,
    val checks: List<AdminBusinessReadinessCheck>,
    val generatedAt: Instant,
) {
    val ready: Boolean get() = overallStatus == AdminBusinessReadinessStatus.READY
}

data class AdminBusinessProfile(
    val id: String,
    val countryCode: String,
    val taxId: String,
    val legalName: String,
    val commercialName: String,
    val status: String,
    val ownerUserId: String,
    val defaultCurrency: String? = null,
    val timezone: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val version: Long = 1,
) {
    val active: Boolean get() = status.equals("active", ignoreCase = true)
}

data class AdminBusinessActivitySummary(
    val id: String,
    val organizationId: String,
    val code: String?,
    val name: String,
    val description: String? = null,
    val activityType: String,
    val workflowMode: String,
    val status: String,
    val requiresScheduling: Boolean,
    val tracksInventory: Boolean,
    val allowsReceivables: Boolean,
    val sortOrder: Int = 0,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    val active: Boolean get() = status.equals("active", ignoreCase = true)
    val archived: Boolean get() = status.equals("archived", ignoreCase = true)
}

data class AdminBusinessBranchSummary(
    val id: String,
    val organizationId: String,
    val code: String?,
    val name: String,
    val type: String,
    val status: String,
    val location: AdminBranchLocation? = null,
    val businessHoursId: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    val active: Boolean get() = status.equals("active", ignoreCase = true)
}

data class AdminBranchLocation(
    val countryCode: String? = null,
    val province: String? = null,
    val city: String? = null,
    val sector: String? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val privacyMode: String? = null,
)

data class AdminBusinessEmissionPointSummary(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val displayName: String,
    val status: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
) {
    val active: Boolean get() = status.equals("active", ignoreCase = true)
    val fullCode: String get() = "$establishmentCode-$emissionPointCode"
}

enum class AdminBusinessReadinessStatus {
    READY,
    WARNING,
    BLOCKED,
}

enum class AdminBusinessReadinessCheckCode {
    BUSINESS_EXISTS,
    BUSINESS_ACTIVE,
    TAX_ID_PRESENT,
    LEGAL_NAME_PRESENT,
    COMMERCIAL_NAME_PRESENT,
    ACTIVE_ACTIVITY_EXISTS,
    ACTIVE_BRANCH_EXISTS,
    ACTIVE_EMISSION_POINT_EXISTS,
    TAX_SETTINGS_INITIALIZED,
    SRI_SETTINGS_CONFIGURED,
    OWNER_OR_ADMIN_CONFIGURED,
}

data class AdminBusinessReadinessCheck(
    val code: AdminBusinessReadinessCheckCode,
    val status: AdminBusinessReadinessStatus,
    val required: Boolean,
    val message: String,
    val action: String? = null,
)
