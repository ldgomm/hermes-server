package com.hermes.application.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class XmlSigningModelsTest {
    @Test
    fun `rejects empty xml`() {
        assertFailsWith<DomainRuleViolation> {
            SignXmlCommand(
                organizationId = "org_1",
                signatureId = "sig_1",
                xml = ByteArray(0),
            )
        }
    }

    @Test
    fun `rejects invalid access key when provided`() {
        assertFailsWith<DomainRuleViolation> {
            SignXmlCommand(
                organizationId = "org_1",
                signatureId = "sig_1",
                xml = "<factura/>".toByteArray(),
                accessKey = "123",
            )
        }
    }
}
