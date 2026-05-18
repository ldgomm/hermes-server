package com.hermes.infrastructure.sri

import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SriWsConfigTest {
    @Test
    fun `resolves official test endpoints`() {
        val config = SriWsConfig()

        assertEquals(SriWsConfig.DEFAULT_TEST_RECEPTION_URL, config.receptionUrl(SriEnvironment.TEST))
        assertEquals(SriWsConfig.DEFAULT_TEST_AUTHORIZATION_URL, config.authorizationUrl(SriEnvironment.TEST))
    }

    @Test
    fun `resolves official production endpoints`() {
        val config = SriWsConfig()

        assertEquals(SriWsConfig.DEFAULT_PRODUCTION_RECEPTION_URL, config.receptionUrl(SriEnvironment.PRODUCTION))
        assertEquals(
            SriWsConfig.DEFAULT_PRODUCTION_AUTHORIZATION_URL,
            config.authorizationUrl(SriEnvironment.PRODUCTION)
        )
    }

    @Test
    fun `rejects non https endpoints`() {
        val error = assertFailsWith<DomainRuleViolation> {
            SriWsConfig(testReceptionUrl = "http://example.com?wsdl")
        }

        assertTrue(error.message!!.contains("HTTPS"))
    }
}
