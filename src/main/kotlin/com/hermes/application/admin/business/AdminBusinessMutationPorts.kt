package com.hermes.application.admin.business

import java.time.Instant
import java.util.*

/**
 * Write-side ports are separated from AdminBusinessRepository so read tests/fakes
 * do not need to change unless they exercise mutations.
 */
interface AdminBusinessMutationRepository {
    fun existsBusinessWithTaxId(
        countryCode: String,
        taxId: String,
        excludeOrganizationId: String,
    ): Boolean

    fun updateBusiness(patch: AdminBusinessUpdatePatch): AdminBusinessProfile
}

data class AdminBusinessUpdatePatch(
    val organizationId: String,
    val countryCode: String? = null,
    val taxId: String? = null,
    val legalName: String? = null,
    val commercialName: String? = null,
    val defaultCurrency: String? = null,
    val timezone: String? = null,
    val updatedBy: String,
    val updatedAt: Instant,
)

interface AdminActivityMutationRepository {
    fun findActivity(organizationId: String, activityId: String): AdminBusinessActivitySummary?

    fun existsActivityCode(
        organizationId: String,
        code: String,
        excludeActivityId: String? = null,
    ): Boolean

    fun createActivity(draft: AdminActivityCreateDraft): AdminBusinessActivitySummary
    fun updateActivity(patch: AdminActivityUpdatePatch): AdminBusinessActivitySummary
    fun updateActivityStatus(patch: AdminActivityStatusPatch): AdminBusinessActivitySummary
}

data class AdminActivityCreateDraft(
    val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val activityType: String,
    val workflowMode: String,
    val status: String,
    val requiresScheduling: Boolean,
    val tracksInventory: Boolean,
    val allowsReceivables: Boolean,
    val sortOrder: Int,
    val createdBy: String,
    val createdAt: Instant,
)

data class AdminActivityUpdatePatch(
    val organizationId: String,
    val activityId: String,
    val code: String? = null,
    val name: String? = null,
    val description: String? = null,
    val changeDescription: Boolean = false,
    val activityType: String? = null,
    val workflowMode: String? = null,
    val requiresScheduling: Boolean? = null,
    val tracksInventory: Boolean? = null,
    val allowsReceivables: Boolean? = null,
    val sortOrder: Int? = null,
    val updatedBy: String,
    val updatedAt: Instant,
) {
    fun hasChanges(): Boolean = listOf(
        code,
        name,
        activityType,
        workflowMode,
        requiresScheduling,
        tracksInventory,
        allowsReceivables,
        sortOrder,
    ).any { it != null } || changeDescription
}

data class AdminActivityStatusPatch(
    val organizationId: String,
    val activityId: String,
    val status: String,
    val updatedBy: String,
    val updatedAt: Instant,
)

interface AdminBranchMutationRepository {
    fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary?

    fun existsBranchCode(
        organizationId: String,
        code: String,
        excludeBranchId: String? = null,
    ): Boolean

    fun hasActiveMainBranch(organizationId: String, excludeBranchId: String? = null): Boolean
    fun countActiveBranches(organizationId: String, excludeBranchId: String? = null): Int
    fun hasActiveEmissionPoints(organizationId: String, branchId: String): Boolean

    fun createBranch(draft: AdminBranchCreateDraft): AdminBusinessBranchSummary
    fun updateBranch(patch: AdminBranchUpdatePatch): AdminBusinessBranchSummary
    fun updateBranchStatus(patch: AdminBranchStatusPatch): AdminBusinessBranchSummary
}

data class AdminBranchCreateDraft(
    val id: String,
    val organizationId: String,
    val code: String,
    val name: String,
    val type: String,
    val status: String,
    val location: AdminBranchLocation? = null,
    val businessHoursId: String? = null,
    val createdBy: String,
    val createdAt: Instant,
)

data class AdminBranchUpdatePatch(
    val organizationId: String,
    val branchId: String,
    val code: String? = null,
    val name: String? = null,
    val type: String? = null,
    val location: AdminBranchLocation? = null,
    val changeLocation: Boolean = false,
    val businessHoursId: String? = null,
    val changeBusinessHoursId: Boolean = false,
    val updatedBy: String,
    val updatedAt: Instant,
) {
    fun hasChanges(): Boolean = listOf(code, name, type).any { it != null } || changeLocation || changeBusinessHoursId
}

data class AdminBranchStatusPatch(
    val organizationId: String,
    val branchId: String,
    val status: String,
    val updatedBy: String,
    val updatedAt: Instant,
)


interface AdminEmissionPointMutationRepository {
    fun findEmissionPoint(organizationId: String, emissionPointId: String): AdminBusinessEmissionPointSummary?
    fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary?

    fun existsEmissionPointCodes(
        organizationId: String,
        establishmentCode: String,
        emissionPointCode: String,
        excludeEmissionPointId: String? = null,
    ): Boolean

    fun createEmissionPoint(draft: AdminEmissionPointCreateDraft): AdminBusinessEmissionPointSummary
    fun updateEmissionPoint(patch: AdminEmissionPointUpdatePatch): AdminBusinessEmissionPointSummary
    fun updateEmissionPointStatus(patch: AdminEmissionPointStatusPatch): AdminBusinessEmissionPointSummary
}

data class AdminEmissionPointCreateDraft(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val displayName: String,
    val status: String,
    val createdBy: String,
    val createdAt: Instant,
)

data class AdminEmissionPointUpdatePatch(
    val organizationId: String,
    val emissionPointId: String,
    val branchId: String? = null,
    val establishmentCode: String? = null,
    val emissionPointCode: String? = null,
    val displayName: String? = null,
    val updatedBy: String,
    val updatedAt: Instant,
) {
    fun hasChanges(): Boolean = listOf(branchId, establishmentCode, emissionPointCode, displayName).any { it != null }
}

data class AdminEmissionPointStatusPatch(
    val organizationId: String,
    val emissionPointId: String,
    val status: String,
    val updatedBy: String,
    val updatedAt: Instant,
)

enum class AdminBusinessAuditAction {
    BUSINESS_SETTINGS_UPDATED,
    ACTIVITY_CREATED,
    ACTIVITY_UPDATED,
    ACTIVITY_ACTIVATED,
    ACTIVITY_DEACTIVATED,
    BRANCH_CREATED,
    BRANCH_UPDATED,
    BRANCH_ACTIVATED,
    BRANCH_DEACTIVATED,
    EMISSION_POINT_CREATED,
    EMISSION_POINT_UPDATED,
    EMISSION_POINT_ACTIVATED,
    EMISSION_POINT_DEACTIVATED,
}

data class AdminBusinessAuditEvent(
    val action: AdminBusinessAuditAction,
    val organizationId: String,
    val actorUserId: String,
    val targetId: String,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String,
    val createdAt: Instant,
)

interface AdminBusinessAuditLogger {
    fun log(event: AdminBusinessAuditEvent)
}

object NoopAdminBusinessAuditLogger : AdminBusinessAuditLogger {
    override fun log(event: AdminBusinessAuditEvent) = Unit
}

fun interface AdminBusinessIdGenerator {
    fun newId(prefix: String): String
}

class UuidAdminBusinessIdGenerator : AdminBusinessIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}
