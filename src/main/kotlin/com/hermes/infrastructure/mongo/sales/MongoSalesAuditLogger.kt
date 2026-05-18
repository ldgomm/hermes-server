package com.hermes.infrastructure.mongo.sales

import com.hermes.application.sales.SalesAuditEvent
import com.hermes.application.sales.SalesAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

class MongoSalesAuditLogger(database: MongoDatabase) : SalesAuditLogger {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: SalesAuditEvent) {
        collection.insertOne(
            Document(MongoDocumentFields.ID, "saudit_${event.createdAt.toEpochMilli()}_${event.action.name.lowercase()}")
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
}
