package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAccessKey
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriErrorClassification
import com.hermes.domain.electronicinvoicing.SriMessage
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

fun interface ElectronicInvoiceIssueIdGenerator {
    fun newId(prefix: String): String
}

interface ElectronicInvoiceIssueRepository {
    fun create(record: ElectronicInvoiceIssueRecord)
    fun update(record: ElectronicInvoiceIssueRecord)
    fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord?
    fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean
}

interface ElectronicInvoiceXmlCommandFactory {
    fun build(command: PrepareElectronicInvoiceXmlCommand): BuildSriInvoiceXmlCommand
}

interface ElectronicDocumentSigningService {
    fun sign(command: SignElectronicDocumentCommand): SignElectronicDocumentResult
}

class SignElectronicDocumentUseCaseSigningService(
    private val useCase: SignElectronicDocumentUseCase,
) : ElectronicDocumentSigningService {
    override fun sign(command: SignElectronicDocumentCommand): SignElectronicDocumentResult =
        useCase.execute(command)
}

interface ElectronicDocumentArtifactStorage {
    fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact
}

data class PrepareElectronicInvoiceXmlCommand(
    val issueCommand: IssueElectronicInvoiceCommand,
    val documentId: String,
    val documentNumber: String,
    val accessKey: SriAccessKey,
    val authorizationNumber: String,
    val sequentialReservation: ReserveSriAccessKeyResult,
) {
    init {
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic invoice document id cannot be blank.")
        if (documentNumber.isBlank()) throw DomainRuleViolation("Electronic invoice document number cannot be blank.")
        if (authorizationNumber != accessKey.value) {
            throw DomainRuleViolation("Offline authorization number must match access key.")
        }
    }
}

enum class ElectronicDocumentArtifactType(val storageValue: String) {
    UNSIGNED_XML("unsigned_xml"),
    SIGNED_XML("signed_xml"),
    AUTHORIZED_XML("authorized_xml"),
    RIDE_PDF("ride_pdf"),
    SRI_RECEPTION_REQUEST("sri_reception_request"),
    SRI_RECEPTION_RESPONSE("sri_reception_response"),
    SRI_AUTHORIZATION_REQUEST("sri_authorization_request"),
    SRI_AUTHORIZATION_RESPONSE("sri_authorization_response");

    companion object {
        fun fromStorage(value: String): ElectronicDocumentArtifactType =
            entries.firstOrNull { it.storageValue == value.trim().lowercase() }
                ?: throw DomainRuleViolation("Unknown electronic document artifact type: $value.")
    }
}

data class StoreElectronicDocumentArtifactCommand(
    val organizationId: String,
    val documentId: String,
    val artifactType: ElectronicDocumentArtifactType,
    val content: ByteArray,
    val contentType: String,
    val fileName: String,
    val createdAt: Instant,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Artifact organization id cannot be blank.")
        if (documentId.isBlank()) throw DomainRuleViolation("Artifact document id cannot be blank.")
        if (content.isEmpty()) throw DomainRuleViolation("Artifact content cannot be empty.")
        if (contentType.isBlank()) throw DomainRuleViolation("Artifact content type cannot be blank.")
        if (fileName.isBlank()) throw DomainRuleViolation("Artifact file name cannot be blank.")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreElectronicDocumentArtifactCommand) return false
        return organizationId == other.organizationId &&
            documentId == other.documentId &&
            artifactType == other.artifactType &&
            content.contentEquals(other.content) &&
            contentType == other.contentType &&
            fileName == other.fileName &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = organizationId.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + artifactType.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

data class StoredElectronicDocumentArtifact(
    val objectKey: String,
    val artifactType: ElectronicDocumentArtifactType,
    val sha256: String,
    val sizeBytes: Long,
    val createdAt: Instant,
) {
    init {
        if (objectKey.isBlank()) throw DomainRuleViolation("Stored artifact object key cannot be blank.")
        if (!Regex("^[A-Fa-f0-9]{64}$").matches(sha256)) {
            throw DomainRuleViolation("Stored artifact SHA-256 hash is invalid.")
        }
        if (sizeBytes <= 0) throw DomainRuleViolation("Stored artifact size must be positive.")
    }
}

