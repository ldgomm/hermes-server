package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.NextElectronicSequentialCommand
import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.infrastructure.mongo.migration.core.M021CreateElectronicSequencesMigration
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MongoElectronicSequenceRepositoryTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("electronic_sequences_test")
        M021CreateElectronicSequencesMigration.up(client.getDatabase(databaseName))
    }

    @AfterTest
    fun tearDown() {
        if (::client.isInitialized) {
            client.getDatabase(databaseName).drop()
            client.close()
        }
    }

    @Test
    fun `creates finds and advances electronic sequence`() {
        val repository = MongoElectronicSequenceRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-18T10:00:00Z")
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            now = now,
        )

        repository.createIfMissing(sequence)
        val found = repository.findByKey(sequence.key)

        assertNotNull(found)
        assertEquals(0, found.currentValue)

        val first = repository.nextSequential(
            NextElectronicSequentialCommand(
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = SriSeries("001", "002"),
                documentId = "doc_001",
                issuedAt = now.plusSeconds(1),
            )
        )

        assertEquals("000000001", first.sequential.formatted)
        assertEquals("001-002-000000001", first.documentNumber)
        assertEquals("doc_001", first.sequence.lastIssuedDocumentId)
        assertEquals(1, first.sequence.currentValue)
    }

    @Test
    fun `create if missing is idempotent`() {
        val repository = MongoElectronicSequenceRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-18T10:00:00Z")
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            startsAfter = 7,
            now = now,
        )

        val first = repository.createIfMissing(sequence)
        val second = repository.createIfMissing(sequence.copy(id = "eseq_other", currentValue = 99))

        assertEquals(first.id, second.id)
        assertEquals(7, second.currentValue)
    }

    @Test
    fun `next sequential is atomic under concurrent reservations`() {
        val repository = MongoElectronicSequenceRepository(client.getDatabase(databaseName))
        val now = Instant.parse("2026-05-18T10:00:00Z")
        val series = SriSeries("001", "002")
        repository.createIfMissing(
            ElectronicSequence.create(
                id = "eseq_001",
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = series,
                now = now,
            )
        )

        val executor = Executors.newFixedThreadPool(8)
        val issued = Collections.synchronizedSet(mutableSetOf<Int>())
        val tasks = (1..40).map { index ->
            Callable {
                val result = repository.nextSequential(
                    NextElectronicSequentialCommand(
                        organizationId = "org_test",
                        environment = SriEnvironment.TEST,
                        documentType = SriDocumentType.INVOICE,
                        series = series,
                        documentId = "doc_$index",
                        issuedAt = now.plusSeconds(index.toLong()),
                    )
                )
                issued += result.sequential.value
            }
        }

        executor.invokeAll(tasks).forEach { it.get() }
        executor.shutdown()

        assertEquals((1..40).toSet(), issued.toSet())

        val persisted = repository.findByKey(
            ElectronicSequenceKey(
                organizationId = "org_test",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = series,
            )
        )

        assertNotNull(persisted)
        assertEquals(40, persisted.currentValue)
        assertTrue(persisted.version >= 41)
    }
}
