package com.hermes.domain.electronicinvoicing

enum class SriErrorCategory {
    XSD_VALIDATION_ERROR,
    SIGNATURE_ERROR,
    SRI_RECEPTION_RETURNED,
    SRI_AUTHORIZATION_REJECTED,
    SRI_PROCESSING,
    SRI_TIMEOUT,
    SRI_UNAVAILABLE,
    DELIVERY_ERROR,
    UNKNOWN
}

enum class SriErrorRecoverability {
    CORRECT_DATA_AND_RETRY_SAME_ACCESS_KEY,
    RETRY_AUTHORIZATION_QUERY_ONLY,
    RETRY_TECHNICAL,
    CONFIGURATION_REQUIRED,
    NO_AUTOMATIC_RETRY,
    UNKNOWN
}

data class SriErrorClassification(
    val category: SriErrorCategory,
    val recoverability: SriErrorRecoverability,
    val userActionRequired: Boolean,
    val shouldKeepSameAccessKey: Boolean,
    val reason: String,
)

data class SriErrorClassificationInput(
    val receptionStatus: SriReceptionStatus? = null,
    val authorizationStatus: SriAuthorizationStatus? = null,
    val messages: List<SriMessage> = emptyList(),
    val technicalCause: String? = null,
)

object SriErrorClassifier {
    fun classify(input: SriErrorClassificationInput): SriErrorClassification {
        val technical = input.technicalCause?.trim()?.lowercase().orEmpty()

        if (technical.contains("timeout") || technical.contains("timed out")) {
            return SriErrorClassification(
                category = SriErrorCategory.SRI_TIMEOUT,
                recoverability = SriErrorRecoverability.RETRY_TECHNICAL,
                userActionRequired = false,
                shouldKeepSameAccessKey = true,
                reason = "Technical timeout while communicating with SRI.",
            )
        }

        if (
            technical.contains("unavailable") ||
            technical.contains("connection refused") ||
            technical.contains("503") ||
            technical.contains("502")
        ) {
            return SriErrorClassification(
                category = SriErrorCategory.SRI_UNAVAILABLE,
                recoverability = SriErrorRecoverability.RETRY_TECHNICAL,
                userActionRequired = false,
                shouldKeepSameAccessKey = true,
                reason = "SRI service is unavailable or unreachable.",
            )
        }

        if (technical.contains("signature") || technical.contains("certificat") || technical.contains("pkcs12")) {
            return SriErrorClassification(
                category = SriErrorCategory.SIGNATURE_ERROR,
                recoverability = SriErrorRecoverability.CONFIGURATION_REQUIRED,
                userActionRequired = true,
                shouldKeepSameAccessKey = true,
                reason = "Electronic signature configuration must be reviewed.",
            )
        }

        if (input.authorizationStatus == SriAuthorizationStatus.PROCESSING) {
            return SriErrorClassification(
                category = SriErrorCategory.SRI_PROCESSING,
                recoverability = SriErrorRecoverability.RETRY_AUTHORIZATION_QUERY_ONLY,
                userActionRequired = false,
                shouldKeepSameAccessKey = true,
                reason = "SRI authorization is still processing.",
            )
        }

        if (input.authorizationStatus == SriAuthorizationStatus.NOT_AUTHORIZED) {
            return SriErrorClassification(
                category = SriErrorCategory.SRI_AUTHORIZATION_REJECTED,
                recoverability = SriErrorRecoverability.CORRECT_DATA_AND_RETRY_SAME_ACCESS_KEY,
                userActionRequired = true,
                shouldKeepSameAccessKey = true,
                reason = input.messages.firstOrNull()?.message ?: "SRI did not authorize the document.",
            )
        }

        if (input.receptionStatus == SriReceptionStatus.RETURNED) {
            return SriErrorClassification(
                category = SriErrorCategory.SRI_RECEPTION_RETURNED,
                recoverability = SriErrorRecoverability.CORRECT_DATA_AND_RETRY_SAME_ACCESS_KEY,
                userActionRequired = true,
                shouldKeepSameAccessKey = true,
                reason = input.messages.firstOrNull()?.message ?: "SRI returned the document at reception.",
            )
        }

        val firstError = input.messages.firstOrNull { it.isError }
        if (firstError != null) {
            return SriErrorClassification(
                category = SriErrorCategory.UNKNOWN,
                recoverability = SriErrorRecoverability.UNKNOWN,
                userActionRequired = true,
                shouldKeepSameAccessKey = true,
                reason = firstError.message,
            )
        }

        return SriErrorClassification(
            category = SriErrorCategory.UNKNOWN,
            recoverability = SriErrorRecoverability.UNKNOWN,
            userActionRequired = true,
            shouldKeepSameAccessKey = true,
            reason = "Unable to classify SRI error.",
        )
    }
}