@Suppress("LongParameterList")
data class ElectronicInvoiceIssueRecord(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String,
    val saleId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType,
    val series: SriSeries,
    val documentNumber: String,
    val accessKey: SriAccessKey,
    val authorizationNumber: String,
    val status: ElectronicDocumentStatus,
    val schemaVersionCode: String? = null,
    val unsignedXmlObjectKey: String? = null,
    val unsignedXmlSha256: String? = null,
    val signedXmlObjectKey: String? = null,
    val signedXmlSha256: String? = null,
    val authorizedXmlObjectKey: String? = null,
    val authorizedXmlSha256: String? = null,
    val ridePdfObjectKey: String? = null,
    val ridePdfSha256: String? = null,
    val signatureId: String? = null,
    val lastSriReceptionStatus: String? = null,
    val lastSriAuthorizationStatus: String? = null,
    val sriMessages: List<SriMessage> = emptyList(),
    val lastErrorClassification: SriErrorClassification? = null,
    val issuedAt: Instant,
    val authorizedAt: Instant? = null,
    val rideGeneratedAt: Instant? = null,
    val deliveryEmailTo: String? = null,
    val deliveredAt: Instant? = null,
    val deliveryErrorMessage: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Electronic invoice issue record id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Electronic invoice organization id cannot be blank.")
        if (branchId.isBlank()) throw DomainRuleViolation("Electronic invoice branch id cannot be blank.")
        if (emissionPointId.isBlank()) throw DomainRuleViolation("Electronic invoice emission point id cannot be blank.")
        if (saleId.isBlank()) throw DomainRuleViolation("Electronic invoice sale id cannot be blank.")
        documentType.assertMvpSupported()
        if (documentType != SriDocumentType.INVOICE) throw DomainRuleViolation("Only invoice issue records are supported.")
        if (documentNumber.isBlank()) throw DomainRuleViolation("Electronic invoice document number cannot be blank.")
        if (authorizationNumber != accessKey.value) throw DomainRuleViolation("Offline authorization number must match access key.")
        if (accessKey.environment != environment) throw DomainRuleViolation("Electronic invoice environment must match access key.")
        if (accessKey.documentType != documentType) throw DomainRuleViolation("Electronic invoice document type must match access key.")
        if (accessKey.series != series) throw DomainRuleViolation("Electronic invoice series must match access key.")
        if (createdBy.isBlank()) throw DomainRuleViolation("Electronic invoice createdBy cannot be blank.")
        if (updatedBy.isBlank()) throw DomainRuleViolation("Electronic invoice updatedBy cannot be blank.")
        if (updatedAt.isBefore(createdAt)) throw DomainRuleViolation("Electronic invoice updatedAt cannot be before createdAt.")
        if (version < 1) throw DomainRuleViolation("Electronic invoice version must be positive.")
        if (deliveredAt != null && deliveryEmailTo.isNullOrBlank()) {
            throw DomainRuleViolation("Delivered electronic invoice requires delivery email recipient.")
        }
    }

    fun transitionTo(
        target: ElectronicDocumentStatus,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord {
        status.assertCanTransitionTo(target)
        return copy(
            status = target,
            updatedAt = now,
            updatedBy = actorUserId.required("Actor user id"),
            version = version + 1,
        )
    }

    fun markXmlGenerated(
        generatedXml: GeneratedXml,
        artifact: StoredElectronicDocumentArtifact,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord = transitionTo(ElectronicDocumentStatus.XML_GENERATED, now, actorUserId).copy(
        schemaVersionCode = generatedXml.schemaVersion.schemaVersionCode,
        unsignedXmlObjectKey = artifact.objectKey,
        unsignedXmlSha256 = generatedXml.sha256,
    )

    fun markXsdValidated(now: Instant, actorUserId: String): ElectronicInvoiceIssueRecord =
        transitionTo(ElectronicDocumentStatus.XSD_VALIDATED, now, actorUserId)

    fun markXsdInvalid(
        errors: List<XsdValidationError>,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord = transitionTo(ElectronicDocumentStatus.XSD_INVALID, now, actorUserId).copy(
        sriMessages = errors.map { error -> SriMessage.error(message = error.message) },
    )

    fun markSigned(
        signed: SignedXml,
        artifact: StoredElectronicDocumentArtifact,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord = transitionTo(ElectronicDocumentStatus.SIGNED, now, actorUserId).copy(
        signatureId = signed.signatureId,
        signedXmlObjectKey = artifact.objectKey,
        signedXmlSha256 = signed.signedXmlSha256,
    )

    fun markSignatureFailed(
        classification: SriErrorClassification,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord = transitionTo(ElectronicDocumentStatus.SIGNATURE_FAILED, now, actorUserId).copy(
        lastErrorClassification = classification,
        sriMessages = listOf(SriMessage.error(message = classification.reason)),
    )

    fun markSubmittedToReception(now: Instant, actorUserId: String): ElectronicInvoiceIssueRecord =
        transitionTo(ElectronicDocumentStatus.SUBMITTED_TO_RECEPTION, now, actorUserId)

    fun markReceptionResult(
        result: SriReceptionResult,
        classification: SriErrorClassification?,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord =
        transitionTo(ElectronicDocumentStatus.fromReceptionStatus(result.status), now, actorUserId).copy(
            lastSriReceptionStatus = result.status.storageValue,
            sriMessages = result.messages,
            lastErrorClassification = classification,
        )

    fun markAuthorizationResult(
        result: SriAuthorizationResult,
        authorizedArtifact: StoredElectronicDocumentArtifact?,
        classification: SriErrorClassification?,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord =
        transitionTo(ElectronicDocumentStatus.fromAuthorizationStatus(result.status), now, actorUserId).copy(
            lastSriAuthorizationStatus = result.status.storageValue,
            authorizedAt = result.authorizedAt ?: authorizedAt,
            authorizedXmlObjectKey = authorizedArtifact?.objectKey ?: authorizedXmlObjectKey,
            authorizedXmlSha256 = authorizedArtifact?.sha256 ?: authorizedXmlSha256,
            sriMessages = result.messages,
            lastErrorClassification = classification,
        )

    fun markRideGenerated(
        rideArtifact: StoredElectronicDocumentArtifact,
        now: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord {
        if (rideArtifact.artifactType != ElectronicDocumentArtifactType.RIDE_PDF) {
            throw DomainRuleViolation("RIDE artifact must have RIDE_PDF type.")
        }
        if (status !in setOf(ElectronicDocumentStatus.AUTHORIZED, ElectronicDocumentStatus.DELIVERY_PENDING, ElectronicDocumentStatus.DELIVERY_FAILED)) {
            throw DomainRuleViolation("RIDE can only be generated for authorized or delivery-pending electronic invoices.")
        }
        val deliveryReady = if (status == ElectronicDocumentStatus.DELIVERY_PENDING) {
            copy(
                updatedAt = now,
                updatedBy = actorUserId.required("Actor user id"),
                version = version + 1,
            )
        } else {
            transitionTo(ElectronicDocumentStatus.DELIVERY_PENDING, now, actorUserId)
        }
        return deliveryReady.copy(
            ridePdfObjectKey = rideArtifact.objectKey,
            ridePdfSha256 = rideArtifact.sha256,
            rideGeneratedAt = now,
            deliveryErrorMessage = null,
        )
    }

    fun markDelivered(emailTo: String, deliveredAt: Instant, actorUserId: String): ElectronicInvoiceIssueRecord =
        transitionTo(ElectronicDocumentStatus.DELIVERED, deliveredAt, actorUserId).copy(
            deliveryEmailTo = emailTo.required("Delivery email recipient"),
            deliveredAt = deliveredAt,
            deliveryErrorMessage = null,
        )

    fun markDeliveryFailed(
        emailTo: String,
        reason: String,
        failedAt: Instant,
        actorUserId: String,
    ): ElectronicInvoiceIssueRecord = transitionTo(ElectronicDocumentStatus.DELIVERY_FAILED, failedAt, actorUserId).copy(
        deliveryEmailTo = emailTo.required("Delivery email recipient"),
        deliveryErrorMessage = reason.trim().takeIf { it.isNotBlank() } ?: "Email delivery failed.",
    )

    companion object {
        @Suppress("LongParameterList")
        fun accessKeyGenerated(
            id: String,
            organizationId: String,
            branchId: String,
            emissionPointId: String,
            saleId: String,
            environment: SriEnvironment,
            documentType: SriDocumentType,
            series: SriSeries,
            documentNumber: String,
            accessKey: SriAccessKey,
            authorizationNumber: String,
            issuedAt: Instant,
            actorUserId: String,
        ): ElectronicInvoiceIssueRecord = ElectronicInvoiceIssueRecord(
            id = id.required("Electronic invoice document id"),
            organizationId = organizationId.required("Organization id"),
            branchId = branchId.required("Branch id"),
            emissionPointId = emissionPointId.required("Emission point id"),
            saleId = saleId.required("Sale id"),
            environment = environment,
            documentType = documentType,
            series = series,
            documentNumber = documentNumber.required("Document number"),
            accessKey = accessKey,
            authorizationNumber = authorizationNumber,
            status = ElectronicDocumentStatus.ACCESS_KEY_GENERATED,
            issuedAt = issuedAt,
            createdAt = issuedAt,
            updatedAt = issuedAt,
            createdBy = actorUserId.required("Actor user id"),
            updatedBy = actorUserId.required("Actor user id"),
        )
    }
}

internal fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
