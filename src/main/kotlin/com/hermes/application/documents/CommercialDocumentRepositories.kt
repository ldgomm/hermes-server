package com.hermes.application.documents

import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.document.DocumentType
import java.time.Instant

interface CommercialDocumentRepository {
    fun create(document: CommercialDocument)
    fun update(document: CommercialDocument)
    fun findById(organizationId: String, documentId: String): CommercialDocument?
    fun findBySale(organizationId: String, saleId: String): List<CommercialDocument>
    fun findByDocumentNumber(organizationId: String, documentNumber: String): CommercialDocument?
    fun search(query: CommercialDocumentSearchQuery): List<CommercialDocument>
}

data class CommercialDocumentSearchQuery(
    val organizationId: String,
    val saleId: String? = null,
    val documentType: DocumentType? = null,
    val statuses: Set<DocumentStatus> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

interface CommercialDocumentNumberGenerator {
    fun nextInternalTicketNumber(organizationId: String, branchId: String, issuedAt: Instant): String
}

interface CommercialDocumentFileStorage {
    fun put(file: CommercialDocumentFile): CommercialDocumentFile
    fun get(objectKey: String): CommercialDocumentFile?
}

interface CommercialDocumentPdfRenderer {
    fun render(document: CommercialDocument): CommercialDocumentFile
}

interface CommercialDocumentEmailSender {
    fun send(email: CommercialDocumentEmail): Boolean
}

data class CommercialDocumentEmail(
    val to: String,
    val subject: String,
    val message: String,
    val attachment: CommercialDocumentFile,
    val document: CommercialDocument,
)

object NoopCommercialDocumentEmailSender : CommercialDocumentEmailSender {
    override fun send(email: CommercialDocumentEmail): Boolean = true
}
