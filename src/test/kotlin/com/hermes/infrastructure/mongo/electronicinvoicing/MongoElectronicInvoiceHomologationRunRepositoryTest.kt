package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.infrastructure.mongo.migration.core.M032CreateElectronicInvoicingProductionGateMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import java.time.Instant
import kotlin.test.*

class MongoElectronicInvoiceHomologationRunRepositoryTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("electronic_homologation_runs_test")
        M032CreateElectronicInvoicingProductionGateMigration.up(client.getDatabase(databaseName))
    }

    @AfterTest
    fun tearDown() {
        if (::client.isInitialized) {
            client.getDatabase(databaseName).drop()
            client.close()
        }
    }

    @Test
    fun `creates finds searches and returns latest approved homologation run`() {
        val repository = MongoElectronicInvoiceHomologationRunRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-19T10:00:00Z")
        val run = mongoHomologationRun12ETest(now)

        repository.create(run)

        val found = repository.findById("org_test", "homologation_run_001")
        assertNotNull(found)
        assertEquals(ElectronicInvoiceHomologationRunStatus.PASSED, found.status)
        assertEquals(true, found.approvedForProduction)
        assertEquals("# Reporte aprobado", found.reportMarkdown)

        val search = repository.search(
            ElectronicInvoiceHomologationRunSearchQuery(
                organizationId = "org_test",
                statuses = setOf(ElectronicInvoiceHomologationRunStatus.PASSED),
                limit = 10,
            )
        )
        assertEquals(1, search.size)
        assertEquals("homologation_run_001", search.first().id)

        val latest = repository.findLatestApprovedForProduction("org_test")
        assertNotNull(latest)
        assertEquals("homologation_run_001", latest.id)
        assertTrue(latest.productionDecision?.approved == true)
    }

    private fun mongoHomologationRun12ETest(now: Instant): ElectronicInvoiceHomologationRun =
        ElectronicInvoiceHomologationRun(
            id = "homologation_run_001",
            organizationId = "org_test",
            status = ElectronicInvoiceHomologationRunStatus.PASSED,
            environment = SriEnvironment.TEST,
            requestedByUserId = "usr_admin",
            requiredScenarioCodes = setOf(ElectronicInvoiceHomologationScenarioCode.FINAL_CONSUMER),
            scenarioResults = emptyList(),
            reportMarkdown = "# Reporte aprobado",
            productionDecision = ElectronicInvoiceProductionReadinessDecision(
                approved = true,
                environment = SriEnvironment.TEST,
                reasons = listOf("All required scenarios passed."),
                decidedAt = now,
            ),
            approvedForProduction = true,
            startedAt = now.minusSeconds(60),
            finishedAt = now,
            createdAt = now,
            updatedAt = now,
        )
}
