package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CheckElectronicInvoiceHomologationReadinessUseCase(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ElectronicInvoiceHomologationReadinessCommand): ElectronicInvoiceHomologationReadinessResult {
        val checks = buildList {
            add(
                check(
                    id = "environment_is_test",
                    label = "Ambiente SRI de pruebas",
                    passed = command.environment == SriEnvironment.TEST,
                    passedMessage = "La homologación se ejecutará contra ambiente de pruebas.",
                    failedMessage = "La homologación nunca debe ejecutarse contra producción.",
                )
            )
            add(
                check(
                    id = "endpoints_are_test",
                    label = "WSDL de certificación",
                    passed = command.endpoints.looksLikeSriTest && !command.endpoints.looksLikeSriProduction,
                    passedMessage = "Los endpoints apuntan a celcer.sri.gob.ec.",
                    failedMessage = "Los endpoints no parecen ser de pruebas o contienen URL de producción.",
                )
            )
            add(
                check(
                    id = "schema_version",
                    label = "Versión XSD factura",
                    passed = command.schemaVersionCode == "2.1.0",
                    passedMessage = "Factura configurada con schemaVersionCode=2.1.0.",
                    failedMessage = "Para el MVP recomendamos homologar factura con schemaVersionCode=2.1.0.",
                )
            )
            add(
                check(
                    id = "signature_available",
                    label = "Firma electrónica activa",
                    passed = !command.activeSignatureId.isNullOrBlank(),
                    passedMessage = "Existe una firma activa para pruebas.",
                    failedMessage = "No hay firma activa; no se puede validar XAdES_BES end-to-end.",
                )
            )
            add(
                check(
                    id = "series_format",
                    label = "Serie establecimiento/punto emisión",
                    passed = command.series.establishmentCode.length == 3 && command.series.emissionPointCode.length == 3,
                    passedMessage = "Serie SRI válida para homologación.",
                    failedMessage = "La serie SRI debe tener establecimiento y punto de emisión de 3 dígitos.",
                )
            )
            add(
                check(
                    id = "required_scenarios",
                    label = "Escenarios obligatorios",
                    passed = command.expectedScenarioCodes.containsAll(
                        ElectronicInvoiceHomologationScenarioCode.requiredForMvpProductionGate()
                    ),
                    passedMessage = "El set de homologación contiene todos los escenarios obligatorios del MVP.",
                    failedMessage = "Faltan escenarios obligatorios para cerrar homologación completa.",
                )
            )
        }

        return ElectronicInvoiceHomologationReadinessResult(
            organizationId = command.organizationId,
            environment = command.environment,
            checks = checks,
            generatedAt = Instant.now(clock),
        )
    }

    private fun check(
        id: String,
        label: String,
        passed: Boolean,
        passedMessage: String,
        failedMessage: String,
    ): ElectronicInvoiceHomologationReadinessCheck = ElectronicInvoiceHomologationReadinessCheck(
        id = id,
        label = label,
        status = if (passed) ElectronicInvoiceHomologationStepStatus.PASSED else ElectronicInvoiceHomologationStepStatus.FAILED,
        message = if (passed) passedMessage else failedMessage,
    )
}

