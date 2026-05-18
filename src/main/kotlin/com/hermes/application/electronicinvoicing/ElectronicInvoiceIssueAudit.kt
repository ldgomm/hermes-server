package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import java.time.Instant

enum class ElectronicInvoiceIssueAuditAction {
    ELECTRONIC_DOCUMENT_CREATED,
    SRI_ACCESS_KEY_GENERATED,
    ELECTRONIC_XML_GENERATED,
    ELECTRONIC_XML_XSD_VALIDATED,
    ELECTRONIC_XML_XSD_INVALID,
    ELECTRONIC_XML_SIGNED,
    ELECTRONIC_XML_SIGNATURE_FAILED,
    SRI_RECEPTION_SUBMITTED,
    SRI_RECEPTION_RECEIVED,
    SRI_RECEPTION_RETURNED,
    SRI_AUTHORIZATION_QUERIED,
    SRI_AUTHORIZED,
    SRI_NOT_AUTHORIZED,
    SRI_AUTHORIZATION_PROCESSING
}

data class ElectronicInvoiceIssueAuditEvent(
    val action: ElectronicInvoiceIssueAuditAction,
    val actorUserId: String?,
    val organizationId: String,
    val documentId: String,
    val saleId: String?,
    val accessKey: String?,
    val status: ElectronicDocumentStatus?,
    val message: String? = null,
    val createdAt: Instant,
)

interface ElectronicInvoiceIssueAuditLogger {
    fun log(event: ElectronicInvoiceIssueAuditEvent)
}

object NoopElectronicInvoiceIssueAuditLogger : ElectronicInvoiceIssueAuditLogger {
    override fun log(event: ElectronicInvoiceIssueAuditEvent) = Unit
}
