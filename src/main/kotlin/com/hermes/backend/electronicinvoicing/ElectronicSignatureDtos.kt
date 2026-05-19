package com.hermes.backend.electronicinvoicing

import com.hermes.application.signature.*
import com.hermes.domain.signature.ElectronicSignature
import com.hermes.domain.signature.SignatureValidityReport
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UploadElectronicSignatureRequest(
    val fileName: String,
    val contentBase64: String,
    val password: String,
)

@Serializable
data class ElectronicSignaturesResponse(
    val signatures: List<ElectronicSignatureResponse>,
)

@Serializable
data class ElectronicSignatureEnvelopeResponse(
    val signature: ElectronicSignatureResponse,
)

@Serializable
data class ElectronicSignatureResponse(
    val id: String,
    val organizationId: String,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val status: String,
    val effectiveStatus: String,
    val daysUntilExpiration: Long,
    val expiresSoon: Boolean,
    val usable: Boolean,
    val uploadedBy: String,
    val uploadedAt: String,
    val lastUsedAt: String?,
)

fun UploadElectronicSignatureRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): UploadElectronicSignatureCommand = UploadElectronicSignatureCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    fileName = fileName,
    content = Base64.getDecoder().decode(contentBase64),
    password = password.toCharArray(),
)

fun listSignaturesCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
): ListElectronicSignaturesCommand = ListElectronicSignaturesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
)

fun getSignatureCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    signatureId: String,
): GetElectronicSignatureCommand = GetElectronicSignatureCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    signatureId = signatureId,
)

fun validateSignatureCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    signatureId: String,
): ValidateElectronicSignatureCommand = ValidateElectronicSignatureCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    signatureId = signatureId,
)

fun activateSignatureCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    signatureId: String,
): ActivateElectronicSignatureCommand = ActivateElectronicSignatureCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    signatureId = signatureId,
)

fun revokeSignatureCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    signatureId: String,
): RevokeElectronicSignatureCommand = RevokeElectronicSignatureCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    signatureId = signatureId,
)

fun ElectronicSignaturesResult.toResponse(): ElectronicSignaturesResponse =
    ElectronicSignaturesResponse(signatures.map { it.toResponse(checkedAt) })

fun ElectronicSignatureResult.toResponse(): ElectronicSignatureEnvelopeResponse =
    ElectronicSignatureEnvelopeResponse(signature.toResponse(validation))

private fun ElectronicSignature.toResponse(now: java.time.Instant): ElectronicSignatureResponse = toResponse(
    SignatureValidityReport(
        storedStatus = status,
        effectiveStatus = effectiveStatus(now),
        validFrom = validFrom,
        validTo = validTo,
        daysUntilExpiration = java.time.Duration.between(now, validTo).toDays(),
        expiresSoon = java.time.Duration.between(now, validTo).toDays() in 0..30,
    )
)

private fun ElectronicSignature.toResponse(validation: SignatureValidityReport): ElectronicSignatureResponse =
    ElectronicSignatureResponse(
        id = id,
        organizationId = organizationId,
        subject = subject,
        issuer = issuer,
        validFrom = validFrom.toString(),
        validTo = validTo.toString(),
        status = status.name,
        effectiveStatus = validation.effectiveStatus.name,
        daysUntilExpiration = validation.daysUntilExpiration,
        expiresSoon = validation.expiresSoon,
        usable = validation.usable,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt.toString(),
        lastUsedAt = lastUsedAt?.toString(),
    )
