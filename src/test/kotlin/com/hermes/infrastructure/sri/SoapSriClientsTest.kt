package com.hermes.infrastructure.sri

import com.hermes.application.electronicinvoicing.SriAuthorizationQueryCommand
import com.hermes.application.electronicinvoicing.SriReceptionCommand
import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriReceptionStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoapSriClientsTest {
    private val accessKey = testAccessKey()

    @Test
    fun `reception client sends signed xml to SRI test endpoint`() {
        val transport = FakeSriSoapTransport(receivedReceptionResponse())
        val client = SoapSriReceptionClient(SriWsConfig(), transport)

        val result = client.submit(
            SriReceptionCommand(
                organizationId = "org_1",
                environment = SriEnvironment.TEST,
                signedXml = "<factura/>".toByteArray(),
                accessKey = accessKey,
            )
        )

        assertEquals(SriReceptionStatus.RECEIVED, result.status)
        assertEquals(SriWsConfig.DEFAULT_TEST_RECEPTION_URL, transport.lastCommand!!.endpointUrl)
        assertEquals(SoapSriReceptionClient.SOAP_ACTION, transport.lastCommand!!.soapAction)
        assertTrue(transport.lastCommand!!.body.contains("validarComprobante"))
    }

    @Test
    fun `authorization client queries SRI test endpoint by access key`() {
        val transport = FakeSriSoapTransport(authorizedResponse(accessKey.value))
        val client = SoapSriAuthorizationClient(SriWsConfig(), transport)

        val result = client.query(
            SriAuthorizationQueryCommand(
                organizationId = "org_1",
                environment = SriEnvironment.TEST,
                accessKey = accessKey,
            )
        )

        assertEquals(SriAuthorizationStatus.AUTHORIZED, result.status)
        assertEquals(SriWsConfig.DEFAULT_TEST_AUTHORIZATION_URL, transport.lastCommand!!.endpointUrl)
        assertEquals(SoapSriAuthorizationClient.SOAP_ACTION, transport.lastCommand!!.soapAction)
        assertTrue(transport.lastCommand!!.body.contains(accessKey.value))
    }

    @Test
    fun `reception client fails on non successful http status`() {
        val transport = FakeSriSoapTransport(receivedReceptionResponse(), statusCode = 503)
        val client = SoapSriReceptionClient(SriWsConfig(), transport)

        val error = assertFailsWith<DomainRuleViolation> {
            client.submit(
                SriReceptionCommand(
                    organizationId = "org_1",
                    environment = SriEnvironment.TEST,
                    signedXml = "<factura/>".toByteArray(),
                    accessKey = accessKey,
                )
            )
        }

        assertTrue(error.message!!.contains("503"))
    }

    @Test
    fun `authorization client fails on non successful http status`() {
        val transport = FakeSriSoapTransport(authorizedResponse(accessKey.value), statusCode = 500)
        val client = SoapSriAuthorizationClient(SriWsConfig(), transport)

        val error = assertFailsWith<DomainRuleViolation> {
            client.query(
                SriAuthorizationQueryCommand(
                    organizationId = "org_1",
                    environment = SriEnvironment.TEST,
                    accessKey = accessKey,
                )
            )
        }

        assertTrue(error.message!!.contains("500"))
    }
}

private class FakeSriSoapTransport(
    private val responseBody: String,
    private val statusCode: Int = 200,
) : SriSoapTransport {
    var lastCommand: SriSoapHttpCommand? = null
        private set

    override fun execute(command: SriSoapHttpCommand): SriSoapHttpResponse {
        lastCommand = command
        return SriSoapHttpResponse(statusCode = statusCode, body = responseBody)
    }
}
