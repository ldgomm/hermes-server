package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

/**
 * Fase 11.10 — Homologación SRI.
 *
 * This layer is deliberately an application-level acceptance harness:
 * it does not know how to build a Sale, how to sign XML, or how to talk SOAP.
 * It verifies that the already wired Fase 11 flow can be executed safely in SRI TEST
 * and produces a deterministic report that can be used as a production gate.
 */
enum class ElectronicInvoiceHomologationScenarioCode(val requiredForMvpProductionGate: Boolean) {
    FINAL_CONSUMER(requiredForMvpProductionGate = true),
    NATIONAL_ID_BUYER(requiredForMvpProductionGate = true),
    RUC_BUYER(requiredForMvpProductionGate = true),
    VAT_CURRENT_RATE(requiredForMvpProductionGate = true),
    VAT_ZERO_RATE(requiredForMvpProductionGate = true),
    MIXED_VAT(requiredForMvpProductionGate = true),
    DISCOUNT(requiredForMvpProductionGate = true),
    XSD_REJECTION_CONTROLLED(requiredForMvpProductionGate = true),
    SRI_RECEPTION_RETURNED_CONTROLLED(requiredForMvpProductionGate = true),
    AUTHORIZATION_PROCESSING_RETRY(requiredForMvpProductionGate = true),
    EMAIL_DELIVERY_WITH_RIDE(requiredForMvpProductionGate = true),
    MANUAL_EXPLORATORY(requiredForMvpProductionGate = false);

    companion object {
        fun requiredForMvpProductionGate(): Set<ElectronicInvoiceHomologationScenarioCode> =
            entries.filter { it.requiredForMvpProductionGate }.toSet()
    }
}

enum class ElectronicInvoiceHomologationStepStatus {
    PASSED,
    FAILED,
    SKIPPED;

    val passed: Boolean get() = this == PASSED
}

data class ElectronicInvoiceHomologationEndpointConfig(
    val receptionWsdlUrl: String,
    val authorizationWsdlUrl: String,
) {
    init {
        if (receptionWsdlUrl.isBlank()) throw DomainRuleViolation("SRI reception WSDL URL is required for homologation.")
        if (authorizationWsdlUrl.isBlank()) throw DomainRuleViolation("SRI authorization WSDL URL is required for homologation.")
    }

    val looksLikeSriTest: Boolean
        get() = receptionWsdlUrl.contains("celcer.sri.gob.ec", ignoreCase = true) &&
                authorizationWsdlUrl.contains("celcer.sri.gob.ec", ignoreCase = true)

    val looksLikeSriProduction: Boolean
        get() = receptionWsdlUrl.contains("cel.sri.gob.ec", ignoreCase = true) ||
                authorizationWsdlUrl.contains("cel.sri.gob.ec", ignoreCase = true)
}

data class ElectronicInvoiceHomologationReadinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val environment: SriEnvironment,
    val issuerRuc: String,
    val series: SriSeries,
    val schemaVersionCode: String,
    val endpoints: ElectronicInvoiceHomologationEndpointConfig,
    val activeSignatureId: String?,
    val expectedScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode> =
        ElectronicInvoiceHomologationScenarioCode.requiredForMvpProductionGate(),
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required for SRI homologation readiness.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required for SRI homologation readiness.")
        if (!issuerRuc.matches(Regex("^[0-9]{13}$"))) throw DomainRuleViolation("Issuer RUC must contain exactly 13 digits.")
        if (schemaVersionCode.isBlank()) throw DomainRuleViolation("SRI invoice schema version is required.")
        if (expectedScenarioCodes.isEmpty()) throw DomainRuleViolation("At least one homologation scenario is required.")
    }
}

data class ElectronicInvoiceHomologationReadinessCheck(
    val id: String,
    val label: String,
    val status: ElectronicInvoiceHomologationStepStatus,
    val message: String,
)

data class ElectronicInvoiceHomologationReadinessResult(
    val organizationId: String,
    val environment: SriEnvironment,
    val checks: List<ElectronicInvoiceHomologationReadinessCheck>,
    val generatedAt: Instant,
) {
    val ready: Boolean get() = checks.all { it.status == ElectronicInvoiceHomologationStepStatus.PASSED }
    val failedChecks: List<ElectronicInvoiceHomologationReadinessCheck> get() = checks.filter { !it.status.passed }
}

