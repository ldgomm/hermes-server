package com.hermes.backend.routes

import com.hermes.application.admin.tax.AdminTaxProfilesResult
import com.hermes.application.admin.tax.AdminTaxRatesResult
import com.hermes.application.admin.tax.AdminTaxReadinessCheck
import com.hermes.application.admin.tax.AdminTaxReadinessResult
import com.hermes.application.admin.tax.GetAdminTaxReadinessCommand
import com.hermes.application.admin.tax.SearchAdminTaxProfilesCommand
import com.hermes.application.admin.tax.SearchAdminTaxRatesCommand
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemCommand
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemResult
import com.hermes.application.tax.TaxCreateProfileCommand
import com.hermes.application.tax.TaxCreateRateCommand
import com.hermes.application.tax.TaxProfileResult
import com.hermes.application.tax.TaxRateResult
import com.hermes.application.tax.TaxUpdateProfileCommand
import com.hermes.application.tax.TaxUpdateRateCommand
import com.hermes.backend.tax.OrganizationTaxSettingsResponse
import com.hermes.backend.tax.TaxProfileResponse
import com.hermes.backend.tax.TaxRateResponse
import com.hermes.backend.tax.toResponse
import com.hermes.domain.tax.TaxKind
import com.hermes.domain.tax.TaxProfileStatus
import com.hermes.domain.tax.TaxRateStatus
import com.hermes.domain.tax.TaxTreatment
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CreateAdminTaxRateRequest(
    val code: String,
    val name: String,
    val kind: String = "IVA",
    val rate: String,
    val status: String = "ACTIVE",
    val sriTaxCode: String? = null,
    val sriRateCode: String? = null,
    val legalBasis: String,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val reason: String,
)

@Serializable
data class UpdateAdminTaxRateRequest(
    val name: String? = null,
    val kind: String? = null,
    val rate: String? = null,
    val status: String? = null,
    val sriTaxCode: String? = null,
    val clearSriTaxCode: Boolean = false,
    val sriRateCode: String? = null,
    val clearSriRateCode: Boolean = false,
    val legalBasis: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val clearEffectiveTo: Boolean = false,
    val reason: String,
)

@Serializable
data class CreateAdminTaxProfileRequest(
    val code: String,
    val name: String,
    val treatment: String,
    val status: String = "ACTIVE",
    val taxRateCode: String? = null,
    val sriTaxCode: String? = null,
    val sriRateCode: String? = null,
    val legalBasis: String,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val reason: String,
)

@Serializable
data class UpdateAdminTaxProfileRequest(
    val name: String? = null,
    val treatment: String? = null,
    val status: String? = null,
    val taxRateCode: String? = null,
    val clearTaxRate: Boolean = false,
    val sriTaxCode: String? = null,
    val clearSriTaxCode: Boolean = false,
    val sriRateCode: String? = null,
    val clearSriRateCode: Boolean = false,
    val legalBasis: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val clearEffectiveTo: Boolean = false,
    val reason: String,
)

@Serializable
data class AssignAdminCatalogItemTaxProfileRequest(
    val taxProfileCode: String,
    val reason: String,
)

@Serializable
data class AdminTaxRatesResponse(
    val rates: List<TaxRateResponse>,
    val meta: AdminTaxListMetaResponse,
)

@Serializable
data class AdminTaxRateResponse(
    val rate: TaxRateResponse,
)

@Serializable
data class AdminTaxProfilesResponse(
    val profiles: List<TaxProfileResponse>,
    val meta: AdminTaxListMetaResponse,
)

@Serializable
data class AdminTaxProfileResponse(
    val profile: TaxProfileResponse,
)

@Serializable
data class AdminTaxListMetaResponse(
    val count: Int,
)

@Serializable
data class AdminCatalogItemTaxProfileAssignmentResponse(
    val organizationId: String,
    val catalogItemId: String,
    val previousTaxProfileId: String?,
    val taxProfileId: String,
    val taxProfileCode: String,
    val updatedAt: String,
)

@Serializable
data class AdminTaxReadinessResponse(
    val organizationId: String,
    val ready: Boolean,
    val status: String,
    val checkedAt: String,
    val settings: OrganizationTaxSettingsResponse?,
    val checks: List<AdminTaxReadinessCheckResponse>,
    val enabledProfileCount: Int,
    val activeEnabledProfileCount: Int,
    val missingProfileCodes: Set<String>,
    val nextActions: List<String>,
)

@Serializable
data class AdminTaxReadinessCheckResponse(
    val code: String,
    val status: String,
    val severity: String,
    val message: String,
)

fun adminTaxRateSearchCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    query: String?,
    kind: String?,
    statuses: String?,
    effectiveAt: String?,
    limit: Int,
): SearchAdminTaxRatesCommand = SearchAdminTaxRatesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    query = query,
    kind = kind?.let { enumValue<TaxKind>(it) },
    statuses = statuses.csvEnumSet<TaxRateStatus>(),
    effectiveAt = effectiveAt?.let(Instant::parse),
    limit = limit,
)

fun adminTaxProfileSearchCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    query: String?,
    treatment: String?,
    statuses: String?,
    effectiveAt: String?,
    limit: Int,
): SearchAdminTaxProfilesCommand = SearchAdminTaxProfilesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    query = query,
    treatment = treatment?.let { enumValue<TaxTreatment>(it) },
    statuses = statuses.csvEnumSet<TaxProfileStatus>(),
    effectiveAt = effectiveAt?.let(Instant::parse),
    limit = limit,
)

