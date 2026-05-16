package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.LocalDate

data class TaxRate(
    val id: String,
    val name: String,
    val ratePercent: BigDecimal,
    val sriTaxCode: String,
    val sriRateCode: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null,
    val status: TaxRateStatus = TaxRateStatus.ACTIVE,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Tax rate id cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Tax rate name cannot be blank.")
        if (ratePercent < BigDecimal.ZERO) throw DomainRuleViolation("Tax rate percentage cannot be negative.")
        if (sriTaxCode.isBlank()) throw DomainRuleViolation("SRI tax code cannot be blank.")
        if (sriRateCode.isBlank()) throw DomainRuleViolation("SRI rate code cannot be blank.")
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw DomainRuleViolation("Tax rate effectiveTo cannot be before effectiveFrom.")
        }
    }

    fun isEffectiveOn(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo))

    companion object {
        fun iva15(effectiveFrom: LocalDate = LocalDate.of(2024, 4, 1)): TaxRate = TaxRate(
            id = "taxr_iva_15",
            name = "IVA 15%",
            ratePercent = BigDecimal("15.00"),
            sriTaxCode = "2",
            sriRateCode = "4",
            effectiveFrom = effectiveFrom,
        )

        fun iva0(effectiveFrom: LocalDate = LocalDate.of(2020, 1, 1)): TaxRate = TaxRate(
            id = "taxr_iva_0",
            name = "IVA 0%",
            ratePercent = BigDecimal("0.00"),
            sriTaxCode = "2",
            sriRateCode = "0",
            effectiveFrom = effectiveFrom,
        )
    }
}
