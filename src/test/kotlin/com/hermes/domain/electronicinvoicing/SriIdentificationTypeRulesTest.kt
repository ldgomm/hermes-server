package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SriIdentificationTypeRulesTest {
    @Test
    fun `validates buyer RUC cedula passport and external identification`() {
        SriIdentificationType.RUC.assertValidFor("1790012345001")
        SriIdentificationType.CEDULA.assertValidFor("1712345678")
        SriIdentificationType.PASSPORT.assertValidFor("AB123456")
        SriIdentificationType.EXTERNAL_IDENTIFICATION.assertValidFor("EXT-001")
    }

    @Test
    fun `validates final consumer only for invoice`() {
        SriIdentificationType.FINAL_CONSUMER.assertValidFor(
            identification = SriIdentificationType.FINAL_CONSUMER_IDENTIFICATION,
            documentType = SriDocumentType.INVOICE,
        )

        assertFailsWith<DomainRuleViolation> {
            SriIdentificationType.FINAL_CONSUMER.assertValidFor(
                identification = SriIdentificationType.FINAL_CONSUMER_IDENTIFICATION,
                documentType = SriDocumentType.CREDIT_NOTE,
            )
        }
    }

    @Test
    fun `infers basic identification type`() {
        assertEquals(SriIdentificationType.FINAL_CONSUMER, SriIdentificationType.inferBasic("9999999999999"))
        assertEquals(SriIdentificationType.RUC, SriIdentificationType.inferBasic("1790012345001"))
        assertEquals(SriIdentificationType.CEDULA, SriIdentificationType.inferBasic("1712345678"))
        assertEquals(SriIdentificationType.PASSPORT, SriIdentificationType.inferBasic("AB123456"))
    }

    @Test
    fun `rejects invalid RUC and cedula format`() {
        assertFailsWith<DomainRuleViolation> {
            SriIdentificationType.RUC.assertValidFor("1790012345000")
        }
        assertFailsWith<DomainRuleViolation> {
            SriIdentificationType.CEDULA.assertValidFor("171234")
        }
    }
}