fun CreateAdminTaxRateRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxCreateRateCommand = TaxCreateRateCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    code = code,
    name = name,
    kind = enumValue(kind),
    rate = rate,
    status = enumValue(status),
    sriTaxCode = sriTaxCode,
    sriRateCode = sriRateCode,
    legalBasis = legalBasis,
    effectiveFrom = Instant.parse(effectiveFrom),
    effectiveTo = effectiveTo?.let(Instant::parse),
    reason = reason,
)

fun UpdateAdminTaxRateRequest.toCommand(
    rateId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxUpdateRateCommand = TaxUpdateRateCommand(
    taxRateId = rateId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    name = name,
    kind = kind?.let { enumValue<TaxKind>(it) },
    rate = rate,
    status = status?.let { enumValue<TaxRateStatus>(it) },
    sriTaxCode = sriTaxCode,
    clearSriTaxCode = clearSriTaxCode,
    sriRateCode = sriRateCode,
    clearSriRateCode = clearSriRateCode,
    legalBasis = legalBasis,
    effectiveFrom = effectiveFrom?.let(Instant::parse),
    effectiveTo = effectiveTo?.let(Instant::parse),
    clearEffectiveTo = clearEffectiveTo,
    reason = reason,
)

fun CreateAdminTaxProfileRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxCreateProfileCommand = TaxCreateProfileCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    code = code,
    name = name,
    treatment = enumValue(treatment),
    status = enumValue(status),
    taxRateCode = taxRateCode,
    sriTaxCode = sriTaxCode,
    sriRateCode = sriRateCode,
    legalBasis = legalBasis,
    effectiveFrom = Instant.parse(effectiveFrom),
    effectiveTo = effectiveTo?.let(Instant::parse),
    reason = reason,
)

fun UpdateAdminTaxProfileRequest.toCommand(
    profileId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxUpdateProfileCommand = TaxUpdateProfileCommand(
    taxProfileId = profileId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    name = name,
    treatment = treatment?.let { enumValue<TaxTreatment>(it) },
    status = status?.let { enumValue<TaxProfileStatus>(it) },
    taxRateCode = taxRateCode,
    clearTaxRate = clearTaxRate,
    sriTaxCode = sriTaxCode,
    clearSriTaxCode = clearSriTaxCode,
    sriRateCode = sriRateCode,
    clearSriRateCode = clearSriRateCode,
    legalBasis = legalBasis,
    effectiveFrom = effectiveFrom?.let(Instant::parse),
    effectiveTo = effectiveTo?.let(Instant::parse),
    clearEffectiveTo = clearEffectiveTo,
    reason = reason,
)

fun AssignAdminCatalogItemTaxProfileRequest.toCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): AssignTaxProfileToCatalogItemCommand = AssignTaxProfileToCatalogItemCommand(
    organizationId = organizationId,
    catalogItemId = itemId,
    taxProfileCode = taxProfileCode,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    reason = reason,
)

fun TaxRateResult.toAdminTaxResponse(): AdminTaxRateResponse = AdminTaxRateResponse(rate.toResponse())

fun AdminTaxRatesResult.toAdminTaxResponse(): AdminTaxRatesResponse = AdminTaxRatesResponse(
    rates = rates.map { it.toResponse() },
    meta = AdminTaxListMetaResponse(rates.size),
)

fun TaxProfileResult.toAdminTaxResponse(): AdminTaxProfileResponse = AdminTaxProfileResponse(profile.toResponse())

fun AdminTaxProfilesResult.toAdminTaxResponse(): AdminTaxProfilesResponse = AdminTaxProfilesResponse(
    profiles = profiles.map { it.toResponse() },
    meta = AdminTaxListMetaResponse(profiles.size),
)

fun AssignTaxProfileToCatalogItemResult.toAdminTaxResponse(taxProfileCode: String): AdminCatalogItemTaxProfileAssignmentResponse =
    AdminCatalogItemTaxProfileAssignmentResponse(
        organizationId = assignment.organizationId,
        catalogItemId = assignment.catalogItemId,
        previousTaxProfileId = assignment.previousTaxProfileId,
        taxProfileId = assignment.taxProfileId,
        taxProfileCode = taxProfileCode.trim().lowercase(),
        updatedAt = assignment.updatedAt.toString(),
    )

fun AdminTaxReadinessResult.toAdminTaxResponse(): AdminTaxReadinessResponse = AdminTaxReadinessResponse(
    organizationId = organizationId,
    ready = ready,
    status = status.name,
    checkedAt = checkedAt.toString(),
    settings = settings?.toResponse(),
    checks = checks.map { it.toAdminTaxResponse() },
    enabledProfileCount = enabledProfileCount,
    activeEnabledProfileCount = activeEnabledProfileCount,
    missingProfileCodes = missingProfileCodes,
    nextActions = nextActions,
)

private fun AdminTaxReadinessCheck.toAdminTaxResponse(): AdminTaxReadinessCheckResponse =
    AdminTaxReadinessCheckResponse(
        code = code,
        status = status.name,
        severity = severity.name,
        message = message,
    )

private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValueOf<T>(value.trim().uppercase())

private inline fun <reified T : Enum<T>> String?.csvEnumSet(): Set<T> =
    this
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { enumValue<T>(it) }
        ?.toSet()
        .orEmpty()
