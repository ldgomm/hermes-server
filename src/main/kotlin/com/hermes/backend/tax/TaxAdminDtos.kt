package com.hermes.backend.tax

import com.hermes.application.tax.*
import com.hermes.domain.tax.*
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class TaxCreateRateRequest(
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
    val reason: String? = null,
)

@Serializable
data class TaxUpdateRateRequest(
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
data class TaxCreateProfileRequest(
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
    val reason: String? = null,
)

@Serializable
data class TaxUpdateProfileRequest(
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
data class TaxUpdateOrganizationSettingsRequest(
    val regime: String? = null,
    val defaultTaxProfileCode: String? = null,
    val enabledTaxProfileCodes: Set<String>? = null,
    val allowTaxInclusivePrices: Boolean? = null,
    val allowManualLineDiscounts: Boolean? = null,
    val requireTaxProfileForCatalogItems: Boolean? = null,
    val status: String? = null,
    val reason: String,
)

fun TaxCreateRateRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxCreateRateCommand =
    TaxCreateRateCommand(
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

fun TaxUpdateRateRequest.toCommand(
    taxRateId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxUpdateRateCommand =
    TaxUpdateRateCommand(
        taxRateId = taxRateId,
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

fun TaxCreateProfileRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxCreateProfileCommand =
    TaxCreateProfileCommand(
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

fun TaxUpdateProfileRequest.toCommand(
    taxProfileId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxUpdateProfileCommand =
    TaxUpdateProfileCommand(
        taxProfileId = taxProfileId,
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

fun TaxUpdateOrganizationSettingsRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxUpdateOrganizationSettingsCommand =
    TaxUpdateOrganizationSettingsCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        regime = regime?.let { enumValue<TaxRegimeCode>(it) },
        defaultTaxProfileCode = defaultTaxProfileCode,
        enabledTaxProfileCodes = enabledTaxProfileCodes,
        allowTaxInclusivePrices = allowTaxInclusivePrices,
        allowManualLineDiscounts = allowManualLineDiscounts,
        requireTaxProfileForCatalogItems = requireTaxProfileForCatalogItems,
        status = status?.let { enumValue<OrganizationTaxSettingsStatus>(it) },
        reason = reason,
    )

private inline fun <reified T : Enum<T>> enumValue(value: String): T =
    enumValueOf<T>(value.trim().uppercase())