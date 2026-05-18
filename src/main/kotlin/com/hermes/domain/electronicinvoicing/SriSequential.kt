package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

@JvmInline
value class SriSequential(val value: Int) {
    init {
        if (value !in MIN_VALUE..MAX_VALUE) {
            throw DomainRuleViolation("SRI sequential must be between $MIN_VALUE and $MAX_VALUE.")
        }
    }

    val formatted: String get() = value.toString().padStart(LENGTH, '0')

    override fun toString(): String = formatted

    companion object {
        const val LENGTH: Int = 9
        const val MIN_VALUE: Int = 1
        const val MAX_VALUE: Int = 999_999_999

        fun parse(value: String): SriSequential {
            val normalized = value.trim()
            if (!normalized.matches(Regex("\\d{1,9}"))) {
                throw DomainRuleViolation("SRI sequential must contain 1 to 9 digits.")
            }
            return SriSequential(normalized.toInt())
        }
    }
}
