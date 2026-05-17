package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class TaxProfile(
    val id: String,
    val code: String,
    val name: String,
    val treatment: TaxTreatment,
    val status: TaxProfileStatus,
    val taxRate: TaxRate?,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: Instant,
    val effectiveTo: Instant? = null,
    val source: TaxSource = TaxSource.SYSTEM_SEED,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 1,
    val schemaVersion: Int = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Tax profile id cannot be blank.")
        if (!CODE_PATTERN.matches(code)) throw DomainRuleViolation("Tax profile code has invalid format: $code.")
        if (name.isBlank()) throw DomainRuleViolation("Tax profile name cannot be blank.")
        if (legalBasis.isBlank()) throw DomainRuleViolation("Tax profile legal basis cannot be blank.")
        if (effectiveTo != null && !effectiveFrom.isBefore(effectiveTo)) {
            throw DomainRuleViolation("Tax profile effectiveFrom must be before effectiveTo.")
        }
        if (version < 1) throw DomainRuleViolation("Tax profile version must be positive.")
        if (schemaVersion < 1) throw DomainRuleViolation("Tax profile schemaVersion must be positive.")

        when (treatment) {
            TaxTreatment.IVA_FULL,
            TaxTreatment.IVA_REDUCED_OR_SPECIAL -> {
                if (taxRate == null) throw DomainRuleViolation("Taxed IVA profile requires a tax rate.")
                if (taxRate.rate.signum() <= 0) throw DomainRuleViolation("Taxed IVA profile requires a positive tax rate.")
            }

            TaxTreatment.IVA_ZERO -> {
                if (taxRate == null) throw DomainRuleViolation("IVA zero profile requires a zero tax rate.")
                if (taxRate.rate.signum() != 0) throw DomainRuleViolation("IVA zero profile must use a zero tax rate.")
            }

            TaxTreatment.EXEMPT_IVA,
            TaxTreatment.NOT_SUBJECT_TO_IVA,
            TaxTreatment.NO_TAX_INTERNAL -> {
                if (taxRate != null && taxRate.rate.signum() != 0) {
                    throw DomainRuleViolation("Non-taxed profiles cannot use a positive tax rate.")
                }
            }
        }
    }

    val isTaxedWithPositiveRate: Boolean
        get() = treatment in setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL)

    fun isEffectiveAt(moment: Instant): Boolean {
        val starts = !moment.isBefore(effectiveFrom)
        val notEnded = effectiveTo?.let { moment.isBefore(it) } ?: true
        return starts && notEnded
    }

    fun assertUsableAt(moment: Instant, forEmission: Boolean = false) {
        if (status != TaxProfileStatus.ACTIVE) {
            throw DomainRuleViolation("Tax profile $code cannot be used from status $status.")
        }
        if (!isEffectiveAt(moment)) {
            throw DomainRuleViolation("Tax profile $code is not effective at $moment.")
        }
        taxRate?.assertUsableAt(moment)

        if (forEmission) {
            if (treatment == TaxTreatment.NO_TAX_INTERNAL) {
                throw DomainRuleViolation("Internal no-tax profile cannot be used for electronic emission.")
            }
            if (sriTaxCode.isNullOrBlank() && taxRate?.sriTaxCode.isNullOrBlank()) {
                throw DomainRuleViolation("Tax profile $code requires SRI tax code for emission.")
            }
            if (sriRateCode.isNullOrBlank() && taxRate?.sriRateCode.isNullOrBlank()) {
                throw DomainRuleViolation("Tax profile $code requires SRI rate code for emission.")
            }
        }
    }

    fun snapshot(moment: Instant, forEmission: Boolean = false): TaxProfileSnapshot {
        assertUsableAt(moment, forEmission)
        return TaxProfileSnapshot.from(this, moment)
    }

    companion object {
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_]*$")
    }
}
