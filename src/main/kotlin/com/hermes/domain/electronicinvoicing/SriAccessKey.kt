package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

@JvmInline
value class SriAccessKey(val value: String) {
    init {
        if (!value.matches(Regex("\\d{49}"))) {
            throw DomainRuleViolation("SRI access key must contain exactly 49 digits.")
        }
        if (!Modulo11Calculator.verify(value)) {
            throw DomainRuleViolation("SRI access key has an invalid modulo 11 check digit.")
        }
    }

    val issuedDateDdmmyyyy: String get() = value.substring(0, 8)
    val documentType: SriDocumentType get() = SriDocumentType.fromCode(value.substring(8, 10))
    val ruc: String get() = value.substring(10, 23)
    val environment: SriEnvironment get() = SriEnvironment.fromCode(value.substring(23, 24))
    val series: SriSeries get() = SriSeries.parse(value.substring(24, 30))
    val sequential: SriSequential get() = SriSequential.parse(value.substring(30, 39))
    val numericCode: SriNumericCode get() = SriNumericCode(value.substring(39, 47))
    val emissionType: SriEmissionType get() = SriEmissionType.fromCode(value.substring(47, 48))
    val checkDigit: Int get() = value.substring(48, 49).toInt()

    override fun toString(): String = value

    companion object {
        const val LENGTH: Int = 49
        const val BODY_LENGTH: Int = 48

        fun unsafe(value: String): SriAccessKey = SriAccessKey(value.trim())
    }
}
