package com.hermes.infrastructure.sri

import com.hermes.application.electronicinvoicing.SriReceptionClient
import com.hermes.application.electronicinvoicing.SriReceptionCommand
import com.hermes.application.electronicinvoicing.SriReceptionResult
import com.hermes.domain.shared.DomainRuleViolation

class SoapSriReceptionClient(
    private val config: SriWsConfig,
    private val transport: SriSoapTransport,
    private val parser: SriSoapResponseParser = SriSoapResponseParser(),
) : SriReceptionClient {
    override fun submit(command: SriReceptionCommand): SriReceptionResult {
        val request = SriSoapEnvelopeBuilder.receptionEnvelope(command.signedXml)
        val response = transport.execute(
            SriSoapHttpCommand(
                endpointUrl = config.receptionUrl(command.environment),
                soapAction = SOAP_ACTION,
                body = request,
                connectTimeout = config.connectTimeout,
                requestTimeout = config.requestTimeout,
            )
        )
        if (!response.successfulHttpStatus) {
            throw DomainRuleViolation("SRI reception HTTP status was ${response.statusCode}.")
        }
        return parser.parseReception(
            environment = command.environment,
            rawResponseXml = response.body,
            fallbackAccessKey = command.accessKey,
            receivedAt = command.requestedAt,
        ).copy(rawRequestXml = request)
    }

    companion object {
        const val SOAP_ACTION: String = "validarComprobante"
    }
}
