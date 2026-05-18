package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

object Modulo11Calculator {
    private val weights = intArrayOf(2, 3, 4, 5, 6, 7)

    fun calculateCheckDigit(digits: String): Int {
        val normalized = digits.trim()
        if (normalized.isBlank()) {
            throw DomainRuleViolation("Modulo 11 input cannot be blank.")
        }
        if (!normalized.matches(Regex("\\d+"))) {
            throw DomainRuleViolation("Modulo 11 input must contain only digits.")
        }

        val total = normalized
            .reversed()
            .mapIndexed { index, char ->
                char.digitToInt() * weights[index % weights.size]
            }
            .sum()

        return when (val candidate = 11 - (total % 11)) {
            11 -> 0
            10 -> 1
            else -> candidate
        }
    }

    fun verify(fullNumber: String): Boolean {
        val normalized = fullNumber.trim()
        if (!normalized.matches(Regex("\\d{2,}"))) return false

        val body = normalized.dropLast(1)
        val expected = normalized.last().digitToInt()

        return calculateCheckDigit(body) == expected
    }

    fun assertValid(fullNumber: String) {
        if (!verify(fullNumber)) {
            throw DomainRuleViolation("Modulo 11 check digit is invalid.")
        }
    }
}
