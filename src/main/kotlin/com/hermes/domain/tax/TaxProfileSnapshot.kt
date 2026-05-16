package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.LocalDate

data class TaxProfileSnapshot(
    val taxProfileId: String,
    val code: String,
    val displayName: String,
    val taxName: String,
    val treatment: TaxTreatment,
    val ratePercent: BigDecimal,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: LocalDate?,
    val effectiveTo: LocalDate?,
    val source: String,
) {
    companion object {
        fun from(
            profile: TaxProfile,
            date: LocalDate,
            source: String = "admin_tax_configuration",
        ): TaxProfileSnapshot {
            if (profile.status != TaxProfileStatus.ACTIVE) {
                throw DomainRuleViolation("Tax profile ${profile.code} is not active for new sales.")
            }

            val rate = profile.rate
            if (rate != null) {
                if (rate.status != TaxRateStatus.ACTIVE) {
                    throw DomainRuleViolation("Tax rate ${rate.id} is not active for new sales.")
                }
                if (!rate.isEffectiveOn(date)) {
                    throw DomainRuleViolation("Tax rate ${rate.id} is not effective on $date.")
                }
            }

            return TaxProfileSnapshot(
                taxProfileId = profile.id,
                code = profile.code,
                displayName = profile.displayName,
                taxName = profile.taxName,
                treatment = profile.treatment,
                ratePercent = rate?.ratePercent ?: BigDecimal.ZERO,
                sriTaxCode = rate?.sriTaxCode,
                sriRateCode = rate?.sriRateCode,
                legalBasis = profile.legalBasis,
                effectiveFrom = rate?.effectiveFrom,
                effectiveTo = rate?.effectiveTo,
                source = source,
            )
        }
    }
}
