package com.hermes.application.documents

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentType
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class GenerateInternalTicketUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val documentRepository: CommercialDocumentRepository,
    private val numberGenerator: CommercialDocumentNumberGenerator,
    private val idGenerator: CommercialDocumentIdGenerator,
    private val pdfRenderer: CommercialDocumentPdfRenderer = SimpleCommercialDocumentPdfRenderer(),
    private val fileStorage: CommercialDocumentFileStorage = InMemoryCommercialDocumentFileStorage(),
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GenerateInternalTicketCommand): CommercialDocumentResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_GENERATE_INTERNAL_TICKET
        )
        val now = command.issuedAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val sale = saleRepository.findById(organizationId, command.saleId.required("Sale id"))
            ?: throw DomainRuleViolation("Sale does not exist.")

        if (!command.allowDuplicate) {
            documentRepository.findBySale(organizationId, sale.id)
                .firstOrNull { it.documentType == DocumentType.INTERNAL_TICKET }
                ?.let { existing ->
                    return CommercialDocumentResult(
                        existing,
                        existing.pdfObjectKey?.let(fileStorage::get)
                    )
                }
        }

        val documentNumber = numberGenerator.nextInternalTicketNumber(organizationId, sale.branchId, now)
        val draft = CommercialDocument.draftFromSale(
            id = idGenerator.newId("doc"),
            sale = sale,
            documentType = DocumentType.INTERNAL_TICKET,
            documentNumber = documentNumber,
            emissionPointId = command.emissionPointId,
            issuedAt = now,
            createdBy = command.actorUserId,
            notes = command.notes,
        )
        documentRepository.create(draft)
        val file = fileStorage.put(pdfRenderer.render(draft))
        val generated = draft.markGenerated(
            payloadId = null,
            pdfObjectKey = file.objectKey,
            updatedAt = now,
            updatedBy = command.actorUserId
        )
        documentRepository.update(generated)
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.INTERNAL_TICKET_GENERATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = generated.id,
                saleId = sale.id,
                after = generated.toAuditMap(),
                createdAt = Instant.now(clock),
            )
        )
        return CommercialDocumentResult(generated, file)
    }
}

class RegisterPhysicalSaleNoteUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val documentRepository: CommercialDocumentRepository,
    private val idGenerator: CommercialDocumentIdGenerator,
    private val pdfRenderer: CommercialDocumentPdfRenderer = SimpleCommercialDocumentPdfRenderer(),
    private val fileStorage: CommercialDocumentFileStorage = InMemoryCommercialDocumentFileStorage(),
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RegisterPhysicalSaleNoteCommand): CommercialDocumentResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY
        )
        val now = command.issuedAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val physicalNumber = command.physicalDocumentNumber.required("Physical sale note number")

        documentRepository.findByDocumentNumber(organizationId, physicalNumber)?.let { existing ->
            throw DomainRuleViolation("Commercial document number already exists: $physicalNumber.")
        }

        val sale = saleRepository.findById(organizationId, command.saleId.required("Sale id"))
            ?: throw DomainRuleViolation("Sale does not exist.")
        if (!command.allowDuplicate) {
            documentRepository.findBySale(organizationId, sale.id)
                .firstOrNull { it.documentType == DocumentType.PHYSICAL_SALE_NOTE_REGISTRY }
                ?.let { existing ->
                    return CommercialDocumentResult(
                        existing,
                        existing.pdfObjectKey?.let(fileStorage::get)
                    )
                }
        }

        val draft = CommercialDocument.draftFromSale(
            id = idGenerator.newId("doc"),
            sale = sale,
            documentType = DocumentType.PHYSICAL_SALE_NOTE_REGISTRY,
            documentNumber = physicalNumber,
            emissionPointId = command.emissionPointId,
            issuedAt = now,
            createdBy = command.actorUserId,
            notes = command.notes,
        )
        documentRepository.create(draft)
        val file = fileStorage.put(pdfRenderer.render(draft))
        val generated = draft.markGenerated(
            payloadId = null,
            pdfObjectKey = file.objectKey,
            updatedAt = now,
            updatedBy = command.actorUserId
        )
        documentRepository.update(generated)
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.PHYSICAL_SALE_NOTE_REGISTERED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = generated.id,
                saleId = sale.id,
                after = generated.toAuditMap(),
                createdAt = Instant.now(clock),
            )
        )
        return CommercialDocumentResult(generated, file)
    }
}

