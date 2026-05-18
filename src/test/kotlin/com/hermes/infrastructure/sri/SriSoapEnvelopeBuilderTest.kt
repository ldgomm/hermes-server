package com.hermes.infrastructure.sri

import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue

class SriSoapEnvelopeBuilderTest {
    @Test
    fun `builds reception envelope with base64 xml`() {
        val xml = "<factura id=\"comprobante\"/>".toByteArray()
        val envelope = SriSoapEnvelopeBuilder.receptionEnvelope(xml)

        assertTrue(envelope.contains("validarComprobante"))
        assertTrue(envelope.contains(Base64.getEncoder().encodeToString(xml)))
    }

    @Test
    fun `builds authorization envelope with access key`() {
        val accessKey = testAccessKey().value
        val envelope = SriSoapEnvelopeBuilder.authorizationEnvelope(accessKey)

        assertTrue(envelope.contains("autorizacionComprobante"))
        assertTrue(envelope.contains("<claveAccesoComprobante>$accessKey</claveAccesoComprobante>"))
    }
}
