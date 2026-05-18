package com.hermes.application.documents

import java.time.Instant

enum class CommercialDocumentAuditAction {
    INTERNAL_TICKET_GENERATED,
    PHYSICAL_SALE_NOTE_REGISTERED,
    COMMERCIAL_DOCUMENT_VIEWED,
    COMMERCIAL_DOCUMENT_LISTED,
    COMMERCIAL_DOCUMENT_PDF_DOWNLOADED,
    COMMERCIAL_DOCUMENT_EMAIL_SENT,
}

data class CommercialDocumentAuditEvent(
    val action: CommercialDocumentAuditAction,
    val actorUserId: String?,
    val organizationId: String,
    val targetId: String?,
    val saleId: String? = null,
    val before: Map<String, String?> = emptyMap(),
    val after: Map<String, String?> = emptyMap(),
    val reason: String? = null,
    val createdAt: Instant,
)

interface CommercialDocumentAuditLogger {
    fun log(event: CommercialDocumentAuditEvent)
}

object NoopCommercialDocumentAuditLogger : CommercialDocumentAuditLogger {
    override fun log(event: CommercialDocumentAuditEvent) = Unit
}
