package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceTimelineEvent
import com.hermes.application.electronicinvoicing.ElectronicInvoiceTimelineQuery
import com.hermes.application.electronicinvoicing.ElectronicInvoiceTimelineRepository
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.time.Instant
import java.util.*

class MongoElectronicInvoiceIssueTimelineRepository(
    database: MongoDatabase,
) : ElectronicInvoiceTimelineRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun list(query: ElectronicInvoiceTimelineQuery): List<ElectronicInvoiceTimelineEvent> = collection
        .find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()),
                Filters.eq("module", "electronic_invoicing"),
                Filters.eq("context", "electronic_invoice_issue"),
                Filters.eq("targetId", query.documentId.trim()),
            )
        )
        .sort(Sorts.ascending("occurredAt", MongoDocumentFields.CREATED_AT, MongoDocumentFields.ID))
        .limit(query.limit)
        .into(mutableListOf())
        .mapNotNull { it.toTimelineEventOrNull() }

    private fun Document.toTimelineEventOrNull(): ElectronicInvoiceTimelineEvent? {
        val metadata = get("metadata", Document::class.java)
        val documentId = getString("documentId")
            ?: getString("targetId")
            ?: getString("entityId")
            ?: metadata?.getString("documentId")
            ?: return null

        return ElectronicInvoiceTimelineEvent(
            id = getString(MongoDocumentFields.ID) ?: return null,
            organizationId = getString(MongoDocumentFields.ORGANIZATION_ID) ?: return null,
            documentId = documentId,
            action = getString("action") ?: return null,
            actorUserId = getString("actorUserId") ?: getString(MongoDocumentFields.CREATED_BY),
            saleId = getString("saleId") ?: metadata?.getString("saleId"),
            accessKey = getString("accessKey") ?: metadata?.getString("accessKey"),
            status = getString("status"),
            message = getString("message"),
            occurredAt = instantField("occurredAt")
                ?: instantField(MongoDocumentFields.CREATED_AT)
                ?: Instant.EPOCH,
        )
    }

    private fun Document.instantField(name: String): Instant? =
        (this[name] as? Date)?.toInstant()
}
