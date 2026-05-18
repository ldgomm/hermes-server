package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class SriIdentificationType(
    val code: String,
    val storageValue: String,
    val displayName: String,
) {
    RUC("04", "ruc", "RUC"),
    CEDULA("05", "cedula", "Cédula"),
    PASSPORT("06", "passport", "Pasaporte"),
    FINAL_CONSUMER("07", "final_consumer", "Consumidor final"),
    EXTERNAL_IDENTIFICATION("08", "external_identification", "Identificación del exterior");

    fun assertValidFor(
        identification: String,
        documentType: SriDocumentType = SriDocumentType.INVOICE,
    ) {
        val normalized = identification.trim()

        when (this) {
            RUC -> {
                if (!normalized.matches(Regex("\\d{13}"))) {
                    throw DomainRuleViolation("Buyer RUC must contain exactly 13 digits.")
                }
                if (!normalized.endsWith("001")) {
                    throw DomainRuleViolation("Buyer RUC must end with establishment suffix 001.")
                }
            }

            CEDULA -> {
                if (!normalized.matches(Regex("\\d{10}"))) {
                    throw DomainRuleViolation("Buyer cedula must contain exactly 10 digits.")
                }
            }

            PASSPORT -> {
                if (normalized.length !in 1..20) {
                    throw DomainRuleViolation("Buyer passport must contain between 1 and 20 characters.")
                }
            }

            FINAL_CONSUMER -> {
                if (!documentType.supportsFinalConsumer) {
                    throw DomainRuleViolation("Final consumer is not allowed for SRI document type ${documentType.code}.")
                }
                if (normalized != FINAL_CONSUMER_IDENTIFICATION) {
                    throw DomainRuleViolation("Final consumer identification must be $FINAL_CONSUMER_IDENTIFICATION.")
                }
            }

            EXTERNAL_IDENTIFICATION -> {
                if (normalized.length !in 1..20) {
                    throw DomainRuleViolation("External identification must contain between 1 and 20 characters.")
                }
            }
        }
    }

    companion object {
        const val FINAL_CONSUMER_IDENTIFICATION: String = "9999999999999"

        fun fromCode(code: String): SriIdentificationType {
            val normalized = code.trim()
            return entries.firstOrNull { it.code == normalized }
                ?: throw DomainRuleViolation("Unknown SRI identification type code: $code.")
        }

        fun fromStorage(value: String): SriIdentificationType {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown SRI identification type: $value.")
        }

        fun inferBasic(identification: String): SriIdentificationType {
            val normalized = identification.trim()
            return when {
                normalized == FINAL_CONSUMER_IDENTIFICATION -> FINAL_CONSUMER
                normalized.matches(Regex("\\d{13}")) -> RUC
                normalized.matches(Regex("\\d{10}")) -> CEDULA
                else -> PASSPORT
            }
        }
    }
}
