package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.domain.electronicinvoicing.*
import com.hermes.infrastructure.mongo.migration.core.M029CreateElectronicInvoicingPersistenceMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import java.time.Instant
import java.time.LocalDate
import kotlin.test.*

class MongoElectronicInvoiceIssueRepositoryTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("electronic_invoice_issues_test")
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
    fun `creates updates and finds electronic invoice issue record`() {
        val repository = MongoElectronicInvoiceIssueRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-18T10:00:00Z")
        val record = issueRecord(now)

        repository.create(record)

        val found = repository.findById("org_test", "edoc_001")
        assertNotNull(found)
        assertEquals(ElectronicDocumentStatus.ACCESS_KEY_GENERATED, found.status)
        assertEquals("001-002-000000001", found.documentNumber)
        assertEquals(false, repository.existsAuthorizedInvoiceForSale("org_test", "sale_001"))

        val authorized = found.copy(
            status = ElectronicDocumentStatus.AUTHORIZED,
            signedXmlObjectKey = "electronic-invoicing/org_test/edoc_001/signed_xml/signed.xml",
            signedXmlSha256 = "a".repeat(64),
            authorizedXmlObjectKey = "electronic-invoicing/org_test/edoc_001/authorized_xml/authorized.xml",
            authorizedXmlSha256 = "b".repeat(64),
            lastSriReceptionStatus = "received",
            lastSriAuthorizationStatus = "authorized",
            authorizedAt = now.plusSeconds(5),
            updatedAt = now.plusSeconds(5),
            updatedBy = "usr_test",
            version = found.version + 1,
        )

        repository.update(authorized)

        val persisted = repository.findById("org_test", "edoc_001")
        assertNotNull(persisted)
        assertEquals(ElectronicDocumentStatus.AUTHORIZED, persisted.status)
        assertEquals("b".repeat(64), persisted.authorizedXmlSha256)
        assertTrue(repository.existsAuthorizedInvoiceForSale("org_test", "sale_001"))
    }

    private fun issueRecord(now: Instant): ElectronicInvoiceIssueRecord {
        val series = SriSeries("001", "002")
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.of(2026, 5, 18),
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = SriSequential(1),
                numericCode = SriNumericCode("12345678"),
            )
        )

        return ElectronicInvoiceIssueRecord.accessKeyGenerated(
            id = "edoc_001",
            organizationId = "org_test",
            branchId = "br_test",
            emissionPointId = "emi_test",
            saleId = "sale_001",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = series,
            documentNumber = "001-002-000000001",
            accessKey = accessKey,
            authorizationNumber = accessKey.value,
            issuedAt = now,
            actorUserId = "usr_test",
        )
    }
}