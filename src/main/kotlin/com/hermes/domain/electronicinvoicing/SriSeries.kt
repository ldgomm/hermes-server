package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

data class SriSeries(
    val establishmentCode: String,
    val emissionPointCode: String,
) {
    init {
        if (!establishmentCode.matches(Regex("\\d{3}"))) {
            throw DomainRuleViolation("SRI establishment code must contain exactly 3 digits.")
        }
        if (!emissionPointCode.matches(Regex("\\d{3}"))) {
            throw DomainRuleViolation("SRI emission point code must contain exactly 3 digits.")
        }
    }

    val value: String get() = establishmentCode + emissionPointCode
    val displayValue: String get() = "$establishmentCode-$emissionPointCode"

    override fun toString(): String = displayValue

    companion object {
        fun parse(value: String): SriSeries {
            val normalized = value.trim().replace("-", "")
            if (!normalized.matches(Regex("\\d{6}"))) {
                throw DomainRuleViolation("SRI series must contain establishment and emission point codes.")
            }
            return SriSeries(
                establishmentCode = normalized.substring(0, 3),
                emissionPointCode = normalized.substring(3, 6),
            )
        }
    }
}