class GetCommercialDocumentUseCase(
    private val documentRepository: CommercialDocumentRepository,
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetCommercialDocumentCommand): CommercialDocumentResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_VIEW)
        val document = documentRepository.findById(
            command.organizationId.required("Organization id"),
            command.documentId.required("Document id")
        )
            ?: throw DomainRuleViolation("Commercial document does not exist.")
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.COMMERCIAL_DOCUMENT_VIEWED,
                actorUserId = command.actorUserId,
                organizationId = document.organizationId,
                targetId = document.id,
                saleId = document.saleId,
                createdAt = Instant.now(clock),
            )
        )
        return CommercialDocumentResult(document)
    }
}

class SearchCommercialDocumentsUseCase(
    private val documentRepository: CommercialDocumentRepository,
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: SearchCommercialDocumentsCommand): CommercialDocumentsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_VIEW)
        val query = CommercialDocumentSearchQuery(
            organizationId = command.organizationId.required("Organization id"),
            saleId = command.saleId?.trim()?.takeIf { it.isNotBlank() },
            documentType = command.documentType,
            statuses = command.statuses,
            from = command.from,
            to = command.to,
            limit = command.limit,
        )
        val documents = documentRepository.search(query)
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.COMMERCIAL_DOCUMENT_LISTED,
                actorUserId = command.actorUserId,
                organizationId = query.organizationId,
                targetId = null,
                after = mapOf("resultCount" to documents.size.toString()),
                createdAt = Instant.now(clock),
            )
        )
        return CommercialDocumentsResult(documents)
    }
}

class DownloadCommercialDocumentPdfUseCase(
    private val documentRepository: CommercialDocumentRepository,
    private val fileStorage: CommercialDocumentFileStorage,
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: DownloadCommercialDocumentPdfCommand): CommercialDocumentResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF)
        val document = documentRepository.findById(
            command.organizationId.required("Organization id"),
            command.documentId.required("Document id")
        )
            ?: throw DomainRuleViolation("Commercial document does not exist.")
        val objectKey =
            document.pdfObjectKey ?: throw DomainRuleViolation("Commercial document does not have a generated PDF.")
        val file = fileStorage.get(objectKey)
            ?: throw DomainRuleViolation("Commercial document PDF file does not exist in storage.")
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.COMMERCIAL_DOCUMENT_PDF_DOWNLOADED,
                actorUserId = command.actorUserId,
                organizationId = document.organizationId,
                targetId = document.id,
                saleId = document.saleId,
                createdAt = Instant.now(clock),
            )
        )
        return CommercialDocumentResult(document, file)
    }
}

class EmailCommercialDocumentUseCase(
    private val documentRepository: CommercialDocumentRepository,
    private val fileStorage: CommercialDocumentFileStorage,
    private val emailSender: CommercialDocumentEmailSender = NoopCommercialDocumentEmailSender,
    private val auditLogger: CommercialDocumentAuditLogger = NoopCommercialDocumentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: EmailCommercialDocumentCommand): EmailCommercialDocumentResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF)
        val now = Instant.now(clock)
        val document = documentRepository.findById(
            command.organizationId.required("Organization id"),
            command.documentId.required("Document id")
        )
            ?: throw DomainRuleViolation("Commercial document does not exist.")
        val objectKey =
            document.pdfObjectKey ?: throw DomainRuleViolation("Commercial document does not have a generated PDF.")
        val file = fileStorage.get(objectKey)
            ?: throw DomainRuleViolation("Commercial document PDF file does not exist in storage.")
        val recipient = command.emailTo.required("Email recipient")
        val delivered = emailSender.send(
            CommercialDocumentEmail(
                to = recipient,
                subject = command.subject?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Documento ${document.documentNumber}",
                message = command.message?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Adjuntamos el documento ${document.documentNumber}.",
                attachment = file,
                document = document,
            )
        )
        val updated = if (delivered) document.markEmailSent(recipient, now, command.actorUserId) else document
        if (delivered) documentRepository.update(updated)
        auditLogger.log(
            CommercialDocumentAuditEvent(
                action = CommercialDocumentAuditAction.COMMERCIAL_DOCUMENT_EMAIL_SENT,
                actorUserId = command.actorUserId,
                organizationId = document.organizationId,
                targetId = document.id,
                saleId = document.saleId,
                after = mapOf("emailTo" to recipient, "delivered" to delivered.toString()),
                createdAt = now,
            )
        )
        return EmailCommercialDocumentResult(updated, delivered)
    }
}

internal fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun CommercialDocument.toAuditMap(): Map<String, String?> = mapOf(
    "documentType" to documentType.storageValue,
    "documentNumber" to documentNumber,
    "status" to status.name,
    "saleId" to saleId,
    "grandTotal" to totalsSnapshot.grandTotal.amount.toPlainString(),
    "currency" to totalsSnapshot.currency.value,
    "pdfObjectKey" to pdfObjectKey,
)
