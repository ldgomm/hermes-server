package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactType
import com.hermes.application.electronicinvoicing.StoreElectronicDocumentArtifactCommand
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.core.M029CreateElectronicInvoicingPersistenceMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class MongoElectronicDocumentArtifactStorageTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("electronic_document_artifacts_test")
        M029CreateElectronicInvoicingPersistenceMigration.up(client.getDatabase(databaseName))
    }

    @AfterTest
    fun tearDown() {
        if (::client.isInitialized) {
            client.getDatabase(databaseName).drop()
            client.close()
        }
    }

    @Test
    fun `stores XML artifact in filesystem and metadata in Mongo`() {
        val database = client.getDatabase(databaseName)
        val root = createTempDirectory("hermes-electronic-artifacts-")
        val storage = MongoElectronicDocumentArtifactStorage(database, root)
        val now = Instant.parse("2026-05-18T10:00:00Z")

        val stored = storage.put(
            StoreElectronicDocumentArtifactCommand(
                organizationId = "org_test",
                documentId = "edoc_001",
                artifactType = ElectronicDocumentArtifactType.SIGNED_XML,
                content = "<factura><ds:Signature>fake</ds:Signature></factura>".toByteArray(Charsets.UTF_8),
                contentType = "application/xml; charset=UTF-8",
                fileName = "edoc_001_signed.xml",
                createdAt = now,
            )
        )

        assertEquals(ElectronicDocumentArtifactType.SIGNED_XML, stored.artifactType)
        assertEquals(64, stored.sha256.length)
        assertTrue(Files.exists(root.resolve(stored.objectKey)))

        val metadata = database
            .getCollection(ElectronicInvoicingMongoCollectionNames.ELECTRONIC_DOCUMENT_ARTIFACTS)
            .find(Filters.eq(MongoDocumentFields.ID, stored.objectKey))
            .firstOrNull()

        assertNotNull(metadata)
        assertEquals("org_test", metadata.getString(MongoDocumentFields.ORGANIZATION_ID))
        assertEquals("edoc_001", metadata.getString("documentId"))
        assertEquals("signed_xml", metadata.getString("artifactType"))
        assertEquals(stored.sha256, metadata.getString("sha256"))
    }
}