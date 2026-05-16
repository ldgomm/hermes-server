package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CommercialDocumentRepositoryIntegrationTest {
    @Test
    fun `commercial document repository queries documents by sale number and access key`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_commercial_document_repository") { database ->
            val documents = CommercialDocumentRepository(database)

            documents.insert(RepositoryTestSupport.commercialDocument())
            documents.insert(
                RepositoryTestSupport.commercialDocument(
                    id = "doc_phase43_ticket",
                    documentNumber = "TCK-000001",
                    accessKey = null,
                    status = "generated",
                ).append("documentType", "internal_ticket")
            )

            assertEquals(
                setOf(RepositoryTestSupport.DOCUMENT_ID, "doc_phase43_ticket"),
                documents.findBySale(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.SALE_ID,
                ).map { it.getString("_id") }.toSet()
            )

            assertEquals(
                RepositoryTestSupport.DOCUMENT_ID,
                documents.findByAccessKey(RepositoryTestSupport.ACCESS_KEY)?.getString("_id")
            )
            assertEquals(
                RepositoryTestSupport.DOCUMENT_ID,
                documents.findByDocumentNumber(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "001-001-000000001",
                )?.getString("_id")
            )
        }
    }

    @Test
    fun `electronic payload and sri submission repositories query integration records`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_document_payload_repository") { database ->
            val payloads = ElectronicDocumentPayloadRepository(database)
            val submissions = SriSubmissionRepository(database)

            payloads.insert(RepositoryTestSupport.electronicPayload())
            submissions.insert(
                RepositoryTestSupport.sriSubmission(
                    id = "sri_phase43_001",
                    status = "sent",
                    requestAt = Instant.parse("2026-05-15T12:22:00Z"),
                )
            )
            submissions.insert(
                RepositoryTestSupport.sriSubmission(
                    id = "sri_phase43_002",
                    status = "authorized",
                    requestAt = Instant.parse("2026-05-15T12:23:00Z"),
                )
            )

            assertEquals(
                RepositoryTestSupport.PAYLOAD_ID,
                payloads.findByDocumentId(RepositoryTestSupport.DOCUMENT_ID)?.getString("_id")
            )

            assertEquals(
                listOf("sri_phase43_002", "sri_phase43_001"),
                submissions.findByPayloadId(RepositoryTestSupport.PAYLOAD_ID)
                    .map { it.getString("_id") }
            )

            assertEquals(
                listOf("sri_phase43_002", "sri_phase43_001"),
                submissions.findByAccessKey(RepositoryTestSupport.ACCESS_KEY)
                    .map { it.getString("_id") }
            )
        }
    }
}