data class ElectronicInvoiceIssueOutcome(
    val record: ElectronicInvoiceIssueRecord,
    val generatedXmlPresent: Boolean,
    val validationValid: Boolean?,
    val signedXmlPresent: Boolean,
    val receptionReceived: Boolean,
    val authorizationStatus: SriAuthorizationStatus?,
    val authorized: Boolean,
    val artifacts: List<StoredElectronicDocumentArtifact>,
)

data class ElectronicInvoiceAuthorizationRetryOutcome(
    val record: ElectronicInvoiceIssueRecord,
    val authorizationStatus: SriAuthorizationStatus,
    val authorized: Boolean,
    val artifacts: List<StoredElectronicDocumentArtifact>,
)

data class ElectronicInvoiceDeliveryOutcome(
    val record: ElectronicInvoiceIssueRecord,
    val delivered: Boolean,
)

data class ElectronicInvoiceHomologationScenarioCommand(
    val code: ElectronicInvoiceHomologationScenarioCode,
    val issueCommand: IssueElectronicInvoiceCommand,
    val expectedFinalStatus: ElectronicDocumentStatus,
    val expectAuthorized: Boolean = expectedFinalStatus in setOf(
        ElectronicDocumentStatus.AUTHORIZED,
        ElectronicDocumentStatus.DELIVERY_PENDING,
        ElectronicDocumentStatus.DELIVERED,
    ),
    val retryAuthorizationWhenPending: Boolean = false,
    val emailTo: String? = null,
    val actorEffectivePermissions: Set<String> = emptySet(),
    val notes: String? = null,
) {
    init {
        emailTo?.let {
            if (it.isBlank()) throw DomainRuleViolation("Homologation delivery email cannot be blank.")
            if (!it.contains("@")) throw DomainRuleViolation("Homologation delivery email is invalid.")
        }
    }
}

data class RunElectronicInvoiceHomologationCommand(
    val organizationId: String,
    val actorUserId: String,
    val environment: SriEnvironment,
    val scenarios: List<ElectronicInvoiceHomologationScenarioCommand>,
    val requiredScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode> =
        ElectronicInvoiceHomologationScenarioCode.requiredForMvpProductionGate(),
    val strict: Boolean = true,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to run SRI homologation.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to run SRI homologation.")
        if (scenarios.isEmpty()) throw DomainRuleViolation("At least one SRI homologation scenario is required.")
        if (requiredScenarioCodes.isEmpty()) throw DomainRuleViolation("At least one required SRI homologation scenario must be configured.")
    }
}

data class ElectronicInvoiceHomologationScenarioResult(
    val code: ElectronicInvoiceHomologationScenarioCode,
    val status: ElectronicInvoiceHomologationStepStatus,
    val documentId: String?,
    val saleId: String?,
    val finalDocumentStatus: ElectronicDocumentStatus?,
    val accessKey: String?,
    val authorized: Boolean,
    val delivered: Boolean,
    val artifactTypes: Set<ElectronicDocumentArtifactType>,
    val messages: List<String>,
    val startedAt: Instant,
    val finishedAt: Instant,
) {
    val passed: Boolean get() = status == ElectronicInvoiceHomologationStepStatus.PASSED
}

data class ElectronicInvoiceHomologationReport(
    val organizationId: String,
    val environment: SriEnvironment,
    val requiredScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode>,
    val scenarioResults: List<ElectronicInvoiceHomologationScenarioResult>,
    val startedAt: Instant,
    val finishedAt: Instant,
) {
    val executedScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode>
        get() = scenarioResults.map { it.code }.toSet()

    val missingRequiredScenarioCodes: Set<ElectronicInvoiceHomologationScenarioCode>
        get() = requiredScenarioCodes - executedScenarioCodes

    val failedScenarioResults: List<ElectronicInvoiceHomologationScenarioResult>
        get() = scenarioResults.filterNot { it.passed }

    val passed: Boolean get() = missingRequiredScenarioCodes.isEmpty() && failedScenarioResults.isEmpty()
}

data class ElectronicInvoiceProductionReadinessDecision(
    val approved: Boolean,
    val environment: SriEnvironment,
    val reasons: List<String>,
    val decidedAt: Instant,
)
