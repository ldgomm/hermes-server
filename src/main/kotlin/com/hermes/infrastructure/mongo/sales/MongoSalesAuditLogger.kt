package com.hermes.infrastructure.mongo.sales

import com.hermes.application.sales.SalesAuditEvent
import com.hermes.application.sales.SalesAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.UUID

class MongoSalesAuditLogger(database: MongoDatabase) : SalesAuditLogger {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: SalesAuditEvent) {
        collection.insertOne(
            Document(MongoDocumentFields.ID, event.auditId())
                .append(MongoDocumentFields.ORGANIZATION_ID, event.organizationId)
                .append("context", "sales")
                .append("action", event.action.name)
                .append("actorUserId", event.actorUserId)
                .append("targetId", event.targetId)
                .append("before", Document(event.before))
                .append("after", Document(event.after))
                .append("reason", event.reason)
                .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(event.createdAt))
                .append(MongoDocumentFields.CREATED_BY, event.actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(event.createdAt))
                .append(MongoDocumentFields.UPDATED_BY, event.actorUserId)
                .append(MongoDocumentFields.VERSION, 1)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1)
        )
    }

    private fun SalesAuditEvent.auditId(): String {
        val action = action.name.lowercase()
        val target = targetId?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "none"
        val entropy = UUID.randomUUID().toString().replace("-", "").take(12)
        return "saudit_${createdAt.toEpochMilli()}_${action}_${target}_$entropy"
    }
}
