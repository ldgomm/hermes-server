package com.hermes.domain.document

import com.hermes.application.sales.confirmedSale
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CommercialDocumentRulesTest {
    @Test
    fun `phase 10 document rejects electronic invoice type`() {
        assertFailsWith<DomainRuleViolation> {
            CommercialDocument.draftFromSale(
                id = "doc_1",
                sale = confirmedSale(),
                documentType = DocumentType.ELECTRONIC_INVOICE,
                documentNumber = "001-001-000000001",
                issuedAt = Instant.parse("2026-05-18T12:00:00Z"),
                createdBy = "usr_1",
            )
        }
    }
}
