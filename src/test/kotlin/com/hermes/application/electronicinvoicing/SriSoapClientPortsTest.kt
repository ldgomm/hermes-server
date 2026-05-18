package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.infrastructure.sri.testAccessKey
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SriSoapClientPortsTest {
    @Test
    fun `reception command rejects empty xml`() {
        val error = assertFailsWith<DomainRuleViolation> {
            SriReceptionCommand(
                organizationId = "org_1",
                environment = SriEnvironment.TEST,
                signedXml = ByteArray(0),
            )
        }

        assertTrue(error.message!!.contains("Signed XML"))
    }

    @Test
    fun `authorization query rejects environment mismatch`() {
        val error = assertFailsWith<DomainRuleViolation> {
            SriAuthorizationQueryCommand(
                organizationId = "org_1",
                environment = SriEnvironment.PRODUCTION,
                accessKey = testAccessKey(SriEnvironment.TEST),
            )
        }

        assertTrue(error.message!!.contains("environment"))
    }
}