class RunElectronicInvoiceHomologationUseCase(
    private val issueRunner: ElectronicInvoiceIssueRunner,
    private val retryAuthorizationRunner: ElectronicInvoiceAuthorizationRetryRunner? = null,
    private val deliveryRunner: ElectronicInvoiceDeliveryRunner? = null,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RunElectronicInvoiceHomologationCommand): ElectronicInvoiceHomologationReport {
        if (command.environment != SriEnvironment.TEST) {
            throw DomainRuleViolation("SRI homologation must run only in TEST environment.")
        }

        val scenarioCodes = command.scenarios.map { it.code }
        val duplicates = scenarioCodes.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw DomainRuleViolation("Duplicated homologation scenarios: ${duplicates.joinToString()}.")
        }

        if (command.strict) {
            val missing = command.requiredScenarioCodes - scenarioCodes.toSet()
            if (missing.isNotEmpty()) {
                throw DomainRuleViolation("Missing required SRI homologation scenarios: ${missing.joinToString()}.")
            }
        }

        val startedAt = Instant.now(clock)
        val results = command.scenarios.map { scenario -> runScenario(command, scenario) }
        return ElectronicInvoiceHomologationReport(
            organizationId = command.organizationId,
            environment = command.environment,
            requiredScenarioCodes = command.requiredScenarioCodes,
            scenarioResults = results,
            startedAt = startedAt,
            finishedAt = Instant.now(clock),
        )
    }

    private fun runScenario(
        command: RunElectronicInvoiceHomologationCommand,
        scenario: ElectronicInvoiceHomologationScenarioCommand,
    ): ElectronicInvoiceHomologationScenarioResult {
        val startedAt = Instant.now(clock)
        return try {
            validateScenario(command, scenario)
            val issueOutcome = issueRunner.issue(scenario.issueCommand)
            var finalRecord = issueOutcome.record
            var authorized = issueOutcome.authorized
            val artifactTypes = issueOutcome.artifacts.map { it.artifactType }.toMutableSet()
            val messages = mutableListOf<String>()

            if (scenario.retryAuthorizationWhenPending && finalRecord.status == ElectronicDocumentStatus.AUTHORIZATION_PENDING) {
                val retryRunner = retryAuthorizationRunner
                    ?: throw DomainRuleViolation("Authorization retry runner is required for PPR homologation scenario.")
                val retry = retryRunner.retry(
                    QuerySriAuthorizationCommand(
                        record = finalRecord,
                        actorUserId = command.actorUserId,
                    )
                )
                finalRecord = retry.record
                authorized = retry.authorized
                artifactTypes += retry.artifacts.map { it.artifactType }
                messages += "Authorization retry ended with ${retry.authorizationStatus}."
            }

            var delivered = false
            scenario.emailTo?.let { emailTo ->
                if (finalRecord.status !in setOf(
                        ElectronicDocumentStatus.AUTHORIZED,
                        ElectronicDocumentStatus.DELIVERY_PENDING
                    )
                ) {
                    throw DomainRuleViolation("Cannot deliver homologation document before authorization.")
                }
                val runner = deliveryRunner
                    ?: throw DomainRuleViolation("Delivery runner is required for email homologation scenario.")
                val delivery = runner.deliver(
                    EmailElectronicInvoiceCommand(
                        organizationId = command.organizationId,
                        documentId = finalRecord.id,
                        actorUserId = command.actorUserId,
                        actorEffectivePermissions = scenario.actorEffectivePermissions,
                        emailTo = emailTo,
                    )
                )
                finalRecord = delivery.record
                delivered = delivery.delivered
                messages += if (delivered) "Email delivery succeeded." else "Email delivery failed."
            }

            val validationMessages =
                validateOutcome(scenario, issueOutcome, finalRecord, authorized, delivered, artifactTypes)
            messages += validationMessages
            val passed = validationMessages.isEmpty()

            ElectronicInvoiceHomologationScenarioResult(
                code = scenario.code,
                status = if (passed) ElectronicInvoiceHomologationStepStatus.PASSED else ElectronicInvoiceHomologationStepStatus.FAILED,
                documentId = finalRecord.id,
                saleId = finalRecord.saleId,
                finalDocumentStatus = finalRecord.status,
                accessKey = finalRecord.accessKey.value,
                authorized = authorized,
                delivered = delivered,
                artifactTypes = artifactTypes,
                messages = messages.ifEmpty { listOf("Scenario passed.") },
                startedAt = startedAt,
                finishedAt = Instant.now(clock),
            )
        } catch (error: Throwable) {
            ElectronicInvoiceHomologationScenarioResult(
                code = scenario.code,
                status = ElectronicInvoiceHomologationStepStatus.FAILED,
                documentId = scenario.issueCommand.documentId,
                saleId = scenario.issueCommand.saleId,
                finalDocumentStatus = null,
                accessKey = null,
                authorized = false,
                delivered = false,
                artifactTypes = emptySet(),
                messages = listOf(error.message ?: error::class.simpleName.orEmpty()),
                startedAt = startedAt,
                finishedAt = Instant.now(clock),
            )
        }
    }

    private fun validateScenario(
        command: RunElectronicInvoiceHomologationCommand,
        scenario: ElectronicInvoiceHomologationScenarioCommand,
    ) {
        if (scenario.issueCommand.environment != SriEnvironment.TEST) {
            throw DomainRuleViolation("Scenario ${scenario.code} must use SRI TEST environment.")
        }
        if (scenario.issueCommand.environment != command.environment) {
            throw DomainRuleViolation("Scenario ${scenario.code} environment does not match homologation command.")
        }
        if (scenario.issueCommand.organizationId != command.organizationId) {
            throw DomainRuleViolation("Scenario ${scenario.code} organization does not match homologation command.")
        }
        if (scenario.issueCommand.actorUserId != command.actorUserId) {
            throw DomainRuleViolation("Scenario ${scenario.code} actor does not match homologation command.")
        }
    }

    private fun validateOutcome(
        scenario: ElectronicInvoiceHomologationScenarioCommand,
        issueOutcome: ElectronicInvoiceIssueOutcome,
        finalRecord: ElectronicInvoiceIssueRecord,
        authorized: Boolean,
        delivered: Boolean,
        artifactTypes: Set<ElectronicDocumentArtifactType>,
    ): List<String> = buildList {
        if (finalRecord.status != scenario.expectedFinalStatus) {
            add("Expected final status ${scenario.expectedFinalStatus}, got ${finalRecord.status}.")
        }
        if (authorized != scenario.expectAuthorized) {
            add("Expected authorized=${scenario.expectAuthorized}, got $authorized.")
        }
        if (!finalRecord.accessKey.value.matches(Regex("^[0-9]{49}$"))) {
            add("Access key must contain exactly 49 digits.")
        }
        if (!issueOutcome.generatedXmlPresent && scenario.expectedFinalStatus != ElectronicDocumentStatus.XSD_INVALID) {
            add("Generated XML is missing.")
        }
        if (scenario.expectedFinalStatus !in setOf(
                ElectronicDocumentStatus.XSD_INVALID,
                ElectronicDocumentStatus.SIGNATURE_FAILED
            ) &&
            !issueOutcome.signedXmlPresent
        ) {
            add("Signed XML is missing.")
        }
        if (scenario.expectAuthorized && ElectronicDocumentArtifactType.AUTHORIZED_XML !in artifactTypes) {
            add("Authorized XML artifact is missing.")
        }
        if (scenario.emailTo != null && !delivered) {
            add("Email delivery did not succeed.")
        }
        if (scenario.emailTo != null && finalRecord.status != ElectronicDocumentStatus.DELIVERED) {
            add("Email scenario must finish DELIVERED.")
        }
        if (scenario.retryAuthorizationWhenPending && issueOutcome.authorizationStatus != SriAuthorizationStatus.PROCESSING) {
            add("PPR retry scenario must first receive PROCESSING authorization status.")
        }
    }
}

class ApproveElectronicInvoiceProductionReadinessUseCase(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(report: ElectronicInvoiceHomologationReport): ElectronicInvoiceProductionReadinessDecision {
        val reasons = buildList {
            if (report.environment != SriEnvironment.TEST) {
                add("Homologation report must come from TEST environment.")
            }
            if (report.missingRequiredScenarioCodes.isNotEmpty()) {
                add("Missing required scenarios: ${report.missingRequiredScenarioCodes.joinToString()}.")
            }
            report.failedScenarioResults.forEach { result ->
                add("Scenario ${result.code} failed: ${result.messages.joinToString(" | ")}.")
            }
        }

        return ElectronicInvoiceProductionReadinessDecision(
            approved = reasons.isEmpty(),
            environment = report.environment,
            reasons = reasons.ifEmpty { listOf("All required SRI homologation scenarios passed in TEST environment.") },
            decidedAt = Instant.now(clock),
        )
    }
}
