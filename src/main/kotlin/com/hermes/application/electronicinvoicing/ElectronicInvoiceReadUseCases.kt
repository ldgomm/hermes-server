package com.hermes.application.electronicinvoicing

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

class GetElectronicInvoiceUseCase(
    private val queryRepository: ElectronicInvoiceIssueQueryRepository,
) {
    fun execute(command: GetElectronicInvoiceCommand): ElectronicInvoiceResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW,
        )

        val organizationId = requiredElectronicInvoiceReadText(command.organizationId, "Organization id")
        val documentId = requiredElectronicInvoiceReadText(command.documentId, "Electronic invoice document id")

        val record = queryRepository.findById(organizationId = organizationId, documentId = documentId)
            ?: throw DomainRuleViolation("Electronic invoice document does not exist.")

        return ElectronicInvoiceResult(record)
    }
}

class ListElectronicInvoicesUseCase(
    private val queryRepository: ElectronicInvoiceIssueQueryRepository,
) {
    fun execute(command: ListElectronicInvoicesCommand): ElectronicInvoicesResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_LIST,
        )

        val query = ElectronicInvoiceIssueSearchQuery(
            organizationId = requiredElectronicInvoiceReadText(command.organizationId, "Organization id"),
            saleId = command.saleId?.trim()?.takeIf { it.isNotBlank() },
            statuses = command.statuses,
            environment = command.environment,
            from = command.from,
            to = command.to,
            limit = command.limit.coerceIn(
                1,
                ElectronicInvoiceIssueSearchQuery.MAX_LIMIT,
            ),
        )

        return ElectronicInvoicesResult(queryRepository.search(query))
    }
}

private fun requiredElectronicInvoiceReadText(value: String, label: String): String =
    value.trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
