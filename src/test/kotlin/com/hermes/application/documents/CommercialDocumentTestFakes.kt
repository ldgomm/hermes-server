package com.hermes.application.documents

import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentStatus
import java.time.Instant

internal class InMemoryCommercialDocumentRepositoryForTest : CommercialDocumentRepository {
    val documents: MutableMap<String, CommercialDocument> = linkedMapOf()

    override fun create(document: CommercialDocument) {
        require(document.id !in documents) { "Commercial document already exists: ${document.id}." }
        documents[document.id] = document
    }

    override fun update(document: CommercialDocument) {
        require(document.id in documents) { "Commercial document does not exist: ${document.id}." }
        documents[document.id] = document
    }

    override fun findById(organizationId: String, documentId: String): CommercialDocument? =
        documents[documentId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun findBySale(organizationId: String, saleId: String): List<CommercialDocument> =
        documents.values
            .filter { it.organizationId == organizationId.trim() && it.saleId == saleId.trim() }
            .sortedByDescending { it.issuedAt }

    override fun findByDocumentNumber(organizationId: String, documentNumber: String): CommercialDocument? =
        documents.values.firstOrNull {
            it.organizationId == organizationId.trim() && it.documentNumber == documentNumber.trim()
        }

    override fun search(query: CommercialDocumentSearchQuery): List<CommercialDocument> =
        documents.values
            .asSequence()
            .filter { it.organizationId == query.organizationId.trim() }
            .filter { query.saleId == null || it.saleId == query.saleId.trim() }
            .filter { query.documentType == null || it.documentType == query.documentType }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }
            .filter { query.from == null || !it.issuedAt.isBefore(query.from) }
            .filter { query.to == null || !it.issuedAt.isAfter(query.to) }
            .sortedByDescending { it.issuedAt }
            .take(query.limit.coerceIn(1, 500))
            .toList()
}

internal class DeterministicCommercialDocumentIdGenerator : CommercialDocumentIdGenerator {
    private val counters = linkedMapOf<String, Int>()

    override fun newId(prefix: String): String {
        val cleanPrefix = prefix.trim().lowercase()
        val next = counters.getOrDefault(cleanPrefix, 0) + 1
        counters[cleanPrefix] = next
        return "${cleanPrefix}_${next}"
    }
}

internal class SequentialCommercialDocumentNumberGenerator : CommercialDocumentNumberGenerator {
    private val counters = linkedMapOf<String, Long>()

    override fun nextInternalTicketNumber(organizationId: String, branchId: String, issuedAt: Instant): String {
        val key = "${organizationId.trim()}::${branchId.trim()}"
        val next = counters.getOrDefault(key, 0L) + 1L
        counters[key] = next
        return "TCK-${issuedAt.atOffset(java.time.ZoneOffset.UTC).toLocalDate()}-${next.toString().padStart(9, '0')}"
    }
}

internal class RecordingCommercialDocumentAuditLogger : CommercialDocumentAuditLogger {
    val events: MutableList<CommercialDocumentAuditEvent> = mutableListOf()

    override fun log(event: CommercialDocumentAuditEvent) {
        events += event
    }
}

internal class RecordingCommercialDocumentEmailSender(
    private val deliver: Boolean = true,
) : CommercialDocumentEmailSender {
    val sent: MutableList<CommercialDocumentEmail> = mutableListOf()

    override fun send(email: CommercialDocumentEmail): Boolean {
        sent += email
        return deliver
    }
}
