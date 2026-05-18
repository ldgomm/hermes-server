package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class SriEnvironmentTest {
    @Test
    fun `parses test and production environment codes`() {
        assertEquals(SriEnvironment.TEST, SriEnvironment.fromCode("1"))
        assertEquals(SriEnvironment.PRODUCTION, SriEnvironment.fromCode("2"))
        assertTrue(SriEnvironment.PRODUCTION.isProduction)
        assertFalse(SriEnvironment.TEST.isProduction)
    }

    @Test
    fun `rejects unknown environment code`() {
        assertFailsWith<DomainRuleViolation> {
            SriEnvironment.fromCode("9")
        }
    }
}
