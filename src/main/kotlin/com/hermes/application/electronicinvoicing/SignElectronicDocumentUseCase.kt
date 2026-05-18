package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

data class SignElectronicDocumentCommand(
    val organizationId: String,
    val documentId: String,
    val signatureId: String? = null,
    val accessKey: String? = null,
    val unsignedXml: ByteArray,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to sign document.")
        if (documentId.isBlank()) throw DomainRuleViolation("Document id is required to sign document.")
        if (unsignedXml.isEmpty()) throw DomainRuleViolation("Unsigned XML cannot be empty.")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignElectronicDocumentCommand) return false
        return organizationId == other.organizationId &&
                documentId == other.documentId &&
                signatureId == other.signatureId &&
                accessKey == other.accessKey &&
                unsignedXml.contentEquals(other.unsignedXml)
    }

    override fun hashCode(): Int {
        var result = organizationId.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + (signatureId?.hashCode() ?: 0)
        result = 31 * result + (accessKey?.hashCode() ?: 0)
        result = 31 * result + unsignedXml.contentHashCode()
        return result
    }
}

data class SignElectronicDocumentResult(
    val signedXml: SignedXml,
    val signatureId: String,
    val signedAt: Instant,
)

class SignElectronicDocumentUseCase(
    private val signatureVault: ElectronicSignatureVault,
    private val signatureRepository: ElectronicSignatureRepository,
    private val xmlSigner: XmlSigner,
    private val auditLogger: ElectronicSignatureUsageAuditLogger = NoopElectronicSignatureUsageAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: SignElectronicDocumentCommand): SignElectronicDocumentResult {
        val now = Instant.now(clock)
        val material = if (command.signatureId.isNullOrBlank()) {
            signatureVault.loadActiveForSigning(command.organizationId, now)
        } else {
            signatureVault.loadForSigning(command.organizationId, command.signatureId, now)
        }

        return try {
            val signedXml = xmlSigner.sign(
                SignXmlCommand(
                    organizationId = command.organizationId,
                    signatureId = material.signature.id,
                    xml = command.unsignedXml,
                    accessKey = command.accessKey,
                    signedAt = now,
                ),
                material.keyMaterial,
            )

            signatureRepository.update(material.signature.markUsed(now))
            auditLogger.log(
                ElectronicSignatureUsageAuditEvent(
                    action = ElectronicSignatureUsageAuditAction.ELECTRONIC_SIGNATURE_USED,
                    organizationId = command.organizationId,
                    signatureId = material.signature.id,
                    documentId = command.documentId,
                    accessKey = command.accessKey,
                    createdAt = now,
                )
            )

            SignElectronicDocumentResult(
                signedXml = signedXml,
                signatureId = material.signature.id,
                signedAt = now,
            )
        } catch (error: Throwable) {
            auditLogger.log(
                ElectronicSignatureUsageAuditEvent(
                    action = ElectronicSignatureUsageAuditAction.ELECTRONIC_SIGNATURE_FAILED,
                    organizationId = command.organizationId,
                    signatureId = material.signature.id,
                    documentId = command.documentId,
                    accessKey = command.accessKey,
                    message = error.message,
                    createdAt = now,
                )
            )
            throw error
        }
    }
}
