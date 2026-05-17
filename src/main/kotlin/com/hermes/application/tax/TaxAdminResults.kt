package com.hermes.application.tax

import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxRate

data class TaxRateResult(
    val rate: TaxRate,
)

data class TaxProfileResult(
    val profile: TaxProfile,
)

data class OrganizationTaxSettingsMutationResult(
    val settings: OrganizationTaxSettings,
)