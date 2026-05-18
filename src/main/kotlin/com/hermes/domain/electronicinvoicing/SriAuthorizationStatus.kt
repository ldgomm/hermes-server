package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriAuthorizationStatus(
    val storageValue: String,
) {
    AUTHORIZED("authorized"),
    NOT_AUTHORIZED("not_authorized"),
    PROCESSING("processing");

    val isTerminal: Boolean get() = this != PROCESSING
    val isSuccessful: Boolean get() = this == AUTHORIZED

    companion object {
        fun fromSriValue(value: String): SriAuthorizationStatus {
            val normalized = value.trim().uppercase().replace("-", " ").replace("_", " ")
            return when (normalized) {
                "AUT", "AUTORIZADO", "AUTHORIZED" -> AUTHORIZED
                "NAT", "NO AUTORIZADO", "NOAUTORIZADO", "RECHAZADO", "REJECTED", "NOT AUTHORIZED" -> NOT_AUTHORIZED
                "PPR", "PROCESAMIENTO", "EN PROCESAMIENTO", "PROCESSING" -> PROCESSING
                else -> throw DomainRuleViolation("Unknown SRI authorization status: $value.")
            }
        }

        fun fromStorage(value: String): SriAuthorizationStatus {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI authorization status storage value: $value.")
        }
    }
}
