package com.hermes.infrastructure.security

import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.testing.electronicinvoicing.TestPkcs12Fixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Pkcs12SigningKeyMaterialLoaderTest {
    private val loader = Pkcs12SigningKeyMaterialLoader()

    @Test
    fun `loads private key and certificate from pkcs12`() {
        val material = loader.loadPkcs12(TestPkcs12Fixture.content(), TestPkcs12Fixture.password())

        assertTrue(material.alias.isNotBlank())
        assertTrue(material.certificate.subjectX500Principal.name.contains("Hermes Test Certificate"))
        assertEquals("RSA", material.privateKey.algorithm)
        assertTrue(material.certificateChain.isNotEmpty())
        assertEquals(64, material.certificateFingerprintSha256.length)
    }

    @Test
    fun `rejects invalid password`() {
        assertFailsWith<Exception> {
            loader.loadPkcs12(TestPkcs12Fixture.content(), "wrong".toCharArray())
        }
    }

    @Test
    fun `rejects empty content`() {
        assertFailsWith<DomainRuleViolation> {
            loader.loadPkcs12(ByteArray(0), TestPkcs12Fixture.password())
        }
    }
}
