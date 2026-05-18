package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SriStatusParsingTest {
    @Test
    fun `parses reception statuses`() {
        assertEquals(SriReceptionStatus.RECEIVED, SriReceptionStatus.fromSriValue("RECIBIDA"))
        assertEquals(SriReceptionStatus.RETURNED, SriReceptionStatus.fromSriValue("DEVUELTA"))
        assertTrue(SriReceptionStatus.RECEIVED.canQueryAuthorization)
    }

    @Test
    fun `parses authorization statuses from textual and compact values`() {
        assertEquals(SriAuthorizationStatus.AUTHORIZED, SriAuthorizationStatus.fromSriValue("AUTORIZADO"))
        assertEquals(SriAuthorizationStatus.AUTHORIZED, SriAuthorizationStatus.fromSriValue("AUT"))
        assertEquals(SriAuthorizationStatus.NOT_AUTHORIZED, SriAuthorizationStatus.fromSriValue("RECHAZADO"))
        assertEquals(SriAuthorizationStatus.NOT_AUTHORIZED, SriAuthorizationStatus.fromSriValue("NAT"))
        assertEquals(SriAuthorizationStatus.PROCESSING, SriAuthorizationStatus.fromSriValue("PPR"))
        assertEquals(SriAuthorizationStatus.PROCESSING, SriAuthorizationStatus.fromSriValue("EN PROCESAMIENTO"))
    }

    @Test
    fun `rejects unknown status`() {
        assertFailsWith<DomainRuleViolation> {
            SriReceptionStatus.fromSriValue("OK")
        }
        assertFailsWith<DomainRuleViolation> {
            SriAuthorizationStatus.fromSriValue("OK")
        }
    }
}
