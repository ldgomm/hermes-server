package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriEmissionType(
    val code: String,
    val storageValue: String,
    val displayName: String,
) {
    NORMAL("1", "normal", "Emisión normal");

    companion object {
        fun fromCode(code: String): SriEmissionType {
            val normalized = code.trim()
            return entries.firstOrNull { it.code == normalized }
                ?: throw DomainRuleViolation("Unknown SRI emission type code: $code.")
        }

        fun fromStorage(value: String): SriEmissionType {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI emission type: $value.")
        }
    }
}
