package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.OrganizationSriSettings
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.signature.ElectronicSignatureStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class GetElectronicInvoiceHomologationReadinessUseCase(
    private val settingsRepository: OrganizationSriSettingsRepository,
    private val signatureRepository: ElectronicSignatureRepository,
    private val endpointGateConfig: SriEndpointGateConfig,
    private val readinessUseCase: CheckElectronicInvoiceHomologationReadinessUseCase = CheckElectronicInvoiceHomologationReadinessUseCase(),
) {
    fun execute(command: GetElectronicInvoiceHomologationReadinessCommand): ElectronicInvoiceHomologationReadinessResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
        )
        val organizationId = command.organizationId.requiredHomologationText("Organization id")
        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("SRI settings are required before homologation.")
        if (settings.environment != SriEnvironment.TEST) {
            throw DomainRuleViolation("SRI homologation readiness can only be evaluated while settings are in TEST environment.")
        }
        val activeSignature = signatureRepository.findActiveByOrganizationId(organizationId)

        return readinessUseCase.execute(
            ElectronicInvoiceHomologationReadinessCommand(
                organizationId = organizationId,
                actorUserId = command.actorUserId.requiredHomologationText("Actor user id"),
                environment = settings.environment,
                issuerRuc = settings.ruc,
                series = settings.series,
                schemaVersionCode = settings.invoiceSchemaVersion.version,
                endpoints = endpointGateConfig.testEndpoints,
                activeSignatureId = activeSignature?.id,
            )
        )
    }
}

class RunElectronicInvoiceHomologationFromAdminUseCase(
    private val settingsRepository: OrganizationSriSettingsRepository,
    private val signatureRepository: ElectronicSignatureRepository,
    private val endpointGateConfig: SriEndpointGateConfig,
    private val homologationUseCase: RunElectronicInvoiceHomologationUseCase,
    private val approvalUseCase: ApproveElectronicInvoiceProductionReadinessUseCase,
    private val repository: ElectronicInvoiceHomologationRunRepository,
    private val idGenerator: ElectronicInvoiceHomologationRunIdGenerator = UuidElectronicInvoiceHomologationRunIdGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RunElectronicInvoiceHomologationFromAdminCommand): ElectronicInvoiceHomologationRunResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
        )
        val now = Instant.now(clock)
        val organizationId = command.organizationId.requiredHomologationText("Organization id")
        val actorUserId = command.actorUserId.requiredHomologationText("Actor user id")
        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("SRI settings are required before homologation.")
        if (settings.environment != SriEnvironment.TEST) {
            throw DomainRuleViolation("SRI homologation must run only while organization SRI settings are in TEST environment.")
        }
        if (!endpointGateConfig.testEndpoints.looksLikeSriTest || endpointGateConfig.testEndpoints.looksLikeSriProduction) {
            throw DomainRuleViolation("SRI homologation endpoints must point only to TEST/certification WSDLs.")
        }
        val activeSignature = signatureRepository.findActiveByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("An active electronic signature is required before homologation.")
        activeSignature.assertUsable(now)

        val scenarios = command.scenarios.map { scenario ->
            scenario.toCoreScenarioCommand(
                organizationId = organizationId,
                actorUserId = actorUserId,
                permissions = command.actorEffectivePermissions,
                settings = settings,
                signatureId = activeSignature.id,
                today = LocalDate.now(clock),
            )
        }

        val report = homologationUseCase.execute(
            RunElectronicInvoiceHomologationCommand(
                organizationId = organizationId,
                actorUserId = actorUserId,
                environment = SriEnvironment.TEST,
                scenarios = scenarios,
                strict = command.strict,
            )
        )
        val decision = approvalUseCase.execute(report)
        val finishedAt = report.finishedAt
        val run = ElectronicInvoiceHomologationRun(
            id = idGenerator.newId("homologation_run"),
            organizationId = organizationId,
            status = if (report.passed && decision.approved) {
                ElectronicInvoiceHomologationRunStatus.PASSED
            } else {
                ElectronicInvoiceHomologationRunStatus.FAILED
            },
            environment = report.environment,
            requestedByUserId = actorUserId,
            requiredScenarioCodes = report.requiredScenarioCodes,
            scenarioResults = report.scenarioResults,
            reportMarkdown = ElectronicInvoiceHomologationReportFormatter.toMarkdown(report),
            productionDecision = decision,
            approvedForProduction = decision.approved,
            startedAt = report.startedAt,
            finishedAt = finishedAt,
            createdAt = now,
            updatedAt = finishedAt,
        )
        repository.create(run)
        return ElectronicInvoiceHomologationRunResult(run)
    }
}

class ListElectronicInvoiceHomologationRunsUseCase(
    private val repository: ElectronicInvoiceHomologationRunRepository,
) {
    fun execute(command: ListElectronicInvoiceHomologationRunsCommand): ElectronicInvoiceHomologationRunsResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
        )
        return ElectronicInvoiceHomologationRunsResult(
            repository.search(
                ElectronicInvoiceHomologationRunSearchQuery(
                    organizationId = command.organizationId.requiredHomologationText("Organization id"),
                    statuses = command.statuses,
                    limit = command.limit.coerceIn(1, 200),
                )
            )
        )
    }
}

