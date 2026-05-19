package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.domain.electronicinvoicing.*
import kotlinx.serialization.Serializable

@Serializable
data class EnsureElectronicSequenceRequest(
    val environment: String = "test",
    val documentType: String = "electronic_invoice",
    val establishmentCode: String,
    val emissionPointCode: String,
    val startsAfter: Int = 0,
)

@Serializable
data class ElectronicSequencesResponse(
    val sequences: List<ElectronicSequenceResponse>,
)

@Serializable
data class ElectronicSequenceEnvelopeResponse(
    val sequence: ElectronicSequenceResponse,
)

@Serializable
data class ElectronicSequenceResponse(
    val id: String,
    val organizationId: String,
    val environment: String,
    val documentType: String,
    val establishmentCode: String,
    val emissionPointCode: String,
    val series: String,
    val currentValue: Int,
    val lastIssuedSequential: String?,
    val status: String,
    val lastIssuedDocumentId: String?,
    val lastIssuedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val version: Int,
)

fun EnsureElectronicSequenceRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): EnsureElectronicSequenceAdminCommand = EnsureElectronicSequenceAdminCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    environment = SriEnvironment.fromStorage(environment),
    documentType = SriDocumentType.fromStorage(documentType),
    series = SriSeries(establishmentCode = establishmentCode, emissionPointCode = emissionPointCode),
    startsAfter = startsAfter,
)

fun listSequencesCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    environment: String?,
    documentType: String?,
    status: String?,
    limit: Int,
): ListElectronicSequencesCommand = ListElectronicSequencesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    environment = environment?.trim()?.takeIf { it.isNotBlank() }?.let(SriEnvironment::fromStorage),
    documentType = documentType?.trim()?.takeIf { it.isNotBlank() }?.let(SriDocumentType::fromStorage),
    status = status?.trim()?.takeIf { it.isNotBlank() }?.let(ElectronicSequenceStatus::fromStorage),
    limit = limit,
)

fun getSequenceCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    sequenceId: String,
): GetElectronicSequenceCommand = GetElectronicSequenceCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    sequenceId = sequenceId,
)

fun ElectronicSequencesResult.toResponse(): ElectronicSequencesResponse =
    ElectronicSequencesResponse(sequences.map { it.toResponse() })

fun ElectronicSequenceResult.toResponse(): ElectronicSequenceEnvelopeResponse =
    ElectronicSequenceEnvelopeResponse(sequence.toResponse())

fun ElectronicSequence.toResponse(): ElectronicSequenceResponse = ElectronicSequenceResponse(
    id = id,
    organizationId = organizationId,
    environment = environment.storageValue,
    documentType = documentType.storageValue,
    establishmentCode = series.establishmentCode,
    emissionPointCode = series.emissionPointCode,
    series = series.displayValue,
    currentValue = currentValue,
    lastIssuedSequential = lastIssuedSequential?.formatted,
    status = status.storageValue,
    lastIssuedDocumentId = lastIssuedDocumentId,
    lastIssuedAt = lastIssuedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)
