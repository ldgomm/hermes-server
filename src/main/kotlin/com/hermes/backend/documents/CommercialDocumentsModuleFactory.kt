package com.hermes.backend.documents

import com.hermes.application.documents.*
import com.hermes.infrastructure.mongo.documents.FileSystemCommercialDocumentFileStorage
import com.hermes.infrastructure.mongo.documents.MongoCommercialDocumentAuditLogger
import com.hermes.infrastructure.mongo.documents.MongoCommercialDocumentStore
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.mongodb.client.MongoDatabase
import java.nio.file.Path
import java.time.Clock

object CommercialDocumentsModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        storageRoot: Path = Path.of("build/hermes-commercial-documents"),
        clock: Clock = Clock.systemUTC(),
    ): CommercialDocumentsModule {
        val documentsStore = MongoCommercialDocumentStore(database)
        val salesStore = MongoSalesStore(database)
        val storage = FileSystemCommercialDocumentFileStorage(storageRoot)
        val renderer = SimpleCommercialDocumentPdfRenderer()
        val idGenerator = UuidCommercialDocumentIdGenerator()
        val auditLogger = MongoCommercialDocumentAuditLogger(database)

        return CommercialDocumentsModule(
            generateInternalTicketUseCase = GenerateInternalTicketUseCase(
                saleRepository = salesStore.saleRepository,
                documentRepository = documentsStore.documentRepository,
                numberGenerator = documentsStore.numberGenerator,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                auditLogger = auditLogger,
                clock = clock,
            ),
            registerPhysicalSaleNoteUseCase = RegisterPhysicalSaleNoteUseCase(
                saleRepository = salesStore.saleRepository,
                documentRepository = documentsStore.documentRepository,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                auditLogger = auditLogger,
                clock = clock,
            ),
            getCommercialDocumentUseCase = GetCommercialDocumentUseCase(
                documentRepository = documentsStore.documentRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchCommercialDocumentsUseCase = SearchCommercialDocumentsUseCase(
                documentRepository = documentsStore.documentRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            downloadCommercialDocumentPdfUseCase = DownloadCommercialDocumentPdfUseCase(
                documentRepository = documentsStore.documentRepository,
                fileStorage = storage,
                auditLogger = auditLogger,
                clock = clock,
            ),
            emailCommercialDocumentUseCase = EmailCommercialDocumentUseCase(
                documentRepository = documentsStore.documentRepository,
                fileStorage = storage,
                emailSender = NoopCommercialDocumentEmailSender,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )
    }
}
