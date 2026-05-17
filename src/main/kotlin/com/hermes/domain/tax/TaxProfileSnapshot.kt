package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class TaxProfileSnapshot(
    val profileId: String,
    val profileCode: String,
    val profileName: String,
    val treatment: TaxTreatment,
    val taxKind: TaxKind?,
    val rateCode: String?,
    val rateName: String?,
    val rate: BigDecimal,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: Instant,
    val effectiveTo: Instant?,
    val source: TaxSource,
    val capturedAt: Instant,
    val profileVersion: Long,
    val rateVersion: Long?,
) {
    init {
        if (profileId.isBlank()) throw DomainRuleViolation("Tax snapshot profileId cannot be blank.")
        if (profileCode.isBlank()) throw DomainRuleViolation("Tax snapshot profileCode cannot be blank.")
        if (profileName.isBlank()) throw DomainRuleViolation("Tax snapshot profileName cannot be blank.")
        if (legalBasis.isBlank()) throw DomainRuleViolation("Tax snapshot legalBasis cannot be blank.")
        if (rate.scale() != TaxRate.RATE_SCALE) throw DomainRuleViolation("Tax snapshot rate scale is invalid.")
        if (rate < BigDecimal.ZERO) throw DomainRuleViolation("Tax snapshot rate cannot be negative.")
        if (profileVersion < 1) throw DomainRuleViolation("Tax snapshot profileVersion must be positive.")
        if (rateVersion != null && rateVersion < 1) throw DomainRuleViolation("Tax snapshot rateVersion must be positive.")
    }

    val isElectronicEmissionCompatible: Boolean
        get() = treatment != TaxTreatment.NO_TAX_INTERNAL &&
            !sriTaxCode.isNullOrBlank() &&
            !sriRateCode.isNullOrBlank()

    val isTaxedWithPositiveRate: Boolean
        get() = treatment in setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL) &&
            rate.signum() > 0

    fun fraction(scale: Int = 8): BigDecimal =
        rate.divide(BigDecimal("100"), scale, RoundingMode.HALF_UP)

    companion object {
        fun from(profile: TaxProfile, capturedAt: Instant): TaxProfileSnapshot {
            val rate = profile.taxRate
            return TaxProfileSnapshot(
                profileId = profile.id,
                profileCode = profile.code,
                profileName = profile.name,
                treatment = profile.treatment,
                taxKind = rate?.kind,
                rateCode = rate?.code,
                rateName = rate?.name,
                rate = rate?.rate ?: BigDecimal.ZERO.setScale(TaxRate.RATE_SCALE),
                sriTaxCode = profile.sriTaxCode ?: rate?.sriTaxCode,
                sriRateCode = profile.sriRateCode ?: rate?.sriRateCode,
                legalBasis = profile.legalBasis,
                effectiveFrom = profile.effectiveFrom,
                effectiveTo = profile.effectiveTo,
                source = profile.source,
                capturedAt = capturedAt,
                profileVersion = profile.version,
                rateVersion = rate?.version,
            )
        }
    }
}
