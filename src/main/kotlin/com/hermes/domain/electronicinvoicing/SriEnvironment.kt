package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriEnvironment(
    val code: String,
    val storageValue: String,
    val displayName: String,
) {
    TEST("1", "test", "Pruebas"),
    PRODUCTION("2", "production", "Producción");

    val isProduction: Boolean get() = this == PRODUCTION
    val isTest: Boolean get() = this == TEST

    companion object {
        fun fromCode(code: String): SriEnvironment {
            val normalized = code.trim()
            return entries.firstOrNull { it.code == normalized }
                ?: throw DomainRuleViolation("Unknown SRI environment code: $code.")
        }

        fun fromStorage(value: String): SriEnvironment {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI environment: $value.")
        }
    }
}
