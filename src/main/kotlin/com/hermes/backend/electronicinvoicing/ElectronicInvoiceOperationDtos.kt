package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceErrorsResult
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceFromSaleCommand
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceFromSaleResult
import com.hermes.application.electronicinvoicing.RetryElectronicInvoiceAuthorizationCommand
import com.hermes.application.electronicinvoicing.RetryElectronicInvoiceAuthorizationResult
import com.hermes.application.electronicinvoicing.StoredElectronicDocumentArtifact
import com.hermes.domain.electronicinvoicing.SriErrorClassification
import com.hermes.domain.electronicinvoicing.SriMessage
import com.hermes.domain.electronicinvoicing.SriNumericCode
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Serializable
data class IssueElectronicInvoiceRequest(
    val saleId: String,
    val signatureId: String? = null,
    val queryAuthorizationImmediately: Boolean = true,
    val numericCode: String? = null,
    val documentId: String? = null,
    val issuedAt: String? = null,
    val issuedDate: String? = null,
)

@Serializable
data class IssueElectronicInvoiceResponse(
    val authorized: Boolean,
    val stoppedBeforeSri: Boolean,
    val document: ElectronicInvoiceDetailResponse,
    val receptionStatus: String?,
    val authorizationStatus: String?,
    val artifacts: List<ElectronicInvoiceOperationArtifactResponse>,
)

@Serializable
data class RetryAuthorizationResponse(
    val authorized: Boolean,
    val authorizationStatus: String,
    val document: ElectronicInvoiceDetailResponse,
    val messages: List<ElectronicInvoiceSriMessageResponse>,
    val artifacts: List<ElectronicInvoiceOperationArtifactResponse>,
)

@Serializable
data class ElectronicInvoiceErrorsResponse(
    val documentId: String,
    val status: String,
    val hasErrors: Boolean,
    val lastSriReceptionStatus: String?,
    val lastSriAuthorizationStatus: String?,
    val messages: List<ElectronicInvoiceSriMessageResponse>,
    val lastErrorClassification: ElectronicInvoiceErrorClassificationResponse?,
    val deliveryErrorMessage: String?,
)

@Serializable
data class ElectronicInvoiceOperationArtifactResponse(
    val artifactType: String,
    val sha256: String,
    val sizeBytes: Long,
    val createdAt: String,
)

fun IssueElectronicInvoiceRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): IssueElectronicInvoiceFromSaleCommand = IssueElectronicInvoiceFromSaleCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    saleId = saleId,
    signatureId = signatureId?.trim()?.takeIf { it.isNotBlank() },
    queryAuthorizationImmediately = queryAuthorizationImmediately,
    numericCode = numericCode?.trim()?.takeIf { it.isNotBlank() }?.let(::SriNumericCode),
    documentId = documentId?.trim()?.takeIf { it.isNotBlank() },
    issuedAt = issuedAt?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse),
    issuedDate = issuedDate?.trim()?.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
)

fun retryAuthorizationCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
): RetryElectronicInvoiceAuthorizationCommand = RetryElectronicInvoiceAuthorizationCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
)

fun electronicInvoiceErrorsCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
): com.hermes.application.electronicinvoicing.GetElectronicInvoiceErrorsCommand =
    com.hermes.application.electronicinvoicing.GetElectronicInvoiceErrorsCommand(
        organizationId = organizationId,
        documentId = documentId,
        actorUserId = actorUserId,
        actorEffectivePermissions = permissions,
    )

fun IssueElectronicInvoiceFromSaleResult.toResponse(): IssueElectronicInvoiceResponse = IssueElectronicInvoiceResponse(
    authorized = issueResult.authorized,
    stoppedBeforeSri = issueResult.stoppedBeforeSri,
    document = issueResult.record.toDetailResponse(),
    receptionStatus = issueResult.reception?.status?.name,
    authorizationStatus = issueResult.authorization?.status?.name,
    artifacts = issueResult.artifacts.map { it.toOperationArtifactResponse() },
)

fun RetryElectronicInvoiceAuthorizationResult.toResponse(): RetryAuthorizationResponse = RetryAuthorizationResponse(
    authorized = authorization.isAuthorized,
    authorizationStatus = authorization.status.name,
    document = record.toDetailResponse(),
    messages = authorization.messages.map { it.toOperationMessageResponse() },
    artifacts = artifacts.map { it.toOperationArtifactResponse() },
)

fun ElectronicInvoiceErrorsResult.toResponse(): ElectronicInvoiceErrorsResponse {
    val invoiceRecord = this.record
    return ElectronicInvoiceErrorsResponse(
        documentId = invoiceRecord.id,
        status = invoiceRecord.status.name,
        hasErrors = invoiceRecord.sriMessages.any { it.isError } || invoiceRecord.lastErrorClassification != null || !invoiceRecord.deliveryErrorMessage.isNullOrBlank(),
        lastSriReceptionStatus = invoiceRecord.lastSriReceptionStatus,
        lastSriAuthorizationStatus = invoiceRecord.lastSriAuthorizationStatus,
        messages = invoiceRecord.sriMessages.map { it.toOperationMessageResponse() },
        lastErrorClassification = invoiceRecord.lastErrorClassification?.toOperationErrorClassificationResponse(),
        deliveryErrorMessage = invoiceRecord.deliveryErrorMessage,
    )
}

private fun StoredElectronicDocumentArtifact.toOperationArtifactResponse(): ElectronicInvoiceOperationArtifactResponse =
    ElectronicInvoiceOperationArtifactResponse(
        artifactType = artifactType.storageValue,
        sha256 = sha256,
        sizeBytes = sizeBytes,
        createdAt = createdAt.toString(),
    )

private fun SriMessage.toOperationMessageResponse(): ElectronicInvoiceSriMessageResponse = ElectronicInvoiceSriMessageResponse(
    identifier = identifier,
    message = message,
    additionalInfo = additionalInfo,
    type = type.name,
)

private fun SriErrorClassification.toOperationErrorClassificationResponse(): ElectronicInvoiceErrorClassificationResponse =
    ElectronicInvoiceErrorClassificationResponse(
        category = category.name,
        recoverability = recoverability.name,
        userActionRequired = userActionRequired,
        shouldKeepSameAccessKey = shouldKeepSameAccessKey,
        reason = reason,
    )
