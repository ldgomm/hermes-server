package com.hermes.application.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

data class GenerateElectronicInvoiceRideCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val forceRegenerate: Boolean = false,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to generate RIDE.")
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic document id is required to generate RIDE.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to generate RIDE.")
    }
}

data class GenerateElectronicInvoiceRideResult(
    val record: ElectronicInvoiceIssueRecord,
    val ridePdf: ElectronicDocumentArtifactFile,
)

data class EmailElectronicInvoiceCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val emailTo: String,
    val subject: String? = null,
    val message: String? = null,
    val forceRegenerateRide: Boolean = false,
    val allowResend: Boolean = true,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to email electronic invoice.")
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic document id is required to email electronic invoice.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to email electronic invoice.")
        if (emailTo.isBlank()) throw DomainRuleViolation("Electronic invoice email recipient is required.")
    }
}

data class EmailElectronicInvoiceResult(
    val record: ElectronicInvoiceIssueRecord,
    val delivered: Boolean,
    val ridePdf: ElectronicDocumentArtifactFile,
    val authorizedXml: ElectronicDocumentArtifactFile,
)