class GetElectronicInvoiceHomologationRunUseCase(
    private val repository: ElectronicInvoiceHomologationRunRepository,
) {
    fun execute(command: GetElectronicInvoiceHomologationRunCommand): ElectronicInvoiceHomologationRunResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
        )
        val run = repository.findById(
            organizationId = command.organizationId.requiredHomologationText("Organization id"),
            runId = command.runId.requiredHomologationText("Homologation run id"),
        ) ?: throw DomainRuleViolation("Electronic invoice homologation run does not exist.")
        return ElectronicInvoiceHomologationRunResult(run)
    }
}

class GetElectronicInvoiceHomologationReportUseCase(
    private val repository: ElectronicInvoiceHomologationRunRepository,
) {
    fun execute(command: GetElectronicInvoiceHomologationRunCommand): ElectronicInvoiceHomologationReportMarkdownResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_HOMOLOGATE,
        )
        val run = repository.findById(
            organizationId = command.organizationId.requiredHomologationText("Organization id"),
            runId = command.runId.requiredHomologationText("Homologation run id"),
        ) ?: throw DomainRuleViolation("Electronic invoice homologation run does not exist.")
        return ElectronicInvoiceHomologationReportMarkdownResult(runId = run.id, markdown = run.reportMarkdown)
    }
}

class EnableSriProductionUseCase(
    private val settingsRepository: OrganizationSriSettingsRepository,
    private val signatureRepository: ElectronicSignatureRepository,
    private val sequenceRepository: ElectronicSequenceRepository,
    private val homologationRunRepository: ElectronicInvoiceHomologationRunRepository,
    private val endpointGateConfig: SriEndpointGateConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: EnableSriProductionCommand): EnableSriProductionResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_ENABLE_PRODUCTION,
        )
        val now = Instant.now(clock)
        val organizationId = command.organizationId.requiredHomologationText("Organization id")
        val actorUserId = command.actorUserId.requiredHomologationText("Actor user id")
        val checks = mutableListOf<SriProductionGateCheck>()

        checks += gateCheck(
            code = "confirmation_phrase",
            ok = command.confirmation == EnableSriProductionCommand.CONFIRMATION_PHRASE,
            message = "Confirmation phrase must be ${EnableSriProductionCommand.CONFIRMATION_PHRASE}.",
        )
        checks += gateCheck(
            code = "global_feature_flag",
            ok = endpointGateConfig.productionGloballyEnabled,
            message = "Global SRI production feature flag must be enabled by deployment configuration.",
        )
        checks += gateCheck(
            code = "production_endpoints_configured",
            ok = endpointGateConfig.productionEndpointsConfigured,
            message = "Production SRI WSDL endpoints must be configured and must not point to TEST/certification.",
        )

        val settings = settingsRepository.findByOrganizationId(organizationId)
        checks += gateCheck(
            code = "sri_settings_configured",
            ok = settings != null,
            message = "SRI settings must exist before enabling production.",
        )
        if (settings != null) {
            checks += gateCheck(
                code = "settings_environment_production",
                ok = settings.environment == SriEnvironment.PRODUCTION,
                message = "SRI settings environment must be switched to PRODUCTION after successful homologation.",
            )
            checks += gateCheck(
                code = "ruc_configured",
                ok = settings.ruc.matches(Regex("^\\d{13}$")),
                message = "Issuer RUC must contain exactly 13 digits.",
            )
            checks += gateCheck(
                code = "settings_complete",
                ok = settings.legalName.isNotBlank() &&
                    settings.matrixAddress.isNotBlank() &&
                    settings.establishmentAddress.isNotBlank() &&
                    settings.establishmentCode.matches(Regex("^\\d{3}$")) &&
                    settings.emissionPointCode.matches(Regex("^\\d{3}$")),
                message = "Legal name, addresses and SRI series must be complete.",
            )

            val activeSignature = signatureRepository.findActiveByOrganizationId(organizationId)
            val signatureStatus = activeSignature?.effectiveStatus(now)
            checks += gateCheck(
                code = "active_signature_valid",
                ok = activeSignature != null && signatureStatus == ElectronicSignatureStatus.VALID,
                message = "A currently valid active electronic signature is required.",
            )

            val productionSequence = sequenceRepository.findByKey(
                ElectronicSequenceKey(
                    organizationId = organizationId,
                    environment = SriEnvironment.PRODUCTION,
                    documentType = SriDocumentType.INVOICE,
                    series = settings.series,
                )
            )
            checks += gateCheck(
                code = "production_sequence_initialized",
                ok = productionSequence != null && productionSequence.isActive,
                message = "An active production invoice sequence must be initialized for the configured series.",
            )
            command.initialSequential?.let { initialSequential ->
                checks += gateCheck(
                    code = "initial_sequential_matches_sequence",
                    ok = productionSequence != null && productionSequence.currentValue == initialSequential - 1,
                    message = "Production sequence current value must equal initialSequential - 1.",
                )
            }
        }

        val latestApproved = homologationRunRepository.findLatestApprovedForProduction(organizationId)
        checks += gateCheck(
            code = "homologation_approved",
            ok = latestApproved != null,
            message = "At least one approved TEST homologation run is required before production.",
        )

        val approved = checks.all { it.ok }
        if (!approved) {
            return EnableSriProductionResult(
                organizationId = organizationId,
                enabled = false,
                settings = settings,
                homologationRun = latestApproved,
                checks = checks,
                enabledAt = null,
            )
        }

        val updatedSettings = settings!!.enableProduction(actorUserId = actorUserId, now = now)
        val savedSettings = settingsRepository.save(updatedSettings)
        return EnableSriProductionResult(
            organizationId = organizationId,
            enabled = true,
            settings = savedSettings,
            homologationRun = latestApproved,
            checks = checks,
            enabledAt = now,
        )
    }

    private fun gateCheck(code: String, ok: Boolean, message: String): SriProductionGateCheck =
        SriProductionGateCheck(code = code, ok = ok, message = if (ok) "OK: $message" else message)
}

