package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation

data class TaxProfile(
    val id: String,
    val code: String,
    val displayName: String,
    val treatment: TaxTreatment,
    val taxName: String,
    val rate: TaxRate?,
    val legalBasis: String,
    val status: TaxProfileStatus = TaxProfileStatus.ACTIVE,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Tax profile id cannot be blank.")
        if (code.isBlank()) throw DomainRuleViolation("Tax profile code cannot be blank.")
        if (displayName.isBlank()) throw DomainRuleViolation("Tax profile display name cannot be blank.")
        if (taxName.isBlank()) throw DomainRuleViolation("Tax profile tax name cannot be blank.")
        if (legalBasis.isBlank()) throw DomainRuleViolation("Tax profile legal basis cannot be blank.")
        if (treatment in setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL, TaxTreatment.IVA_ZERO) && rate == null) {
            throw DomainRuleViolation("IVA tax profiles require a tax rate.")
        }
    }

    val isTaxable: Boolean
        get() = treatment in setOf(TaxTreatment.IVA_FULL, TaxTreatment.IVA_REDUCED_OR_SPECIAL)

    companion object {
        fun ivaFull(rate: TaxRate = TaxRate.iva15()): TaxProfile = TaxProfile(
            id = "taxp_iva_full_current",
            code = "iva_current_full",
            displayName = "IVA tarifa vigente",
            treatment = TaxTreatment.IVA_FULL,
            taxName = "IVA",
            rate = rate,
            legalBasis = "SRI vigente al momento de emisión",
        )

        fun ivaZero(rate: TaxRate = TaxRate.iva0()): TaxProfile = TaxProfile(
            id = "taxp_iva_0",
            code = "iva_0",
            displayName = "IVA 0%",
            treatment = TaxTreatment.IVA_ZERO,
            taxName = "IVA",
            rate = rate,
            legalBasis = "SRI vigente al momento de emisión",
        )

        fun exemptIva(): TaxProfile = TaxProfile(
            id = "taxp_exempt_iva",
            code = "exempt_iva",
            displayName = "Exento de IVA",
            treatment = TaxTreatment.EXEMPT_IVA,
            taxName = "IVA",
            rate = null,
            legalBasis = "Exento según configuración tributaria vigente",
        )

        fun notSubjectToIva(): TaxProfile = TaxProfile(
            id = "taxp_not_subject_to_iva",
            code = "not_subject_to_iva",
            displayName = "No objeto de IVA",
            treatment = TaxTreatment.NOT_SUBJECT_TO_IVA,
            taxName = "IVA",
            rate = null,
            legalBasis = "No objeto según configuración tributaria vigente",
        )
    }
}
