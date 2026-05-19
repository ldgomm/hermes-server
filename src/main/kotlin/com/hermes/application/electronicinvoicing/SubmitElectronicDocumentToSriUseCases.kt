package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class SubmitElectronicDocumentToSriUseCase(
    private val repository: ElectronicInvoiceIssueRepository,
    private val artifactStorage: ElectronicDocumentArtifactStorage,
    private val receptionClient: SriReceptionClient,
    private val auditLogger: ElectronicInvoiceIssueAuditLogger = NoopElectronicInvoiceIssueAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: SubmitElectronicDocumentToSriCommand): SubmitElectronicDocumentToSriResult {
        val now = Instant.now(clock)
        command.record.status.assertCanTransitionTo(ElectronicDocumentStatus.SUBMITTED_TO_RECEPTION)

        val submitted = command.record.markSubmittedToReception(now, command.actorUserId)
        repository.update(submitted)
        auditLogger.log(
            submitted.toIssueAuditEvent(
                ElectronicInvoiceIssueAuditAction.SRI_RECEPTION_SUBMITTED,
                command.actorUserId,
                now
            )
        )

        val reception = receptionClient.submit(
            SriReceptionCommand(
                organizationId = submitted.organizationId,
                environment = submitted.environment,
                signedXml = command.signedXml.signedXml,
                accessKey = submitted.accessKey,
                requestedAt = now,
            )
        )

        val artifacts = mutableListOf<StoredElectronicDocumentArtifact>()
        reception.rawRequestXml?.takeIf { it.isNotBlank() }?.let { rawRequest ->
            artifacts += artifactStorage.put(
                rawXmlArtifact(
                    submitted,
                    ElectronicDocumentArtifactType.SRI_RECEPTION_REQUEST,
                    rawRequest,
                    now
                )
            )
        }
        artifacts += artifactStorage.put(
            rawXmlArtifact(
                submitted,
                ElectronicDocumentArtifactType.SRI_RECEPTION_RESPONSE,
                reception.rawResponseXml,
                now
            )
        )

        val classification =
            if (reception.status == SriReceptionStatus.RETURNED || reception.messages.any { it.isError }) {
                SriErrorClassifier.classify(
                    SriErrorClassificationInput(
                        receptionStatus = reception.status,
                        messages = reception.messages,
                    )
                )
            } else {
                null
            }

        val updated = submitted.markReceptionResult(reception, classification, Instant.now(clock), command.actorUserId)
        repository.update(updated)
        auditLogger.log(
            updated.toIssueAuditEvent(
                action = if (reception.status == SriReceptionStatus.RECEIVED) {
                    ElectronicInvoiceIssueAuditAction.SRI_RECEPTION_RECEIVED
                } else {
                    ElectronicInvoiceIssueAuditAction.SRI_RECEPTION_RETURNED
                },
                actorUserId = command.actorUserId,
                now = Instant.now(clock),
                message = reception.messages.firstOrNull()?.message,
            )
        )

        return SubmitElectronicDocumentToSriResult(
            record = updated,
            reception = reception,
            artifacts = artifacts,
        )
    }
}

