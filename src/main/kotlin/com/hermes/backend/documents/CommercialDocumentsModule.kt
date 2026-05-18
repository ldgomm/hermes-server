package com.hermes.backend.documents

import com.hermes.application.documents.*

data class CommercialDocumentsModule(
    val generateInternalTicketUseCase: GenerateInternalTicketUseCase,
    val registerPhysicalSaleNoteUseCase: RegisterPhysicalSaleNoteUseCase,
    val getCommercialDocumentUseCase: GetCommercialDocumentUseCase,
    val searchCommercialDocumentsUseCase: SearchCommercialDocumentsUseCase,
    val downloadCommercialDocumentPdfUseCase: DownloadCommercialDocumentPdfUseCase,
    val emailCommercialDocumentUseCase: EmailCommercialDocumentUseCase,
)
