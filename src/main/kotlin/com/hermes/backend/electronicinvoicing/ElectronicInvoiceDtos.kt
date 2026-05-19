package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceResult
import com.hermes.application.electronicinvoicing.ElectronicInvoicesResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesCommand
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriErrorClassification
import com.hermes.domain.electronicinvoicing.SriMessage
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ElectronicInvoicesResponse(
    val documents: List<ElectronicInvoiceSummaryResponse>,
)

@Serializable
data class ElectronicInvoiceSummaryResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String,
    val saleId: String,
    val environment: String,
    val documentType: String,
    val documentNumber: String,
    val accessKey: String,
    val authorizationNumber: String,
    val status: String,
    val schemaVersionCode: String?,
    val lastSriReceptionStatus: String?,
    val lastSriAuthorizationStatus: String?,
    val issuedAt: String,
    val authorizedAt: String?,
    val rideGeneratedAt: String?,
    val deliveryEmailTo: String?,
    val deliveredAt: String?,
    val hasErrors: Boolean,
    val artifacts: ElectronicInvoiceArtifactResponse,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class ElectronicInvoiceDetailResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String,
    val saleId: String,
    val environment: String,
    val documentType: String,
    val documentNumber: String,
    val accessKey: String,
    val authorizationNumber: String,
    val status: String,
    val schemaVersionCode: String?,
    val signatureId: String?,
    val lastSriReceptionStatus: String?,
    val lastSriAuthorizationStatus: String?,
    val sriMessages: List<ElectronicInvoiceSriMessageResponse>,
    val lastErrorClassification: ElectronicInvoiceErrorClassificationResponse?,
    val issuedAt: String,
    val authorizedAt: String?,
    val rideGeneratedAt: String?,
    val deliveryEmailTo: String?,
    val deliveredAt: String?,
    val deliveryErrorMessage: String?,
    val artifacts: ElectronicInvoiceArtifactResponse,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String,
    val version: Long,
)

@Serializable
data class ElectronicInvoiceSriMessageResponse(
    val identifier: String?,
    val message: String,
    val additionalInfo: String?,
    val type: String,
)

@Serializable
data class ElectronicInvoiceErrorClassificationResponse(
    val category: String,
    val recoverability: String,
    val userActionRequired: Boolean,
    val shouldKeepSameAccessKey: Boolean,
    val reason: String,
)

@Serializable
data class ElectronicInvoiceArtifactResponse(
    val unsignedXmlAvailable: Boolean,
    val unsignedXmlSha256: String?,
    val signedXmlAvailable: Boolean,
    val signedXmlSha256: String?,
    val authorizedXmlAvailable: Boolean,
    val authorizedXmlSha256: String?,
    val ridePdfAvailable: Boolean,
    val ridePdfSha256: String?,
)

fun electronicInvoiceSearchCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    saleId: String?,
    statuses: String?,
    environment: String?,
    from: String?,
    to: String?,
    limit: Int,
): ListElectronicInvoicesCommand = ListElectronicInvoicesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    saleId = saleId?.trim()?.takeIf { it.isNotBlank() },
    statuses = parseElectronicInvoiceStatuses(statuses),
    environment = parseElectronicInvoiceEnvironment(environment),
    from = from?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse),
    to = to?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse),
    limit = limit,
)

fun ElectronicInvoiceResult.toDetailResponse(): ElectronicInvoiceDetailResponse = record.toDetailResponse()

fun ElectronicInvoicesResult.toResponse(): ElectronicInvoicesResponse =
    ElectronicInvoicesResponse(records.map { it.toSummaryResponse() })

