package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class SriDocumentTypeTest {
    @Test
    fun `parses supported SRI document type codes`() {
        assertEquals(SriDocumentType.INVOICE, SriDocumentType.fromCode("01"))
        assertEquals(SriDocumentType.CREDIT_NOTE, SriDocumentType.fromCode("04"))
        assertEquals(SriDocumentType.WITHHOLDING, SriDocumentType.fromStorage("withholding"))
    }

    @Test
    fun `only invoice is MVP supported`() {
        assertTrue(SriDocumentType.INVOICE.isMvpSupported)
        assertFalse(SriDocumentType.CREDIT_NOTE.isMvpSupported)

        assertFailsWith<DomainRuleViolation> {
            SriDocumentType.CREDIT_NOTE.assertMvpSupported()
        }
    }
}
