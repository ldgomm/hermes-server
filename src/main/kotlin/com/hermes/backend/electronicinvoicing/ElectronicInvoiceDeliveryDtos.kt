package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.DownloadElectronicInvoiceArtifactCommand
import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactFile
import com.hermes.application.electronicinvoicing.ElectronicInvoiceDownloadArtifactKind
import com.hermes.application.electronicinvoicing.EmailElectronicInvoiceCommand
import com.hermes.application.electronicinvoicing.EmailElectronicInvoiceResult
import com.hermes.application.electronicinvoicing.GenerateElectronicInvoiceRideCommand
import com.hermes.application.electronicinvoicing.GenerateElectronicInvoiceRideResult
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceTimelineCommand
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceTimelineResult
import com.hermes.application.electronicinvoicing.ElectronicInvoiceTimelineEvent
import kotlinx.serialization.Serializable

@Serializable
data class GenerateRideRequest(
    val forceRegenerate: Boolean = false,
)

@Serializable
data class GenerateRideResponse(
    val document: ElectronicInvoiceDetailResponse,
    val ride: ElectronicInvoiceGeneratedArtifactResponse,
)

@Serializable
data class EmailElectronicInvoiceRequest(
    val emailTo: String,
    val subject: String? = null,
    val message: String? = null,
    val forceRegenerateRide: Boolean = false,
    val allowResend: Boolean = true,
)

@Serializable
data class EmailElectronicInvoiceResponse(
    val delivered: Boolean,
    val emailTo: String,
    val document: ElectronicInvoiceDetailResponse,
    val ride: ElectronicInvoiceGeneratedArtifactResponse,
    val authorizedXml: ElectronicInvoiceGeneratedArtifactResponse,
)

@Serializable
data class ElectronicInvoiceGeneratedArtifactResponse(
    val artifactType: String,
    val filename: String,
    val contentType: String,
    val sha256: String,
    val sizeBytes: Long,
    val createdAt: String,
)

@Serializable
data class ElectronicInvoiceTimelineResponse(
    val documentId: String,
    val events: List<ElectronicInvoiceTimelineEventResponse>,
)

@Serializable
data class ElectronicInvoiceTimelineEventResponse(
    val id: String,
    val action: String,
    val actorUserId: String?,
    val saleId: String?,
    val accessKey: String?,
    val status: String?,
    val message: String?,
    val occurredAt: String,
)

fun generateRideCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
    request: GenerateRideRequest,
): GenerateElectronicInvoiceRideCommand = GenerateElectronicInvoiceRideCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    forceRegenerate = request.forceRegenerate,
)

fun EmailElectronicInvoiceRequest.toCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
): EmailElectronicInvoiceCommand = EmailElectronicInvoiceCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    emailTo = emailTo,
    subject = subject,
    message = message,
    forceRegenerateRide = forceRegenerateRide,
    allowResend = allowResend,
)

fun downloadArtifactCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
    artifactKind: ElectronicInvoiceDownloadArtifactKind,
): DownloadElectronicInvoiceArtifactCommand = DownloadElectronicInvoiceArtifactCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    artifactKind = artifactKind,
)

fun timelineCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
    limit: Int,
): GetElectronicInvoiceTimelineCommand = GetElectronicInvoiceTimelineCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    limit = limit,
)

fun GenerateElectronicInvoiceRideResult.toResponse(): GenerateRideResponse = GenerateRideResponse(
    document = record.toDetailResponse(),
    ride = ridePdf.toGeneratedArtifactResponse(),
)

fun EmailElectronicInvoiceResult.toResponse(): EmailElectronicInvoiceResponse = EmailElectronicInvoiceResponse(
    delivered = delivered,
    emailTo = record.deliveryEmailTo ?: "",
    document = record.toDetailResponse(),
    ride = ridePdf.toGeneratedArtifactResponse(),
    authorizedXml = authorizedXml.toGeneratedArtifactResponse(),
)

fun GetElectronicInvoiceTimelineResult.toResponse(): ElectronicInvoiceTimelineResponse = ElectronicInvoiceTimelineResponse(
    documentId = record.id,
    events = events.map { it.toResponse() },
)

private fun ElectronicDocumentArtifactFile.toGeneratedArtifactResponse(): ElectronicInvoiceGeneratedArtifactResponse =
    ElectronicInvoiceGeneratedArtifactResponse(
        artifactType = artifactType.storageValue,
        filename = filename,
        contentType = contentType,
        sha256 = sha256,
        sizeBytes = bytes.size.toLong(),
        createdAt = createdAt.toString(),
    )

private fun ElectronicInvoiceTimelineEvent.toResponse(): ElectronicInvoiceTimelineEventResponse =
    ElectronicInvoiceTimelineEventResponse(
        id = id,
        action = action,
        actorUserId = actorUserId,
        saleId = saleId,
        accessKey = accessKey,
        status = status,
        message = message,
        occurredAt = occurredAt.toString(),
    )
