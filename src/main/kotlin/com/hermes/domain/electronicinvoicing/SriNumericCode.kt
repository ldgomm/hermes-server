package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.security.SecureRandom

@JvmInline
value class SriNumericCode(val value: String) {
    init {
        if (!value.matches(Regex("\\d{8}"))) {
            throw DomainRuleViolation("SRI numeric code must contain exactly 8 digits.")
        }
    }

    override fun toString(): String = value

    companion object {
        fun fromInt(value: Int): SriNumericCode {
            if (value !in 0..99_999_999) {
                throw DomainRuleViolation("SRI numeric code integer must be between 0 and 99999999.")
            }
            return SriNumericCode(value.toString().padStart(8, '0'))
        }

        fun random(random: SecureRandom = SecureRandom()): SriNumericCode =
            fromInt(random.nextInt(100_000_000))
    }
}
