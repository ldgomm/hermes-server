package com.hermes.domain.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus.*
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
        assertEquals(AUTHORIZED, SriAuthorizationStatus.fromSriValue("AUTORIZADO"))
        assertEquals(AUTHORIZED, SriAuthorizationStatus.fromSriValue("AUT"))
        assertEquals(NOT_AUTHORIZED, SriAuthorizationStatus.fromSriValue("RECHAZADO"))
        assertEquals(NOT_AUTHORIZED, SriAuthorizationStatus.fromSriValue("NAT"))
        assertEquals(PROCESSING, SriAuthorizationStatus.fromSriValue("PPR"))
        assertEquals(PROCESSING, SriAuthorizationStatus.fromSriValue("EN PROCESAMIENTO"))
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
