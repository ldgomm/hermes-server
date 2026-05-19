package com.hermes.application.electronicinvoicing

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.*

class UuidElectronicInvoiceIssueIdGenerator : ElectronicInvoiceIssueIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

@Suppress("LongParameterList")
data class IssueElectronicInvoiceFromSaleCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val saleId: String,
    val signatureId: String? = null,
    val queryAuthorizationImmediately: Boolean = true,
    val numericCode: SriNumericCode? = null,
    val documentId: String? = null,
    val issuedAt: Instant? = null,
    val issuedDate: LocalDate? = null,
)

data class IssueElectronicInvoiceFromSaleResult(
    val issueResult: IssueElectronicInvoiceResult,
)

class IssueElectronicInvoiceFromSaleUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val settingsRepository: OrganizationSriSettingsRepository,
    private val signatureRepository: ElectronicSignatureRepository,
    private val sequenceRepository: ElectronicSequenceRepository,
    private val issueElectronicInvoiceUseCase: IssueElectronicInvoiceUseCase,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: IssueElectronicInvoiceFromSaleCommand): IssueElectronicInvoiceFromSaleResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_ISSUE,
        )

        val organizationId = command.organizationId.requiredOperationText("Organization id")
        val actorUserId = command.actorUserId.requiredOperationText("Actor user id")
        val saleId = command.saleId.requiredOperationText("Sale id")
        val now = Instant.now(clock)
        val sale = saleRepository.findById(organizationId, saleId)
            ?: throw DomainRuleViolation("Sale does not exist for electronic invoice emission.")
        sale.assertEligibleForElectronicInvoice()

        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("SRI settings are required before issuing electronic invoices.")
        settings.assertReadyForIssue()

        if (settings.environment == SriEnvironment.PRODUCTION && !settings.productionEnabled) {
            throw DomainRuleViolation("SRI production emission is disabled for this organization.")
        }

        val signature = command.signatureId?.trim()?.takeIf { it.isNotBlank() }?.let { signatureId ->
            signatureRepository.findById(signatureId)
                ?: throw DomainRuleViolation("Electronic signature does not exist.")
        } ?: signatureRepository.findActiveByOrganizationId(organizationId)
        ?: throw DomainRuleViolation("Organization does not have an active electronic signature.")

        if (signature.organizationId != organizationId) {
            throw DomainRuleViolation("Electronic signature does not belong to requested organization.")
        }
        signature.assertUsable(now)

        val sequence = sequenceRepository.findByKey(
            ElectronicSequenceKey(
                organizationId = organizationId,
                environment = settings.environment,
                documentType = SriDocumentType.INVOICE,
                series = settings.series,
            )
        ) ?: throw DomainRuleViolation("Active electronic invoice sequence is required before issuing.")
        sequence.assertActive()

        val issueCommand = IssueElectronicInvoiceCommand(
            organizationId = organizationId,
            actorUserId = actorUserId,
            saleId = saleId,
            branchId = sale.branchId,
            emissionPointId = settings.emissionPointCode,
            environment = settings.environment,
            issuerRuc = settings.ruc,
            series = settings.series,
            issuedDate = command.issuedDate ?: java.time.LocalDate.now(clock),
            numericCode = command.numericCode ?: SriNumericCode.random(),
            signatureId = signature.id,
            documentId = command.documentId,
            issuedAt = command.issuedAt,
            queryAuthorizationImmediately = command.queryAuthorizationImmediately,
        )

        return IssueElectronicInvoiceFromSaleResult(issueElectronicInvoiceUseCase.execute(issueCommand))
    }

    private fun OrganizationSriSettings.assertReadyForIssue() {
        if (ruc.isBlank()) throw DomainRuleViolation("SRI settings RUC is required.")
        if (legalName.isBlank()) throw DomainRuleViolation("SRI settings legal name is required.")
        if (matrixAddress.isBlank()) throw DomainRuleViolation("SRI settings matrix address is required.")
        if (establishmentAddress.isBlank()) throw DomainRuleViolation("SRI settings establishment address is required.")
        if (establishmentCode.isBlank()) throw DomainRuleViolation("SRI establishment code is required.")
        if (emissionPointCode.isBlank()) throw DomainRuleViolation("SRI emission point code is required.")
    }

    private fun Sale.assertEligibleForElectronicInvoice() {
        if (operationalStatus !in issuableStatuses) {
            throw DomainRuleViolation("Sale with status $operationalStatus cannot be electronically invoiced.")
        }
        if (activeItems.isEmpty()) throw DomainRuleViolation("Cannot issue electronic invoice for sale without active items.")
        if (total.amount.signum() <= 0) throw DomainRuleViolation("Cannot issue electronic invoice for sale with non-positive total.")
        activeItems.forEach { item ->
            if (item.taxProfileSnapshot.sriTaxCode.isBlank()) {
                throw DomainRuleViolation("Sale item ${item.id} does not contain SRI tax code snapshot.")
            }
            if (item.taxProfileSnapshot.sriRateCode.isBlank()) {
                throw DomainRuleViolation("Sale item ${item.id} does not contain SRI tax rate code snapshot.")
            }
        }
    }

    private companion object {
        val issuableStatuses = setOf(
            SaleOperationalStatus.CONFIRMED,
            SaleOperationalStatus.IN_PROGRESS,
            SaleOperationalStatus.READY,
            SaleOperationalStatus.DELIVERED,
            SaleOperationalStatus.CLOSED,
        )
    }
}

data class RetryElectronicInvoiceAuthorizationCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class RetryElectronicInvoiceAuthorizationResult(
    val queryResult: QuerySriAuthorizationResult,
) {
    val record: ElectronicInvoiceIssueRecord get() = queryResult.record
    val authorization: SriAuthorizationResult get() = queryResult.authorization
    val artifacts: List<StoredElectronicDocumentArtifact> get() = queryResult.artifacts
}

class RetryElectronicInvoiceAuthorizationUseCase(
    private val issueRepository: ElectronicInvoiceIssueRepository,
    private val querySriAuthorizationUseCase: QuerySriAuthorizationUseCase,
) {
    fun execute(command: RetryElectronicInvoiceAuthorizationCommand): RetryElectronicInvoiceAuthorizationResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_RETRY,
        )

        val organizationId = command.organizationId.requiredOperationText("Organization id")
        val documentId = command.documentId.requiredOperationText("Electronic invoice document id")
        val actorUserId = command.actorUserId.requiredOperationText("Actor user id")

        val record = issueRepository.findById(organizationId, documentId)
            ?: throw DomainRuleViolation("Electronic invoice document does not exist.")

        if (record.status !in retryableAuthorizationStatuses) {
            throw DomainRuleViolation("Electronic invoice authorization can only be retried from RECEIVED_BY_SRI or AUTHORIZATION_PENDING.")
        }

        return RetryElectronicInvoiceAuthorizationResult(
            querySriAuthorizationUseCase.execute(
                QuerySriAuthorizationCommand(
                    record = record,
                    actorUserId = actorUserId,
                )
            )
        )
    }

    private companion object {
        val retryableAuthorizationStatuses = setOf(
            ElectronicDocumentStatus.RECEIVED_BY_SRI,
            ElectronicDocumentStatus.AUTHORIZATION_PENDING,
        )
    }
}

data class GetElectronicInvoiceErrorsCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class ElectronicInvoiceErrorsResult(
    val record: ElectronicInvoiceIssueRecord,
)

class GetElectronicInvoiceErrorsUseCase(
    private val issueRepository: ElectronicInvoiceIssueRepository,
) {
    fun execute(command: GetElectronicInvoiceErrorsCommand): ElectronicInvoiceErrorsResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_ERRORS,
        )

        val record = issueRepository.findById(
            organizationId = command.organizationId.requiredOperationText("Organization id"),
            documentId = command.documentId.requiredOperationText("Electronic invoice document id"),
        ) ?: throw DomainRuleViolation("Electronic invoice document does not exist.")

        return ElectronicInvoiceErrorsResult(record)
    }
}

private fun String.requiredOperationText(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
