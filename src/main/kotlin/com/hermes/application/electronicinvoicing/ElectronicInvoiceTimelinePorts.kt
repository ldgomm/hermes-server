package com.hermes.application.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class ElectronicInvoiceTimelineQuery(
    val organizationId: String,
    val documentId: String,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to list electronic invoice timeline.")
        if (documentId.isBlank()) throw DomainRuleViolation("Electronic document id is required to list timeline.")
        if (limit !in 1..MAX_LIMIT) {
            throw DomainRuleViolation("Electronic invoice timeline limit must be between 1 and $MAX_LIMIT.")
        }
    }

    companion object {
        const val DEFAULT_LIMIT: Int = 100
        const val MAX_LIMIT: Int = 300
    }
}

data class ElectronicInvoiceTimelineEvent(
    val id: String,
    val organizationId: String,
    val documentId: String,
    val action: String,
    val actorUserId: String?,
    val saleId: String?,
    val accessKey: String?,
    val status: String?,
    val message: String?,
    val occurredAt: Instant,
)

interface ElectronicInvoiceTimelineRepository {
    fun list(query: ElectronicInvoiceTimelineQuery): List<ElectronicInvoiceTimelineEvent>
}
