package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import java.time.LocalDate

@Suppress("LongParameterList")
data class IssueElectronicInvoiceCommand(
    val organizationId: String,
    val actorUserId: String,
    val saleId: String,
    val branchId: String,
    val emissionPointId: String,
    val environment: SriEnvironment,
    val issuerRuc: String,
    val series: SriSeries,
    val issuedDate: LocalDate,
    val numericCode: SriNumericCode,
    val signatureId: String? = null,
    val documentId: String? = null,
    val issuedAt: Instant? = null,
    val queryAuthorizationImmediately: Boolean = true,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to issue electronic invoice.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to issue electronic invoice.")
        if (saleId.isBlank()) throw DomainRuleViolation("Sale id is required to issue electronic invoice.")
        if (branchId.isBlank()) throw DomainRuleViolation("Branch id is required to issue electronic invoice.")
        if (emissionPointId.isBlank()) throw DomainRuleViolation("Emission point id is required to issue electronic invoice.")
        if (!issuerRuc.trim()
                .matches(Regex("\\d{13}"))
        ) throw DomainRuleViolation("Issuer RUC must contain exactly 13 digits.")
        documentId?.let {
            if (it.isBlank()) throw DomainRuleViolation("Electronic invoice document id cannot be blank when provided.")
        }
    }
}

data class IssueElectronicInvoiceResult(
    val record: ElectronicInvoiceIssueRecord,
    val reservation: ReserveSriAccessKeyResult,
    val generatedXml: GeneratedXml?,
    val validation: XsdValidationResult?,
    val signedXml: SignedXml?,
    val reception: SriReceptionResult?,
    val authorization: SriAuthorizationResult?,
    val artifacts: List<StoredElectronicDocumentArtifact>,
) {
    val authorized: Boolean get() = authorization?.isAuthorized == true
    val stoppedBeforeSri: Boolean get() = reception == null
}

data class SubmitElectronicDocumentToSriCommand(
    val record: ElectronicInvoiceIssueRecord,
    val signedXml: SignedXml,
    val actorUserId: String,
) {
    init {
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to submit electronic document to SRI.")
    }
}

data class SubmitElectronicDocumentToSriResult(
    val record: ElectronicInvoiceIssueRecord,
    val reception: SriReceptionResult,
    val artifacts: List<StoredElectronicDocumentArtifact>,
)

data class QuerySriAuthorizationCommand(
    val record: ElectronicInvoiceIssueRecord,
    val actorUserId: String,
) {
    init {
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to query SRI authorization.")
    }
}

data class QuerySriAuthorizationResult(
    val record: ElectronicInvoiceIssueRecord,
    val authorization: SriAuthorizationResult,
    val artifacts: List<StoredElectronicDocumentArtifact>,
)

@Suppress("LongParameterList")
internal fun reserveAccessKeyCommandFrom(
    command: IssueElectronicInvoiceCommand,
    documentId: String,
    issuedAt: Instant
): ReserveSriAccessKeyCommand =
    ReserveSriAccessKeyCommand(
        organizationId = command.organizationId,
        environment = command.environment,
        documentType = SriDocumentType.INVOICE,
        ruc = command.issuerRuc,
        series = command.series,
        issuedDate = command.issuedDate,
        numericCode = command.numericCode,
        documentId = documentId,
        issuedAt = issuedAt,
    )
