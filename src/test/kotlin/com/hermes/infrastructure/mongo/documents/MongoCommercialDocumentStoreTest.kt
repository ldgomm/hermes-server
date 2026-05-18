package com.hermes.infrastructure.mongo.documents

import com.hermes.application.sales.confirmedSale
import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.document.DocumentType
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.core.M014CreateCommercialDocumentsMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import org.bson.Document
import org.bson.types.Decimal128
import java.time.Instant
import kotlin.test.*

class MongoCommercialDocumentStoreTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_commercial_documents_store_test")
        M014CreateCommercialDocumentsMigration.up(client.getDatabase(databaseName))
    }

    @AfterTest
    fun tearDown() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `stores searches and reads generated commercial document preserving Decimal128 totals`() {
        val database = client.getDatabase(databaseName)
        val store = MongoCommercialDocumentStore(database)
        val document = generatedDocument()

        store.documentRepository.create(document)

        val byId = store.documentRepository.findById("org_1", "doc_1")
        val bySale = store.documentRepository.findBySale("org_1", "sale_1")
        val search = store.documentRepository.search(
            com.hermes.application.documents.CommercialDocumentSearchQuery(
                organizationId = "org_1",
                documentType = DocumentType.INTERNAL_TICKET,
                statuses = setOf(DocumentStatus.GENERATED),
            )
        )

        assertNotNull(byId)
        assertEquals(document.id, byId!!.id)
        assertEquals(DocumentType.INTERNAL_TICKET, byId.documentType)
        assertEquals(DocumentStatus.GENERATED, byId.status)
        assertEquals(document.totalsSnapshot.grandTotal, byId.totalsSnapshot.grandTotal)
        assertEquals(1, bySale.size)
        assertEquals(1, search.size)

        val raw = database.getCollection(MongoCollectionNames.COMMERCIAL_DOCUMENTS)
            .find(Document(MongoDocumentFields.ID, "doc_1"))
            .first() ?: error("Expected persisted commercial document.")
        val totals = raw.get("totalsSnapshot", Document::class.java)
        val subtotal = totals.get("subtotal", Document::class.java).get("amount")
        val lineSnapshots = raw.getList("lineSnapshots", Document::class.java)

        assertTrue(subtotal is Decimal128)
        assertTrue(lineSnapshots.isNotEmpty())
    }

    @Test
    fun `counter generator increments ticket sequence by organization and branch`() {
        val store = MongoCommercialDocumentStore(client.getDatabase(databaseName))

        val first = store.numberGenerator.nextInternalTicketNumber("org_1", "br_1", NOW)
        val second = store.numberGenerator.nextInternalTicketNumber("org_1", "br_1", NOW.plusSeconds(60))
        val otherBranch = store.numberGenerator.nextInternalTicketNumber("org_1", "br_2", NOW)

        assertEquals("TCK-2026-05-18-000000001", first)
        assertEquals("TCK-2026-05-18-000000002", second)
        assertEquals("TCK-2026-05-18-000000001", otherBranch)
    }

    private fun generatedDocument(): CommercialDocument {
        val sale = confirmedSale()
        val draft = CommercialDocument.draftFromSale(
            id = "doc_1",
            sale = sale,
            documentType = DocumentType.INTERNAL_TICKET,
            documentNumber = "TCK-2026-05-18-000000001",
            emissionPointId = "ep_1",
            issuedAt = NOW,
            createdBy = "usr_1",
            notes = "Documento generado por test Mongo",
        )
        return draft.markGenerated(
            payloadId = null,
            pdfObjectKey = "commercial-documents/org_1/doc_1.pdf",
            updatedAt = NOW,
            updatedBy = "usr_1",
        )
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}
