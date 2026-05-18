package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriAccessKey
import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriMessage
import com.hermes.domain.electronicinvoicing.SriReceptionStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

interface SriReceptionClient {
    fun submit(command: SriReceptionCommand): SriReceptionResult
}

interface SriAuthorizationClient {
    fun query(command: SriAuthorizationQueryCommand): SriAuthorizationResult
}

data class SriReceptionCommand(
    val organizationId: String,
    val environment: SriEnvironment,
    val signedXml: ByteArray,
    val accessKey: SriAccessKey? = null,
    val requestedAt: Instant = Instant.now(),
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required for SRI reception.")
        if (signedXml.isEmpty()) throw DomainRuleViolation("Signed XML is required for SRI reception.")
        accessKey?.let {
            if (it.environment != environment) {
                throw DomainRuleViolation("SRI reception environment must match access key environment.")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SriReceptionCommand) return false
        return organizationId == other.organizationId &&
            environment == other.environment &&
            signedXml.contentEquals(other.signedXml) &&
            accessKey == other.accessKey &&
            requestedAt == other.requestedAt
    }

    override fun hashCode(): Int {
        var result = organizationId.hashCode()
        result = 31 * result + environment.hashCode()
        result = 31 * result + signedXml.contentHashCode()
        result = 31 * result + (accessKey?.hashCode() ?: 0)
        result = 31 * result + requestedAt.hashCode()
        return result
    }
}

data class SriAuthorizationQueryCommand(
    val organizationId: String,
    val environment: SriEnvironment,
    val accessKey: SriAccessKey,
    val requestedAt: Instant = Instant.now(),
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required for SRI authorization query.")
        if (accessKey.environment != environment) {
            throw DomainRuleViolation("SRI authorization environment must match access key environment.")
        }
    }
}

data class SriReceptionResult(
    val environment: SriEnvironment,
    val status: SriReceptionStatus,
    val accessKey: SriAccessKey? = null,
    val messages: List<SriMessage> = emptyList(),
    val rawResponseXml: String,
    val rawRequestXml: String? = null,
    val receivedAt: Instant = Instant.now(),
) {
    init {
        if (rawResponseXml.isBlank()) throw DomainRuleViolation("Raw SRI reception response cannot be blank.")
    }

    val canQueryAuthorization: Boolean get() = status.canQueryAuthorization
}

data class SriAuthorizationResult(
    val environment: SriEnvironment,
    val status: SriAuthorizationStatus,
    val accessKey: SriAccessKey,
    val authorizationNumber: String? = null,
    val authorizedAt: Instant? = null,
    val authorizedXml: String? = null,
    val messages: List<SriMessage> = emptyList(),
    val rawResponseXml: String,
    val rawRequestXml: String? = null,
    val queriedAt: Instant = Instant.now(),
) {
    init {
        if (rawResponseXml.isBlank()) throw DomainRuleViolation("Raw SRI authorization response cannot be blank.")
        if (status == SriAuthorizationStatus.AUTHORIZED) {
            if (authorizationNumber.isNullOrBlank()) {
                throw DomainRuleViolation("Authorized SRI document requires authorization number.")
            }
            if (authorizedXml.isNullOrBlank()) {
                throw DomainRuleViolation("Authorized SRI document requires authorized XML.")
            }
        }
    }

    val isAuthorized: Boolean get() = status == SriAuthorizationStatus.AUTHORIZED
    val isProcessing: Boolean get() = status == SriAuthorizationStatus.PROCESSING
    val isRejected: Boolean get() = status == SriAuthorizationStatus.NOT_AUTHORIZED
}
