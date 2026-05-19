package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactType
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationApiScenarioInput
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationReadinessResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationReportMarkdownResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationRun
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationRunResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationRunStatus
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationRunsResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceHomologationScenarioCode
import com.hermes.application.electronicinvoicing.EnableSriProductionCommand
import com.hermes.application.electronicinvoicing.EnableSriProductionResult
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationReadinessCommand
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationRunCommand
import com.hermes.application.electronicinvoicing.ListElectronicInvoiceHomologationRunsCommand
import com.hermes.application.electronicinvoicing.RunElectronicInvoiceHomologationFromAdminCommand
import com.hermes.application.electronicinvoicing.SriProductionGateCheck
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriNumericCode
import kotlinx.serialization.Serializable

@Serializable
data class RunElectronicInvoiceHomologationRequest(
    val strict: Boolean = true,
    val scenarios: List<ElectronicInvoiceHomologationScenarioRequest>,
)

@Serializable
data class ElectronicInvoiceHomologationScenarioRequest(
    val code: String,
    val saleId: String,
    val branchId: String,
    val emissionPointId: String? = null,
    val expectedFinalStatus: String = "AUTHORIZED",
    val expectAuthorized: Boolean? = null,
    val retryAuthorizationWhenPending: Boolean = false,
    val emailTo: String? = null,
    val queryAuthorizationImmediately: Boolean = true,
    val documentId: String? = null,
    val numericCode: String? = null,
    val notes: String? = null,
)

@Serializable
data class HomologationReadinessResponse(
    val organizationId: String,
    val environment: String,
    val ready: Boolean,
    val generatedAt: String,
    val checks: List<HomologationReadinessCheckResponse>,
)

@Serializable
data class HomologationReadinessCheckResponse(
    val id: String,
    val label: String,
    val status: String,
    val message: String,
)

@Serializable
data class HomologationRunResponse(
    val id: String,
    val organizationId: String,
    val status: String,
    val environment: String,
    val requestedByUserId: String,
    val approvedForProduction: Boolean,
    val requiredScenarioCodes: List<String>,
    val scenarioResults: List<HomologationScenarioResultResponse>,
    val productionDecision: HomologationProductionDecisionResponse?,
    val startedAt: String,
    val finishedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class HomologationRunsResponse(
    val runs: List<HomologationRunResponse>,
)

@Serializable
data class HomologationScenarioResultResponse(
    val code: String,
    val status: String,
    val documentId: String?,
    val saleId: String?,
    val finalDocumentStatus: String?,
    val accessKey: String?,
    val authorized: Boolean,
    val delivered: Boolean,
    val artifactTypes: List<String>,
    val messages: List<String>,
    val startedAt: String,
    val finishedAt: String,
)

@Serializable
data class HomologationProductionDecisionResponse(
    val approved: Boolean,
    val environment: String,
    val reasons: List<String>,
    val decidedAt: String,
)

@Serializable
data class EnableSriProductionRequest(
    val confirmation: String,
    val initialSequential: Int? = null,
)

@Serializable
data class EnableSriProductionResponse(
    val organizationId: String,
    val enabled: Boolean,
    val productionEnabled: Boolean,
    val homologationRunId: String?,
    val enabledAt: String?,
    val checks: List<SriProductionGateCheckResponse>,
)

@Serializable
data class SriProductionGateCheckResponse(
    val code: String,
    val ok: Boolean,
    val message: String,
)

fun homologationReadinessCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): GetElectronicInvoiceHomologationReadinessCommand = GetElectronicInvoiceHomologationReadinessCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
)

fun RunElectronicInvoiceHomologationRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): RunElectronicInvoiceHomologationFromAdminCommand = RunElectronicInvoiceHomologationFromAdminCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    strict = strict,
    scenarios = scenarios.map { it.toScenarioInput() },
)

fun listHomologationRunsCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    statuses: String?,
    limit: Int,
): ListElectronicInvoiceHomologationRunsCommand = ListElectronicInvoiceHomologationRunsCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    statuses = statuses
        ?.split(',')
        ?.mapNotNull { raw -> raw.trim().takeIf { it.isNotBlank() } }
        ?.map { ElectronicInvoiceHomologationRunStatus.fromStorage(it) }
        ?.toSet()
        .orEmpty(),
    limit = limit,
)