data class GetElectronicInvoiceHomologationReadinessCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class RunElectronicInvoiceHomologationFromAdminCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val scenarios: List<ElectronicInvoiceHomologationApiScenarioInput>,
    val strict: Boolean = true,
) {
    init {
        if (scenarios.isEmpty()) throw DomainRuleViolation("At least one homologation scenario is required.")
    }
}

data class ElectronicInvoiceHomologationApiScenarioInput(
    val code: ElectronicInvoiceHomologationScenarioCode,
    val saleId: String,
    val branchId: String,
    val emissionPointId: String? = null,
    val expectedFinalStatus: ElectronicDocumentStatus = ElectronicDocumentStatus.AUTHORIZED,
    val expectAuthorized: Boolean = expectedFinalStatus in setOf(
        ElectronicDocumentStatus.AUTHORIZED,
        ElectronicDocumentStatus.DELIVERY_PENDING,
        ElectronicDocumentStatus.DELIVERED,
    ),
    val retryAuthorizationWhenPending: Boolean = false,
    val emailTo: String? = null,
    val queryAuthorizationImmediately: Boolean = true,
    val documentId: String? = null,
    val numericCode: SriNumericCode? = null,
    val notes: String? = null,
)

data class ListElectronicInvoiceHomologationRunsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val statuses: Set<ElectronicInvoiceHomologationRunStatus> = emptySet(),
    val limit: Int = 50,
)

data class GetElectronicInvoiceHomologationRunCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val runId: String,
)

data class EnableSriProductionCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val confirmation: String,
    val initialSequential: Int? = null,
) {
    init {
        initialSequential?.let {
            if (it !in 1..999_999_999) throw DomainRuleViolation("Initial production sequential must be between 1 and 999999999.")
        }
    }

    companion object {
        const val CONFIRMATION_PHRASE: String = "ENABLE_SRI_PRODUCTION"
    }
}

data class ElectronicInvoiceHomologationRunResult(
    val run: ElectronicInvoiceHomologationRun,
)

data class ElectronicInvoiceHomologationRunsResult(
    val runs: List<ElectronicInvoiceHomologationRun>,
)

data class ElectronicInvoiceHomologationReportMarkdownResult(
    val runId: String,
    val markdown: String,
)

data class SriProductionGateCheck(
    val code: String,
    val ok: Boolean,
    val message: String,
)

data class EnableSriProductionResult(
    val organizationId: String,
    val enabled: Boolean,
    val settings: OrganizationSriSettings?,
    val homologationRun: ElectronicInvoiceHomologationRun?,
    val checks: List<SriProductionGateCheck>,
    val enabledAt: Instant?,
)

private fun ElectronicInvoiceHomologationApiScenarioInput.toCoreScenarioCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    settings: OrganizationSriSettings,
    signatureId: String,
    today: LocalDate,
): ElectronicInvoiceHomologationScenarioCommand = ElectronicInvoiceHomologationScenarioCommand(
    code = code,
    issueCommand = IssueElectronicInvoiceCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        saleId = saleId.requiredHomologationText("Homologation sale id"),
        branchId = branchId.requiredHomologationText("Homologation branch id"),
        emissionPointId = emissionPointId?.trim()?.takeIf { it.isNotBlank() } ?: settings.emissionPointCode,
        environment = SriEnvironment.TEST,
        issuerRuc = settings.ruc,
        series = settings.series,
        issuedDate = today,
        numericCode = numericCode ?: SriNumericCode.random(),
        signatureId = signatureId,
        documentId = documentId,
        queryAuthorizationImmediately = queryAuthorizationImmediately,
    ),
    expectedFinalStatus = expectedFinalStatus,
    expectAuthorized = expectAuthorized,
    retryAuthorizationWhenPending = retryAuthorizationWhenPending,
    emailTo = emailTo,
    actorEffectivePermissions = permissions,
    notes = notes,
)

private fun String.requiredHomologationText(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label is required.")
