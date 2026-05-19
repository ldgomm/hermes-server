package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GenerateElectronicInvoiceRideUseCase(
    private val repository: ElectronicInvoiceIssueRepository,
    private val artifactStorage: ElectronicDocumentArtifactStorage,
    private val artifactReader: ElectronicDocumentArtifactReader,
    private val rideRenderer: ElectronicInvoiceRideRenderer = SimpleSriRidePdfRenderer(),
    private val auditLogger: ElectronicInvoiceIssueAuditLogger = NoopElectronicInvoiceIssueAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GenerateElectronicInvoiceRideCommand): GenerateElectronicInvoiceRideResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_INVOICE_DOWNLOAD_RIDE
        )
        val now = Instant.now(clock)
        var record = repository.findById(command.organizationId.trim(), command.documentId.trim())
            ?: throw DomainRuleViolation("Electronic invoice issue record does not exist.")

        if (record.status !in rideAllowedStatuses) {
            throw DomainRuleViolation("RIDE can only be generated after SRI authorization.")
        }

        val existingRide = record.ridePdfObjectKey
            ?.let(artifactReader::get)
            ?: artifactReader.findLatest(record.organizationId, record.id, ElectronicDocumentArtifactType.RIDE_PDF)

        if (existingRide != null && !command.forceRegenerate) {
            if (record.status == ElectronicDocumentStatus.AUTHORIZED || record.status == ElectronicDocumentStatus.DELIVERY_FAILED) {
                record = record.markRideGenerated(existingRide.toStoredArtifact(), now, command.actorUserId)
                repository.update(record)
            }
            auditLogger.log(
                record.toIssueAuditEvent(
                    action = ElectronicInvoiceIssueAuditAction.ELECTRONIC_RIDE_REUSED,
                    actorUserId = command.actorUserId,
                    now = now,
                    message = existingRide.objectKey,
                )
            )
            return GenerateElectronicInvoiceRideResult(record, existingRide)
        }

        if (record.status == ElectronicDocumentStatus.DELIVERED) {
            throw DomainRuleViolation("Delivered electronic invoice already requires an existing RIDE artifact to resend.")
        }

        val authorizedXmlKey = record.authorizedXmlObjectKey
            ?: throw DomainRuleViolation("Authorized XML must exist before generating RIDE.")
        val authorizedXml = artifactReader.get(authorizedXmlKey)
            ?: throw DomainRuleViolation("Authorized XML artifact does not exist in storage.")

        val generatedRide = rideRenderer.render(
            ElectronicInvoiceRideRenderCommand(
                record = record,
                authorizedXml = authorizedXml.bytes,
                generatedAt = now,
            )
        )
        val stored = artifactStorage.put(
            StoreElectronicDocumentArtifactCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                artifactType = ElectronicDocumentArtifactType.RIDE_PDF,
                content = generatedRide.bytes,
                contentType = generatedRide.contentType,
                fileName = generatedRide.filename,
                createdAt = now,
            )
        )
        val rideFile = ElectronicDocumentArtifactFile(
            objectKey = stored.objectKey,
            artifactType = stored.artifactType,
            filename = generatedRide.filename,
            contentType = generatedRide.contentType,
            bytes = generatedRide.bytes,
            sha256 = stored.sha256,
            createdAt = stored.createdAt,
        )

        record = record.markRideGenerated(stored, now, command.actorUserId)
        repository.update(record)
        auditLogger.log(
            record.toIssueAuditEvent(
                action = ElectronicInvoiceIssueAuditAction.ELECTRONIC_RIDE_GENERATED,
                actorUserId = command.actorUserId,
                now = now,
                message = stored.objectKey,
            )
        )

        return GenerateElectronicInvoiceRideResult(record, rideFile)
    }

    private companion object {
        val rideAllowedStatuses: Set<ElectronicDocumentStatus> = setOf(
            ElectronicDocumentStatus.AUTHORIZED,
            ElectronicDocumentStatus.DELIVERY_PENDING,
            ElectronicDocumentStatus.DELIVERY_FAILED,
            ElectronicDocumentStatus.DELIVERED,
        )
    }
}
