package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriMessageType {
    INFO,
    WARNING,
    ERROR;

    companion object {
        fun fromRaw(value: String?): SriMessageType {
            val normalized = value?.trim()?.uppercase().orEmpty()
            return when (normalized) {
                "", "INFO", "INFORMATIVO" -> INFO
                "WARNING", "WARN", "ADVERTENCIA" -> WARNING
                "ERROR" -> ERROR
                else -> ERROR
            }
        }
    }
}

data class SriMessage(
    val identifier: String?,
    val message: String,
    val additionalInfo: String? = null,
    val type: SriMessageType = SriMessageType.ERROR,
) {
    init {
        if (message.isBlank()) {
            throw DomainRuleViolation("SRI message cannot be blank.")
        }
        identifier?.let {
            if (it.isBlank()) {
                throw DomainRuleViolation("SRI message identifier cannot be blank when provided.")
            }
        }
    }

    val isError: Boolean get() = type == SriMessageType.ERROR

    companion object {
        fun error(
            identifier: String? = null,
            message: String,
            additionalInfo: String? = null,
        ): SriMessage = SriMessage(
            identifier = identifier?.trim()?.takeIf { it.isNotBlank() },
            message = message.trim(),
            additionalInfo = additionalInfo?.trim()?.takeIf { it.isNotBlank() },
            type = SriMessageType.ERROR,
        )

        fun fromRaw(
            identifier: String?,
            message: String,
            additionalInfo: String?,
            type: String?,
        ): SriMessage = SriMessage(
            identifier = identifier?.trim()?.takeIf { it.isNotBlank() },
            message = message.trim(),
            additionalInfo = additionalInfo?.trim()?.takeIf { it.isNotBlank() },
            type = SriMessageType.fromRaw(type),
        )
    }
}
