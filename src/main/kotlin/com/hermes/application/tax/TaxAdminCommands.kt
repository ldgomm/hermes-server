package com.hermes.application.tax

import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxKind
import com.hermes.domain.tax.TaxProfileStatus
import com.hermes.domain.tax.TaxRateStatus
import com.hermes.domain.tax.TaxRegimeCode
import com.hermes.domain.tax.TaxTreatment
import java.time.Instant

data class TaxCreateRateCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val code: String,
    val name: String,
    val kind: TaxKind,
    val rate: String,
    val status: TaxRateStatus = TaxRateStatus.ACTIVE,
    val sriTaxCode: String? = null,
    val sriRateCode: String? = null,
    val legalBasis: String,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
    val reason: String? = null,
)

data class TaxUpdateRateCommand(
    val taxRateId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val name: String? = null,
    val kind: TaxKind? = null,
    val rate: String? = null,
    val status: TaxRateStatus? = null,
    val sriTaxCode: String? = null,
    val clearSriTaxCode: Boolean = false,
    val sriRateCode: String? = null,
    val clearSriRateCode: Boolean = false,
    val legalBasis: String? = null,
    val effectiveFrom: Instant? = null,
    val effectiveTo: Instant? = null,
    val clearEffectiveTo: Boolean = false,
    val reason: String,
)

data class TaxGetRateCommand(
    val taxRateId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class TaxCreateProfileCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val code: String,
    val name: String,
    val treatment: TaxTreatment,
    val status: TaxProfileStatus = TaxProfileStatus.ACTIVE,
    val taxRateCode: String? = null,
    val sriTaxCode: String? = null,
    val sriRateCode: String? = null,
    val legalBasis: String,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
    val reason: String? = null,
)

data class TaxUpdateProfileCommand(
    val taxProfileId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val name: String? = null,
    val treatment: TaxTreatment? = null,
    val status: TaxProfileStatus? = null,
    val taxRateCode: String? = null,
    val clearTaxRate: Boolean = false,
    val sriTaxCode: String? = null,
    val clearSriTaxCode: Boolean = false,
    val sriRateCode: String? = null,
    val clearSriRateCode: Boolean = false,
    val legalBasis: String? = null,
    val effectiveFrom: Instant? = null,
    val effectiveTo: Instant? = null,
    val clearEffectiveTo: Boolean = false,
    val reason: String,
)

data class TaxGetProfileCommand(
    val taxProfileId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class TaxUpdateOrganizationSettingsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val regime: TaxRegimeCode? = null,
    val defaultTaxProfileCode: String? = null,
    val enabledTaxProfileCodes: Set<String>? = null,
    val allowTaxInclusivePrices: Boolean? = null,
    val allowManualLineDiscounts: Boolean? = null,
    val requireTaxProfileForCatalogItems: Boolean? = null,
    val status: OrganizationTaxSettingsStatus? = null,
    val reason: String,
)