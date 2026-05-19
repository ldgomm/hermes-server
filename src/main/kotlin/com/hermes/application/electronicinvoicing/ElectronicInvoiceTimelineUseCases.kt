package com.hermes.application.electronicinvoicing

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

data class GetElectronicInvoiceTimelineCommand(
    val organizationId: String,
    val documentId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val limit: Int = ElectronicInvoiceTimelineQuery.DEFAULT_LIMIT,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to get electronic invoice timeline.")
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic document id is required to get timeline.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required to get timeline.")
    }
}

data class GetElectronicInvoiceTimelineResult(
    val record: ElectronicInvoiceIssueRecord,
    val events: List<ElectronicInvoiceTimelineEvent>,
)

class GetElectronicInvoiceTimelineUseCase(
    private val issueRepository: ElectronicInvoiceIssueQueryRepository,
    private val timelineRepository: ElectronicInvoiceTimelineRepository,
) {
    fun execute(command: GetElectronicInvoiceTimelineCommand): GetElectronicInvoiceTimelineResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_AUDIT,
        )

        val record = issueRepository.findById(
            organizationId = command.organizationId.trim(),
            documentId = command.documentId.trim(),
        ) ?: throw DomainRuleViolation("Electronic invoice issue record does not exist.")

        val events = timelineRepository.list(
            ElectronicInvoiceTimelineQuery(
                organizationId = record.organizationId,
                documentId = record.id,
                limit = command.limit.coerceIn(1, ElectronicInvoiceTimelineQuery.MAX_LIMIT),
            )
        )

        return GetElectronicInvoiceTimelineResult(record = record, events = events)
    }
}
