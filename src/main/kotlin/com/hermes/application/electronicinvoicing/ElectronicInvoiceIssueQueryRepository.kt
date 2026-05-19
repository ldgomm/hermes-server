package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

interface ElectronicInvoiceIssueQueryRepository {
    fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord?
    fun search(query: ElectronicInvoiceIssueSearchQuery): List<ElectronicInvoiceIssueRecord>
}

data class ElectronicInvoiceIssueSearchQuery(
    val organizationId: String,
    val saleId: String? = null,
    val statuses: Set<ElectronicDocumentStatus> = emptySet(),
    val environment: SriEnvironment? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to search electronic invoices.")
        saleId?.let {
            if (it.isBlank()) throw DomainRuleViolation("Sale id filter cannot be blank when provided.")
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw DomainRuleViolation("Electronic invoice search from cannot be after to.")
        }
        if (limit !in 1..MAX_LIMIT) {
            throw DomainRuleViolation("Electronic invoice search limit must be between 1 and $MAX_LIMIT.")
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 200
    }
}
