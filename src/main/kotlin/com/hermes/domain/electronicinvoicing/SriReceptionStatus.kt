package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriReceptionStatus(
    val sriValue: String,
    val storageValue: String,
) {
    RECEIVED("RECIBIDA", "received"),
    RETURNED("DEVUELTA", "returned");

    val canQueryAuthorization: Boolean get() = this == RECEIVED

    companion object {
        fun fromSriValue(value: String): SriReceptionStatus {
            val normalized = value.trim().uppercase()
            return entries.firstOrNull { it.sriValue == normalized || it.storageValue.uppercase() == normalized }
                ?: throw DomainRuleViolation("Unknown SRI reception status: $value.")
        }

        fun fromStorage(value: String): SriReceptionStatus {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI reception status storage value: $value.")
        }
    }
}
