package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueSearchQuery
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerationCommand
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerator
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.infrastructure.mongo.migration.core.M029CreateElectronicInvoicingPersistenceMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.time.LocalDate

class MongoElectronicInvoiceIssueRepositorySearchTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("electronic_invoice_issues_search_test")
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
    fun `search filters by organization sale status and environment`() {
        val repository = MongoElectronicInvoiceIssueRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-18T10:00:00Z")

        repository.create(mongoSearchIssueRecord(id = "edoc_1", organizationId = "org_test", saleId = "sale_1", sequential = 1, now = now))
        repository.create(
            mongoSearchIssueRecord(id = "edoc_2", organizationId = "org_test", saleId = "sale_1", sequential = 2, now = now.plusSeconds(10))
                .copy(status = ElectronicDocumentStatus.AUTHORIZED, authorizedAt = now.plusSeconds(15))
        )
        repository.create(
            mongoSearchIssueRecord(id = "edoc_other_org", organizationId = "org_other", saleId = "sale_1", sequential = 3, now = now.plusSeconds(20))
                .copy(status = ElectronicDocumentStatus.AUTHORIZED, authorizedAt = now.plusSeconds(25))
        )

        val result = repository.search(
            ElectronicInvoiceIssueSearchQuery(
                organizationId = "org_test",
                saleId = "sale_1",
                statuses = setOf(ElectronicDocumentStatus.AUTHORIZED),
                environment = SriEnvironment.TEST,
                limit = 20,
            )
        )

        assertEquals(listOf("edoc_2"), result.map { it.id })
    }
}

private fun mongoSearchIssueRecord(
    id: String,
    organizationId: String,
    saleId: String,
    sequential: Int,
    now: Instant,
): com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord {
    val series = SriSeries("001", "002")
    val accessKey = SriAccessKeyGenerator.generate(
        SriAccessKeyGenerationCommand(
            issuedDate = LocalDate.of(2026, 5, 18),
            documentType = SriDocumentType.INVOICE,
            ruc = "1790012345001",
            environment = SriEnvironment.TEST,
            series = series,
            sequential = SriSequential(sequential),
            numericCode = SriNumericCode("12345678"),
        )
    )

    return com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord.accessKeyGenerated(
        id = id,
        organizationId = organizationId,
        branchId = "br_test",
        emissionPointId = "emi_test",
        saleId = saleId,
        environment = SriEnvironment.TEST,
        documentType = SriDocumentType.INVOICE,
        series = series,
        documentNumber = "001-002-${sequential.toString().padStart(9, '0')}",
        accessKey = accessKey,
        authorizationNumber = accessKey.value,
        issuedAt = now,
        actorUserId = "usr_test",
    )
}
