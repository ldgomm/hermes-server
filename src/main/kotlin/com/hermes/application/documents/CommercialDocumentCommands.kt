package com.hermes.application.documents

import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.document.DocumentType
import java.time.Instant

data class GenerateInternalTicketCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val emissionPointId: String? = null,
    val issuedAt: Instant? = null,
    val notes: String? = null,
    val allowDuplicate: Boolean = false,
)

data class RegisterPhysicalSaleNoteCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val physicalDocumentNumber: String,
    val emissionPointId: String? = null,
    val issuedAt: Instant? = null,
    val notes: String? = null,
    val allowDuplicate: Boolean = false,
)

data class GetCommercialDocumentCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class SearchCommercialDocumentsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val saleId: String? = null,
    val documentType: DocumentType? = null,
    val statuses: Set<DocumentStatus> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class DownloadCommercialDocumentPdfCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class EmailCommercialDocumentCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val emailTo: String,
    val subject: String? = null,
    val message: String? = null,
)
