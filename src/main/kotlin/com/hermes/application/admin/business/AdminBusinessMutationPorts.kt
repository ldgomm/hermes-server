package com.hermes.application.admin.business

import java.time.Instant

/**
 * Write-side port separated from AdminBusinessRepository so 13A.1 read tests/fakes
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

enum class AdminBusinessAuditAction {
    BUSINESS_SETTINGS_UPDATED,
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
