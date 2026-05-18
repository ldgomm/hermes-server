package com.hermes.infrastructure.sri

import com.hermes.domain.shared.DomainRuleViolation
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface SriSoapTransport {
    fun execute(command: SriSoapHttpCommand): SriSoapHttpResponse
}

data class SriSoapHttpCommand(
    val endpointUrl: String,
    val soapAction: String,
    val body: String,
    val connectTimeout: Duration,
    val requestTimeout: Duration,
) {
    init {
        if (!endpointUrl.startsWith("https://")) throw DomainRuleViolation("SRI SOAP endpoint must be HTTPS.")
        if (soapAction.isBlank()) throw DomainRuleViolation("SRI SOAP action cannot be blank.")
        if (body.isBlank()) throw DomainRuleViolation("SRI SOAP request body cannot be blank.")
        if (connectTimeout.isZero || connectTimeout.isNegative) throw DomainRuleViolation("SRI SOAP connect timeout must be positive.")
        if (requestTimeout.isZero || requestTimeout.isNegative) throw DomainRuleViolation("SRI SOAP request timeout must be positive.")
    }
}

data class SriSoapHttpResponse(
    val statusCode: Int,
    val body: String,
) {
    init {
        if (body.isBlank()) throw DomainRuleViolation("SRI SOAP response body cannot be blank.")
    }

    val successfulHttpStatus: Boolean get() = statusCode in 200..299
}

class JdkSriSoapTransport : SriSoapTransport {
    override fun execute(command: SriSoapHttpCommand): SriSoapHttpResponse {
        val client = HttpClient.newBuilder().connectTimeout(command.connectTimeout).build()

        val request = HttpRequest.newBuilder().uri(URI.create(command.endpointUrl)).timeout(command.requestTimeout)
            .header("Content-Type", "text/xml; charset=UTF-8").header("Accept", "text/xml")
            .header("SOAPAction", command.soapAction)
            .POST(HttpRequest.BodyPublishers.ofString(command.body, Charsets.UTF_8)).build()

        val response = runCatching {
            client.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
        }.getOrElse { error ->
            throw DomainRuleViolation("SRI SOAP request failed: ${error.message ?: error::class.simpleName}.")
        }

        if (response.body().isBlank()) {
            throw DomainRuleViolation("SRI SOAP response was empty.")
        }

        return SriSoapHttpResponse(
            statusCode = response.statusCode(),
            body = response.body(),
        )
    }
}
