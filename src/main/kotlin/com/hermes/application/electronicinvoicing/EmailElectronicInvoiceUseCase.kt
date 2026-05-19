package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class EmailElectronicInvoiceUseCase(
    private val repository: ElectronicInvoiceIssueRepository,
    private val artifactReader: ElectronicDocumentArtifactReader,
    private val generateRideUseCase: GenerateElectronicInvoiceRideUseCase,
    private val emailSender: ElectronicInvoiceEmailSender = NoopElectronicInvoiceEmailSender,
    private val auditLogger: ElectronicInvoiceIssueAuditLogger = NoopElectronicInvoiceIssueAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: EmailElectronicInvoiceCommand): EmailElectronicInvoiceResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_EMAIL,
        )

        val now = Instant.now(clock)
        var record = repository.findById(command.organizationId.trim(), command.documentId.trim())
            ?: throw DomainRuleViolation("Electronic invoice issue record does not exist.")

        if (record.status == ElectronicDocumentStatus.DELIVERED && !command.allowResend) {
            throw DomainRuleViolation("Electronic invoice has already been delivered.")
        }
        if (record.status !in emailableStatuses) {
            throw DomainRuleViolation("Electronic invoice can only be emailed after SRI authorization and RIDE generation.")
        }

        val rideResult = if (record.ridePdfObjectKey.isNullOrBlank() ||
            record.status == ElectronicDocumentStatus.AUTHORIZED ||
            command.forceRegenerateRide
        ) {
            generateRideUseCase.execute(
                GenerateElectronicInvoiceRideCommand(
                    organizationId = command.organizationId,
                    documentId = command.documentId,
                    actorUserId = command.actorUserId,
                    actorEffectivePermissions = command.actorEffectivePermissions,
                    forceRegenerate = command.forceRegenerateRide,
                )
            )
        } else {
            val ride = record.ridePdfObjectKey
                ?.let(artifactReader::get)
                ?: artifactReader.findLatest(record.organizationId, record.id, ElectronicDocumentArtifactType.RIDE_PDF)
                ?: throw DomainRuleViolation("RIDE PDF artifact does not exist in storage.")
            GenerateElectronicInvoiceRideResult(record, ride)
        }

        record = rideResult.record
        if (record.status == ElectronicDocumentStatus.DELIVERY_FAILED) {
            record = record.transitionTo(ElectronicDocumentStatus.DELIVERY_PENDING, now, command.actorUserId)
            repository.update(record)
        }

        val authorizedXmlKey = record.authorizedXmlObjectKey
            ?: throw DomainRuleViolation("Authorized XML must exist before emailing electronic invoice.")
        val authorizedXml = artifactReader.get(authorizedXmlKey)
            ?: throw DomainRuleViolation("Authorized XML artifact does not exist in storage.")

        val recipient = command.emailTo.required("Electronic invoice email recipient")
        val subject = command.subject?.trim()?.takeIf { it.isNotBlank() }
            ?: "Factura electrónica ${record.documentNumber}"
        val message = command.message?.trim()?.takeIf { it.isNotBlank() }
            ?: "Adjuntamos su factura electrónica autorizada por el SRI, junto con el XML y el RIDE."

        val delivered = emailSender.send(
            ElectronicInvoiceEmail(
                to = recipient,
                subject = subject,
                message = message,
                attachments = listOf(
                    rideResult.ridePdf.toEmailAttachment(),
                    authorizedXml.toEmailAttachment(),
                ),
                record = record,
            )
        )

        val updated = when {
            delivered -> record.markDelivered(recipient, Instant.now(clock), command.actorUserId)
            record.status == ElectronicDocumentStatus.DELIVERED -> record
            else -> record.markDeliveryFailed(
                emailTo = recipient,
                reason = "Email sender returned false.",
                failedAt = Instant.now(clock),
                actorUserId = command.actorUserId,
            )
        }
        if (updated != record) repository.update(updated)

        auditLogger.log(
            updated.toIssueAuditEvent(
                action = if (delivered) {
                    ElectronicInvoiceIssueAuditAction.ELECTRONIC_INVOICE_EMAIL_SENT
                } else {
                    ElectronicInvoiceIssueAuditAction.ELECTRONIC_INVOICE_EMAIL_FAILED
                },
                actorUserId = command.actorUserId,
                now = Instant.now(clock),
                message = recipient,
            )
        )

        return EmailElectronicInvoiceResult(
            record = updated,
            delivered = delivered,
            ridePdf = rideResult.ridePdf,
            authorizedXml = authorizedXml,
        )
    }

    private fun ElectronicDocumentArtifactFile.toEmailAttachment(): ElectronicInvoiceEmailAttachment =
        ElectronicInvoiceEmailAttachment(
            filename = filename,
            contentType = contentType,
            bytes = bytes,
        )

    private companion object {
        val emailableStatuses: Set<ElectronicDocumentStatus> = setOf(
            ElectronicDocumentStatus.AUTHORIZED,
            ElectronicDocumentStatus.DELIVERY_PENDING,
            ElectronicDocumentStatus.DELIVERY_FAILED,
            ElectronicDocumentStatus.DELIVERED,
        )
    }
}