fun ElectronicInvoiceIssueRecord.toSummaryResponse(): ElectronicInvoiceSummaryResponse = ElectronicInvoiceSummaryResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    emissionPointId = emissionPointId,
    saleId = saleId,
    environment = environment.storageValue,
    documentType = documentType.storageValue,
    documentNumber = documentNumber,
    accessKey = accessKey.value,
    authorizationNumber = authorizationNumber,
    status = status.name,
    schemaVersionCode = schemaVersionCode,
    lastSriReceptionStatus = lastSriReceptionStatus,
    lastSriAuthorizationStatus = lastSriAuthorizationStatus,
    issuedAt = issuedAt.toString(),
    authorizedAt = authorizedAt?.toString(),
    rideGeneratedAt = rideGeneratedAt?.toString(),
    deliveryEmailTo = deliveryEmailTo,
    deliveredAt = deliveredAt?.toString(),
    hasErrors = sriMessages.any { it.isError } || lastErrorClassification != null || !deliveryErrorMessage.isNullOrBlank(),
    artifacts = toArtifactResponse(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun ElectronicInvoiceIssueRecord.toDetailResponse(): ElectronicInvoiceDetailResponse = ElectronicInvoiceDetailResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    emissionPointId = emissionPointId,
    saleId = saleId,
    environment = environment.storageValue,
    documentType = documentType.storageValue,
    documentNumber = documentNumber,
    accessKey = accessKey.value,
    authorizationNumber = authorizationNumber,
    status = status.name,
    schemaVersionCode = schemaVersionCode,
    signatureId = signatureId,
    lastSriReceptionStatus = lastSriReceptionStatus,
    lastSriAuthorizationStatus = lastSriAuthorizationStatus,
    sriMessages = sriMessages.map { it.toResponse() },
    lastErrorClassification = lastErrorClassification?.toResponse(),
    issuedAt = issuedAt.toString(),
    authorizedAt = authorizedAt?.toString(),
    rideGeneratedAt = rideGeneratedAt?.toString(),
    deliveryEmailTo = deliveryEmailTo,
    deliveredAt = deliveredAt?.toString(),
    deliveryErrorMessage = deliveryErrorMessage,
    artifacts = toArtifactResponse(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    createdBy = createdBy,
    updatedBy = updatedBy,
    version = version,
)

private fun ElectronicInvoiceIssueRecord.toArtifactResponse(): ElectronicInvoiceArtifactResponse =
    ElectronicInvoiceArtifactResponse(
        unsignedXmlAvailable = unsignedXmlObjectKey != null,
        unsignedXmlSha256 = unsignedXmlSha256,
        signedXmlAvailable = signedXmlObjectKey != null,
        signedXmlSha256 = signedXmlSha256,
        authorizedXmlAvailable = authorizedXmlObjectKey != null,
        authorizedXmlSha256 = authorizedXmlSha256,
        ridePdfAvailable = ridePdfObjectKey != null,
        ridePdfSha256 = ridePdfSha256,
    )

private fun SriMessage.toResponse(): ElectronicInvoiceSriMessageResponse = ElectronicInvoiceSriMessageResponse(
    identifier = identifier,
    message = message,
    additionalInfo = additionalInfo,
    type = type.name,
)

private fun SriErrorClassification.toResponse(): ElectronicInvoiceErrorClassificationResponse =
    ElectronicInvoiceErrorClassificationResponse(
        category = category.name,
        recoverability = recoverability.name,
        userActionRequired = userActionRequired,
        shouldKeepSameAccessKey = shouldKeepSameAccessKey,
        reason = reason,
    )

private fun parseElectronicInvoiceStatuses(raw: String?): Set<ElectronicDocumentStatus> =
    raw?.split(',')
        ?.mapNotNull { token -> token.trim().takeIf { it.isNotBlank() } }
        ?.map { token -> enumValueOf<ElectronicDocumentStatus>(token.uppercase()) }
        ?.toSet()
        .orEmpty()

private fun parseElectronicInvoiceEnvironment(raw: String?): SriEnvironment? =
    raw?.trim()?.takeIf { it.isNotBlank() }?.let(SriEnvironment::fromStorage)
