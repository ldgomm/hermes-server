package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class TaxRate(
    val id: String,
    val code: String,
    val name: String,
    val kind: TaxKind,
    val rate: BigDecimal,
    val status: TaxRateStatus,
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
        if (id.isBlank()) throw DomainRuleViolation("Tax rate id cannot be blank.")
        if (!CODE_PATTERN.matches(code)) throw DomainRuleViolation("Tax rate code has invalid format: $code.")
        if (name.isBlank()) throw DomainRuleViolation("Tax rate name cannot be blank.")
        if (rate.scale() != RATE_SCALE) throw DomainRuleViolation("Tax rate must use exactly $RATE_SCALE decimal places.")
        if (rate < BigDecimal.ZERO) throw DomainRuleViolation("Tax rate cannot be negative.")
        if (rate > BigDecimal("100.0000")) throw DomainRuleViolation("Tax rate cannot be greater than 100.")
        if (legalBasis.isBlank()) throw DomainRuleViolation("Tax rate legal basis cannot be blank.")
        if (effectiveTo != null && !effectiveFrom.isBefore(effectiveTo)) {
            throw DomainRuleViolation("Tax rate effectiveFrom must be before effectiveTo.")
        }
        if (sriTaxCode != null && sriTaxCode.isBlank()) throw DomainRuleViolation("SRI tax code cannot be blank.")
        if (sriRateCode != null && sriRateCode.isBlank()) throw DomainRuleViolation("SRI rate code cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Tax rate version must be positive.")
        if (schemaVersion < 1) throw DomainRuleViolation("Tax rate schemaVersion must be positive.")
    }

    fun isEffectiveAt(moment: Instant): Boolean {
        val starts = !moment.isBefore(effectiveFrom)
        val notEnded = effectiveTo?.let { moment.isBefore(it) } ?: true
        return starts && notEnded
    }

    fun assertUsableAt(moment: Instant) {
        if (status != TaxRateStatus.ACTIVE) {
            throw DomainRuleViolation("Tax rate $code cannot be used from status $status.")
        }
        if (!isEffectiveAt(moment)) {
            throw DomainRuleViolation("Tax rate $code is not effective at $moment.")
        }
    }

    fun fraction(scale: Int = FRACTION_SCALE): BigDecimal =
        rate.divide(BigDecimal("100"), scale, RoundingMode.HALF_UP)

    fun depreciate(now: Instant, effectiveTo: Instant = now): TaxRate {
        if (status == TaxRateStatus.ARCHIVED) throw DomainRuleViolation("Archived tax rate cannot be deprecated.")
        if (effectiveTo.isBefore(effectiveFrom)) throw DomainRuleViolation("Tax rate effectiveTo cannot be before effectiveFrom.")
        return copy(
            status = TaxRateStatus.DEPRECATED,
            effectiveTo = effectiveTo,
            updatedAt = now,
            version = version + 1,
        )
    }

    companion object {
        const val RATE_SCALE = 4
        private const val FRACTION_SCALE = 8
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_]*$")

        fun of(
            id: String,
            code: String,
            name: String,
            kind: TaxKind = TaxKind.IVA,
            rate: String,
            status: TaxRateStatus = TaxRateStatus.ACTIVE,
            sriTaxCode: String? = null,
            sriRateCode: String? = null,
            legalBasis: String,
            effectiveFrom: Instant,
            effectiveTo: Instant? = null,
            source: TaxSource = TaxSource.SYSTEM_SEED,
            now: Instant = Instant.now(),
        ): TaxRate = TaxRate(
            id = id,
            code = code,
            name = name,
            kind = kind,
            rate = BigDecimal(rate).setScale(RATE_SCALE, RoundingMode.HALF_UP),
            status = status,
            sriTaxCode = sriTaxCode?.trim()?.takeIf { it.isNotBlank() },
            sriRateCode = sriRateCode?.trim()?.takeIf { it.isNotBlank() },
            legalBasis = legalBasis.trim(),
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
            source = source,
            createdAt = now,
            updatedAt = now,
        )
    }
}
