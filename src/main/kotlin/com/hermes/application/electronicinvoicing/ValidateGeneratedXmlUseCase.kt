package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.shared.DomainRuleViolation

class ValidateGeneratedXmlUseCase(
    private val validator: SriXsdValidator,
) {
    fun execute(command: ValidateGeneratedXmlCommand): ValidateGeneratedXmlResult {
        command.currentStatus.assertCanTransitionTo(ElectronicDocumentStatus.XSD_VALIDATED)

        val validation = validator.validate(
            xml = command.generatedXml.bytes,
            schemaVersionCode = command.generatedXml.schemaVersion.schemaVersionCode,
        )
        val targetStatus = validation.targetStatus
        command.currentStatus.assertCanTransitionTo(targetStatus)

        return ValidateGeneratedXmlResult(
            documentId = command.documentId,
            schemaVersionCode = validation.schemaVersionCode,
            valid = validation.valid,
            targetStatus = targetStatus,
            errors = validation.errors,
            warnings = validation.warnings,
            xmlSha256 = command.generatedXml.sha256,
        )
    }
}

data class ValidateGeneratedXmlCommand(
    val generatedXml: GeneratedXml,
    val documentId: String? = null,
    val currentStatus: ElectronicDocumentStatus = ElectronicDocumentStatus.XML_GENERATED,
) {
    init {
        documentId?.let {
            if (it.isBlank()) throw DomainRuleViolation("Electronic document id cannot be blank.")
        }
    }
}

data class ValidateGeneratedXmlResult(
    val documentId: String?,
    val schemaVersionCode: String,
    val valid: Boolean,
    val targetStatus: ElectronicDocumentStatus,
    val errors: List<XsdValidationError>,
    val warnings: List<XsdValidationError>,
    val xmlSha256: String,
)
