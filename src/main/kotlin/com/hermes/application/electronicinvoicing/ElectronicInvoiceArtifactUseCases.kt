package com.hermes.application.electronicinvoicing

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

enum class ElectronicInvoiceDownloadArtifactKind {
    SIGNED_XML,
    AUTHORIZED_XML,
    RIDE_PDF,
}

data class DownloadElectronicInvoiceArtifactCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val artifactKind: ElectronicInvoiceDownloadArtifactKind,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to download electronic invoice artifact.")
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic document id is required to download artifact.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to download artifact.")
    }
}

data class DownloadElectronicInvoiceArtifactResult(
    val record: ElectronicInvoiceIssueRecord,
    val artifact: ElectronicDocumentArtifactFile,
)

class DownloadElectronicInvoiceArtifactUseCase(
    private val issueRepository: ElectronicInvoiceIssueQueryRepository,
    private val artifactReader: ElectronicDocumentArtifactReader,
) {
    fun execute(command: DownloadElectronicInvoiceArtifactCommand): DownloadElectronicInvoiceArtifactResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            command.artifactKind.requiredPermission,
        )

        val record = issueRepository.findById(
            organizationId = command.organizationId.trim(),
            documentId = command.documentId.trim(),
        ) ?: throw DomainRuleViolation("Electronic invoice issue record does not exist.")

        val expectedType = command.artifactKind.artifactType
        val objectKey = when (command.artifactKind) {
            ElectronicInvoiceDownloadArtifactKind.SIGNED_XML -> record.signedXmlObjectKey
            ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML -> record.authorizedXmlObjectKey
            ElectronicInvoiceDownloadArtifactKind.RIDE_PDF -> record.ridePdfObjectKey
        } ?: throw DomainRuleViolation("Requested electronic invoice artifact is not available yet.")

        val artifact = artifactReader.get(objectKey)
            ?: throw DomainRuleViolation("Requested electronic invoice artifact does not exist in storage.")

        if (artifact.artifactType != expectedType) {
            throw DomainRuleViolation("Requested electronic invoice artifact has an invalid type.")
        }

        return DownloadElectronicInvoiceArtifactResult(record = record, artifact = artifact)
    }
}

private val ElectronicInvoiceDownloadArtifactKind.requiredPermission: String
    get() = when (this) {
        ElectronicInvoiceDownloadArtifactKind.SIGNED_XML,
        ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML -> PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_XML
        ElectronicInvoiceDownloadArtifactKind.RIDE_PDF -> PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_RIDE
    }

private val ElectronicInvoiceDownloadArtifactKind.artifactType: ElectronicDocumentArtifactType
    get() = when (this) {
        ElectronicInvoiceDownloadArtifactKind.SIGNED_XML -> ElectronicDocumentArtifactType.SIGNED_XML
        ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML -> ElectronicDocumentArtifactType.AUTHORIZED_XML
        ElectronicInvoiceDownloadArtifactKind.RIDE_PDF -> ElectronicDocumentArtifactType.RIDE_PDF
    }
