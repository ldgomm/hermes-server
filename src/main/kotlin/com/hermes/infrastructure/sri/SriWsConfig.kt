package com.hermes.infrastructure.sri

import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Duration

data class SriWsConfig(
    val testReceptionUrl: String = DEFAULT_TEST_RECEPTION_URL,
    val testAuthorizationUrl: String = DEFAULT_TEST_AUTHORIZATION_URL,
    val productionReceptionUrl: String = DEFAULT_PRODUCTION_RECEPTION_URL,
    val productionAuthorizationUrl: String = DEFAULT_PRODUCTION_AUTHORIZATION_URL,
    val connectTimeout: Duration = Duration.ofSeconds(15),
    val requestTimeout: Duration = Duration.ofSeconds(45),
) {
    init {
        validateUrl(testReceptionUrl, "SRI test reception URL")
        validateUrl(testAuthorizationUrl, "SRI test authorization URL")
        validateUrl(productionReceptionUrl, "SRI production reception URL")
        validateUrl(productionAuthorizationUrl, "SRI production authorization URL")
        if (connectTimeout.isZero || connectTimeout.isNegative) {
            throw DomainRuleViolation("SRI connect timeout must be positive.")
        }
        if (requestTimeout.isZero || requestTimeout.isNegative) {
            throw DomainRuleViolation("SRI request timeout must be positive.")
        }
    }

    fun receptionUrl(environment: SriEnvironment): String = when (environment) {
        SriEnvironment.TEST -> testReceptionUrl
        SriEnvironment.PRODUCTION -> productionReceptionUrl
    }

    fun authorizationUrl(environment: SriEnvironment): String = when (environment) {
        SriEnvironment.TEST -> testAuthorizationUrl
        SriEnvironment.PRODUCTION -> productionAuthorizationUrl
    }

    private fun validateUrl(value: String, label: String) {
        val normalized = value.trim()
        if (!normalized.startsWith("https://")) {
            throw DomainRuleViolation("$label must be an HTTPS URL.")
        }
        if (!normalized.contains("?wsdl")) {
            throw DomainRuleViolation("$label must point to the WSDL endpoint.")
        }
    }

    companion object {
        const val DEFAULT_TEST_RECEPTION_URL: String =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
        const val DEFAULT_TEST_AUTHORIZATION_URL: String =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
        const val DEFAULT_PRODUCTION_RECEPTION_URL: String =
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
        const val DEFAULT_PRODUCTION_AUTHORIZATION_URL: String =
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
    }
}
