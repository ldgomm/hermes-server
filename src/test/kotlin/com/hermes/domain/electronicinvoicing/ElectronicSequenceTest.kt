package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ElectronicSequenceTest {
    private val now = Instant.parse("2026-05-18T10:00:00Z")

    @Test
    fun `new sequence starts at zero and previews first sri sequential`() {
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            now = now,
        )

        assertEquals(0, sequence.currentValue)
        assertNull(sequence.lastIssuedSequential)
        assertEquals("000000001", sequence.nextSequential().formatted)
        assertEquals("org_test:test:electronic_invoice:001:002", sequence.key.storageKey)
    }

    @Test
    fun `mark issued advances sequence in strict order`() {
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            now = now,
        )

        val updated = sequence.markIssued(
            sequential = SriSequential(1),
            documentId = "doc_001",
            issuedAt = now.plusSeconds(1),
        )

        assertEquals(1, updated.currentValue)
        assertEquals("000000001", updated.lastIssuedSequential!!.formatted)
        assertEquals("doc_001", updated.lastIssuedDocumentId)
        assertEquals(2, updated.version)
    }

    @Test
    fun `rejects issuing out of order`() {
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            now = now,
        )

        assertFailsWith<DomainRuleViolation> {
            sequence.markIssued(
                sequential = SriSequential(2),
                documentId = "doc_002",
                issuedAt = now.plusSeconds(1),
            )
        }
    }

    @Test
    fun `rejects next sequential when exhausted`() {
        val sequence = ElectronicSequence.create(
            id = "eseq_001",
            organizationId = "org_test",
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = SriSeries("001", "002"),
            startsAfter = SriSequential.MAX_VALUE - 1,
            now = now,
        ).markIssued(
            sequential = SriSequential(SriSequential.MAX_VALUE),
            documentId = "doc_last",
            issuedAt = now.plusSeconds(1),
        )

        assertFailsWith<DomainRuleViolation> { sequence.nextSequential() }
    }
}
