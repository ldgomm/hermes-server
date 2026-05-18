package com.hermes.infrastructure.mongo.documents

import com.hermes.application.documents.CommercialDocumentAuditEvent
import com.hermes.application.documents.CommercialDocumentAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.*

class MongoCommercialDocumentAuditLogger(database: MongoDatabase) : CommercialDocumentAuditLogger {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: CommercialDocumentAuditEvent) {
        val occurredAt = MongoInstantMapper.toDate(event.createdAt)
        collection.insertOne(
            Document(MongoDocumentFields.ID, "docaudit_" + UUID.randomUUID().toString().replace("-", ""))
                .append(MongoDocumentFields.ORGANIZATION_ID, event.organizationId)
                .append(MongoDocumentFields.CREATED_AT, occurredAt)
                .append(MongoDocumentFields.CREATED_BY, event.actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, occurredAt)
                .append(MongoDocumentFields.UPDATED_BY, event.actorUserId)
                .append(MongoDocumentFields.VERSION, 1)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1)
                .append("module", "commercial_documents")
                .append("context", "commercial_documents")
                .append("actorUserId", event.actorUserId)
                .append("action", event.action.name)
                .append("entityType", "commercial_document")
                .append("entityId", event.targetId)
                .append("targetId", event.targetId)
                .append("saleId", event.saleId)
                .append("occurredAt", occurredAt)
                .append("reason", event.reason)
                .append("before", Document(event.before))
                .append("after", Document(event.after))
                .append("metadata", Document("module", "commercial_documents")),
        )
    }
}
