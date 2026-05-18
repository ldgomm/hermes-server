package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Result of local XSD validation before XML signature and SRI submission.
 */
data class XsdValidationResult(
    val schemaVersionCode: String,
    val valid: Boolean,
    val errors: List<XsdValidationError> = emptyList(),
    val warnings: List<XsdValidationError> = emptyList(),
) {
    init {
        if (schemaVersionCode.isBlank()) throw DomainRuleViolation("XSD schema version code cannot be blank.")
        if (valid && errors.isNotEmpty()) throw DomainRuleViolation("A valid XSD result cannot contain errors.")
        if (!valid && errors.isEmpty()) throw DomainRuleViolation("An invalid XSD result requires at least one error.")
    }

    val targetStatus: ElectronicDocumentStatus
        get() = if (valid) ElectronicDocumentStatus.XSD_VALIDATED else ElectronicDocumentStatus.XSD_INVALID

    companion object {
        fun valid(schemaVersionCode: String, warnings: List<XsdValidationError> = emptyList()): XsdValidationResult =
            XsdValidationResult(
                schemaVersionCode = schemaVersionCode.trim(),
                valid = true,
                warnings = warnings,
            )

        fun invalid(schemaVersionCode: String, errors: List<XsdValidationError>, warnings: List<XsdValidationError> = emptyList()): XsdValidationResult =
            XsdValidationResult(
                schemaVersionCode = schemaVersionCode.trim(),
                valid = false,
                errors = errors,
                warnings = warnings,
            )
    }
}

data class XsdValidationError(
    val severity: XsdValidationSeverity,
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val publicId: String? = null,
    val systemId: String? = null,
) {
    init {
        if (message.isBlank()) throw DomainRuleViolation("XSD validation error message cannot be blank.")
        if (line != null && line < 1) throw DomainRuleViolation("XSD validation error line must be positive.")
        if (column != null && column < 1) throw DomainRuleViolation("XSD validation error column must be positive.")
    }

    val location: String?
        get() = when {
            line != null && column != null -> "$line:$column"
            line != null -> line.toString()
            else -> null
        }
}

enum class XsdValidationSeverity {
    WARNING,
    ERROR,
    FATAL,
}