fun getHomologationRunCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    runId: String,
): GetElectronicInvoiceHomologationRunCommand = GetElectronicInvoiceHomologationRunCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    runId = runId,
)

fun EnableSriProductionRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): EnableSriProductionCommand = EnableSriProductionCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    confirmation = confirmation,
    initialSequential = initialSequential,
)

fun ElectronicInvoiceHomologationReadinessResult.toResponse(): HomologationReadinessResponse =
    HomologationReadinessResponse(
        organizationId = organizationId,
        environment = environment.storageValue,
        ready = ready,
        generatedAt = generatedAt.toString(),
        checks = checks.map {
            HomologationReadinessCheckResponse(
                id = it.id,
                label = it.label,
                status = it.status.name,
                message = it.message,
            )
        },
    )

fun ElectronicInvoiceHomologationRunResult.toResponse(): HomologationRunResponse = run.toResponse()
fun ElectronicInvoiceHomologationRunsResult.toResponse(): HomologationRunsResponse =
    HomologationRunsResponse(runs.map { it.toResponse() })

fun ElectronicInvoiceHomologationReportMarkdownResult.toResponseText(): String = markdown

fun EnableSriProductionResult.toResponse(): EnableSriProductionResponse = EnableSriProductionResponse(
    organizationId = organizationId,
    enabled = enabled,
    productionEnabled = settings?.productionEnabled ?: false,
    homologationRunId = homologationRun?.id,
    enabledAt = enabledAt?.toString(),
    checks = checks.map { it.toResponse() },
)

private fun ElectronicInvoiceHomologationScenarioRequest.toScenarioInput(): ElectronicInvoiceHomologationApiScenarioInput {
    val status = ElectronicDocumentStatus.valueOf(expectedFinalStatus.trim().uppercase())
    return ElectronicInvoiceHomologationApiScenarioInput(
        code = ElectronicInvoiceHomologationScenarioCode.valueOf(code.trim().uppercase()),
        saleId = saleId,
        branchId = branchId,
        emissionPointId = emissionPointId,
        expectedFinalStatus = status,
        expectAuthorized = expectAuthorized ?: status in setOf(
            ElectronicDocumentStatus.AUTHORIZED,
            ElectronicDocumentStatus.DELIVERY_PENDING,
            ElectronicDocumentStatus.DELIVERED,
        ),
        retryAuthorizationWhenPending = retryAuthorizationWhenPending,
        emailTo = emailTo,
        queryAuthorizationImmediately = queryAuthorizationImmediately,
        documentId = documentId,
        numericCode = numericCode?.trim()?.takeIf { it.isNotBlank() }?.let(::SriNumericCode),
        notes = notes,
    )
}

private fun ElectronicInvoiceHomologationRun.toResponse(): HomologationRunResponse = HomologationRunResponse(
    id = id,
    organizationId = organizationId,
    status = status.storageValue,
    environment = environment.storageValue,
    requestedByUserId = requestedByUserId,
    approvedForProduction = approvedForProduction,
    requiredScenarioCodes = requiredScenarioCodes.map { it.name }.sorted(),
    scenarioResults = scenarioResults.map { result ->
        HomologationScenarioResultResponse(
            code = result.code.name,
            status = result.status.name,
            documentId = result.documentId,
            saleId = result.saleId,
            finalDocumentStatus = result.finalDocumentStatus?.name,
            accessKey = result.accessKey,
            authorized = result.authorized,
            delivered = result.delivered,
            artifactTypes = result.artifactTypes.map(ElectronicDocumentArtifactType::name).sorted(),
            messages = result.messages,
            startedAt = result.startedAt.toString(),
            finishedAt = result.finishedAt.toString(),
        )
    },
    productionDecision = productionDecision?.let { decision ->
        HomologationProductionDecisionResponse(
            approved = decision.approved,
            environment = decision.environment.storageValue,
            reasons = decision.reasons,
            decidedAt = decision.decidedAt.toString(),
        )
    },
    startedAt = startedAt.toString(),
    finishedAt = finishedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

private fun SriProductionGateCheck.toResponse(): SriProductionGateCheckResponse = SriProductionGateCheckResponse(
    code = code,
    ok = ok,
    message = message,
)
