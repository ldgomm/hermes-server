package com.hermes.infrastructure.sri

import com.hermes.application.electronicinvoicing.SriAuthorizationClient
import com.hermes.application.electronicinvoicing.SriAuthorizationQueryCommand
import com.hermes.application.electronicinvoicing.SriAuthorizationResult
import com.hermes.domain.shared.DomainRuleViolation

class SoapSriAuthorizationClient(
    private val config: SriWsConfig,
    private val transport: SriSoapTransport,
    private val parser: SriSoapResponseParser = SriSoapResponseParser(),
) : SriAuthorizationClient {
    override fun query(command: SriAuthorizationQueryCommand): SriAuthorizationResult {
        val request = SriSoapEnvelopeBuilder.authorizationEnvelope(command.accessKey.value)
        val response = transport.execute(
            SriSoapHttpCommand(
                endpointUrl = config.authorizationUrl(command.environment),
                soapAction = SOAP_ACTION,
                body = request,
                connectTimeout = config.connectTimeout,
                requestTimeout = config.requestTimeout,
            )
        )
        if (!response.successfulHttpStatus) {
            throw DomainRuleViolation("SRI authorization HTTP status was ${response.statusCode}.")
        }
        return parser.parseAuthorization(
            environment = command.environment,
            accessKey = command.accessKey,
            rawResponseXml = response.body,
            queriedAt = command.requestedAt,
        ).copy(rawRequestXml = request)
    }

    companion object {
        const val SOAP_ACTION: String = "autorizacionComprobante"
    }
}