class QuerySriAuthorizationUseCase(
    private val repository: ElectronicInvoiceIssueRepository,
    private val artifactStorage: ElectronicDocumentArtifactStorage,
    private val authorizationClient: SriAuthorizationClient,
    private val auditLogger: ElectronicInvoiceIssueAuditLogger = NoopElectronicInvoiceIssueAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: QuerySriAuthorizationCommand): QuerySriAuthorizationResult {
        if (command.record.status !in setOf(
                ElectronicDocumentStatus.RECEIVED_BY_SRI,
                ElectronicDocumentStatus.AUTHORIZATION_PENDING
            )
        ) {
            throw DomainRuleViolation("SRI authorization can only be queried after reception is received or pending authorization.")
        }

        val now = Instant.now(clock)
        auditLogger.log(
            command.record.toIssueAuditEvent(
                ElectronicInvoiceIssueAuditAction.SRI_AUTHORIZATION_QUERIED,
                command.actorUserId,
                now
            )
        )

        val authorization = authorizationClient.query(
            SriAuthorizationQueryCommand(
                organizationId = command.record.organizationId,
                environment = command.record.environment,
                accessKey = command.record.accessKey,
                requestedAt = now,
            )
        )

        val artifacts = mutableListOf<StoredElectronicDocumentArtifact>()
        authorization.rawRequestXml?.takeIf { it.isNotBlank() }?.let { rawRequest ->
            artifacts += artifactStorage.put(
                rawXmlArtifact(
                    command.record,
                    ElectronicDocumentArtifactType.SRI_AUTHORIZATION_REQUEST,
                    rawRequest,
                    now
                )
            )
        }
        artifacts += artifactStorage.put(
            rawXmlArtifact(
                command.record,
                ElectronicDocumentArtifactType.SRI_AUTHORIZATION_RESPONSE,
                authorization.rawResponseXml,
                now
            )
        )

        val authorizedArtifact = authorization.authorizedXml?.takeIf { it.isNotBlank() }?.let { authorizedXml ->
            artifactStorage.put(
                rawXmlArtifact(
                    command.record,
                    ElectronicDocumentArtifactType.AUTHORIZED_XML,
                    authorizedXml,
                    now
                )
            )
                .also { artifacts += it }
        }

        val classification =
            if (authorization.status != SriAuthorizationStatus.AUTHORIZED || authorization.messages.any { it.isError }) {
                SriErrorClassifier.classify(
                    SriErrorClassificationInput(
                        authorizationStatus = authorization.status,
                        messages = authorization.messages,
                    )
                )
            } else {
                null
            }

        val updated = command.record.markAuthorizationResult(
            result = authorization,
            authorizedArtifact = authorizedArtifact,
            classification = classification,
            now = Instant.now(clock),
            actorUserId = command.actorUserId,
        )
        repository.update(updated)
        auditLogger.log(
            updated.toIssueAuditEvent(
                action = when (authorization.status) {
                    SriAuthorizationStatus.AUTHORIZED -> ElectronicInvoiceIssueAuditAction.SRI_AUTHORIZED
                    SriAuthorizationStatus.NOT_AUTHORIZED -> ElectronicInvoiceIssueAuditAction.SRI_NOT_AUTHORIZED
                    SriAuthorizationStatus.PROCESSING -> ElectronicInvoiceIssueAuditAction.SRI_AUTHORIZATION_PROCESSING
                },
                actorUserId = command.actorUserId,
                now = Instant.now(clock),
                message = authorization.messages.firstOrNull()?.message,
            )
        )

        return QuerySriAuthorizationResult(
            record = updated,
            authorization = authorization,
            artifacts = artifacts,
        )
    }
}

class RetrySriAuthorizationUseCase(
    private val querySriAuthorizationUseCase: QuerySriAuthorizationUseCase,
) {
    fun execute(command: QuerySriAuthorizationCommand): QuerySriAuthorizationResult {
        if (command.record.status != ElectronicDocumentStatus.AUTHORIZATION_PENDING) {
            throw DomainRuleViolation("Only authorization-pending documents can retry SRI authorization query.")
        }
        return querySriAuthorizationUseCase.execute(command)
    }
}

private fun rawXmlArtifact(
    record: ElectronicInvoiceIssueRecord,
    type: ElectronicDocumentArtifactType,
    xml: String,
    now: Instant,
): StoreElectronicDocumentArtifactCommand = StoreElectronicDocumentArtifactCommand(
    organizationId = record.organizationId,
    documentId = record.id,
    artifactType = type,
    content = xml.toByteArray(Charsets.UTF_8),
    contentType = "application/xml; charset=UTF-8",
    fileName = "${record.id}_${type.storageValue}.xml",
    createdAt = now,
)
