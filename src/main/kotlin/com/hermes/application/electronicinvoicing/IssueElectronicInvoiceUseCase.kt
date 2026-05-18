package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriErrorClassificationInput
import com.hermes.domain.electronicinvoicing.SriErrorClassifier
import java.time.Clock
import java.time.Instant

@Suppress("LongParameterList")
class IssueElectronicInvoiceUseCase(
    private val idGenerator: ElectronicInvoiceIssueIdGenerator,
    private val accessKeyUseCase: ReserveSriAccessKeyUseCase,
    private val xmlCommandFactory: ElectronicInvoiceXmlCommandFactory,
    private val xmlBuilder: InvoiceXmlBuilder,
    private val xsdValidator: SriXsdValidator,
    private val signingService: ElectronicDocumentSigningService,
    private val submitToSriUseCase: SubmitElectronicDocumentToSriUseCase,
    private val queryAuthorizationUseCase: QuerySriAuthorizationUseCase,
    private val repository: ElectronicInvoiceIssueRepository,
    private val artifactStorage: ElectronicDocumentArtifactStorage,
    private val auditLogger: ElectronicInvoiceIssueAuditLogger = NoopElectronicInvoiceIssueAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: IssueElectronicInvoiceCommand): IssueElectronicInvoiceResult {
        val issuedAt = command.issuedAt ?: Instant.now(clock)
        val documentId = command.documentId?.trim()?.takeIf { it.isNotBlank() } ?: idGenerator.newId("doc")

        if (repository.existsAuthorizedInvoiceForSale(command.organizationId, command.saleId)) {
            throw com.hermes.domain.shared.DomainRuleViolation("Sale already has an authorized electronic invoice.")
        }

        val reservation = accessKeyUseCase.execute(reserveAccessKeyCommandFrom(command, documentId, issuedAt))
        var record = ElectronicInvoiceIssueRecord.accessKeyGenerated(
            id = documentId,
            organizationId = command.organizationId,
            branchId = command.branchId,
            emissionPointId = command.emissionPointId,
            saleId = command.saleId,
            environment = command.environment,
            documentType = SriDocumentType.INVOICE,
            series = command.series,
            documentNumber = reservation.documentNumber,
            accessKey = reservation.accessKey,
            authorizationNumber = reservation.authorizationNumber,
            issuedAt = issuedAt,
            actorUserId = command.actorUserId,
        )
        repository.create(record)
        auditLogger.log(
            record.audit(
                ElectronicInvoiceIssueAuditAction.SRI_ACCESS_KEY_GENERATED,
                command.actorUserId,
                issuedAt
            )
        )

        val xmlCommand = xmlCommandFactory.build(
            PrepareElectronicInvoiceXmlCommand(
                issueCommand = command,
                documentId = documentId,
                documentNumber = reservation.documentNumber,
                accessKey = reservation.accessKey,
                authorizationNumber = reservation.authorizationNumber,
                sequentialReservation = reservation,
            )
        )
        val generatedXml = xmlBuilder.build(xmlCommand)
        val unsignedArtifact = artifactStorage.put(
            StoreElectronicDocumentArtifactCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                artifactType = ElectronicDocumentArtifactType.UNSIGNED_XML,
                content = generatedXml.bytes,
                contentType = "application/xml; charset=UTF-8",
                fileName = "${record.id}_unsigned.xml",
                createdAt = Instant.now(clock),
            )
        )
        record = record.markXmlGenerated(generatedXml, unsignedArtifact, Instant.now(clock), command.actorUserId)
        repository.update(record)
        auditLogger.log(
            record.audit(
                ElectronicInvoiceIssueAuditAction.ELECTRONIC_XML_GENERATED,
                command.actorUserId,
                Instant.now(clock)
            )
        )

        val validation = xsdValidator.validate(
            xml = generatedXml.bytes,
            schemaVersionCode = generatedXml.schemaVersion.schemaVersionCode,
        )
        if (!validation.valid) {
            record = record.markXsdInvalid(validation.errors, Instant.now(clock), command.actorUserId)
            repository.update(record)
            auditLogger.log(
                record.audit(
                    ElectronicInvoiceIssueAuditAction.ELECTRONIC_XML_XSD_INVALID,
                    command.actorUserId,
                    Instant.now(clock),
                    validation.errors.firstOrNull()?.message,
                )
            )
            return IssueElectronicInvoiceResult(
                record = record,
                reservation = reservation,
                generatedXml = generatedXml,
                validation = validation,
                signedXml = null,
                reception = null,
                authorization = null,
                artifacts = listOf(unsignedArtifact),
            )
        }

        record = record.markXsdValidated(Instant.now(clock), command.actorUserId)
        repository.update(record)
        auditLogger.log(
            record.audit(
                ElectronicInvoiceIssueAuditAction.ELECTRONIC_XML_XSD_VALIDATED,
                command.actorUserId,
                Instant.now(clock)
            )
        )

        val signResult = try {
            signingService.sign(
                SignElectronicDocumentCommand(
                    organizationId = record.organizationId,
                    documentId = record.id,
                    signatureId = command.signatureId,
                    accessKey = record.accessKey.value,
                    unsignedXml = generatedXml.bytes,
                )
            )
        } catch (error: Throwable) {
            val classification = SriErrorClassifier.classify(
                SriErrorClassificationInput(technicalCause = error.message ?: error::class.simpleName)
            )
            record = record.markSignatureFailed(classification, Instant.now(clock), command.actorUserId)
            repository.update(record)
            auditLogger.log(
                record.audit(
                    ElectronicInvoiceIssueAuditAction.ELECTRONIC_XML_SIGNATURE_FAILED,
                    command.actorUserId,
                    Instant.now(clock),
                    classification.reason,
                )
            )
            throw error
        }

        val signedArtifact = artifactStorage.put(
            StoreElectronicDocumentArtifactCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                artifactType = ElectronicDocumentArtifactType.SIGNED_XML,
                content = signResult.signedXml.signedXml,
                contentType = "application/xml; charset=UTF-8",
                fileName = "${record.id}_signed.xml",
                createdAt = signResult.signedAt,
            )
        )
        record = record.markSigned(signResult.signedXml, signedArtifact, signResult.signedAt, command.actorUserId)
        repository.update(record)
        auditLogger.log(
            record.audit(
                ElectronicInvoiceIssueAuditAction.ELECTRONIC_XML_SIGNED,
                command.actorUserId,
                Instant.now(clock)
            )
        )

        val submitResult = submitToSriUseCase.execute(
            SubmitElectronicDocumentToSriCommand(
                record = record,
                signedXml = signResult.signedXml,
                actorUserId = command.actorUserId,
            )
        )
        record = submitResult.record
        val artifacts = mutableListOf(unsignedArtifact, signedArtifact)
        artifacts += submitResult.artifacts

        if (!submitResult.reception.canQueryAuthorization || !command.queryAuthorizationImmediately) {
            return IssueElectronicInvoiceResult(
                record = record,
                reservation = reservation,
                generatedXml = generatedXml,
                validation = validation,
                signedXml = signResult.signedXml,
                reception = submitResult.reception,
                authorization = null,
                artifacts = artifacts,
            )
        }

        if (record.status == ElectronicDocumentStatus.RECEIVED_BY_SRI) {
            record = record.transitionTo(
                ElectronicDocumentStatus.AUTHORIZATION_PENDING,
                Instant.now(clock),
                command.actorUserId
            )
            repository.update(record)
        }

        val authorizationResult = queryAuthorizationUseCase.execute(
            QuerySriAuthorizationCommand(
                record = record,
                actorUserId = command.actorUserId,
            )
        )
        artifacts += authorizationResult.artifacts

        return IssueElectronicInvoiceResult(
            record = authorizationResult.record,
            reservation = reservation,
            generatedXml = generatedXml,
            validation = validation,
            signedXml = signResult.signedXml,
            reception = submitResult.reception,
            authorization = authorizationResult.authorization,
            artifacts = artifacts,
        )
    }
}

private fun ElectronicInvoiceIssueRecord.audit(
    action: ElectronicInvoiceIssueAuditAction,
    actorUserId: String?,
    now: Instant,
    message: String? = null,
): ElectronicInvoiceIssueAuditEvent = ElectronicInvoiceIssueAuditEvent(
    action = action,
    actorUserId = actorUserId,
    organizationId = organizationId,
    documentId = id,
    saleId = saleId,
    accessKey = accessKey.value,
    status = status,
    message = message,
    createdAt = now,
)
