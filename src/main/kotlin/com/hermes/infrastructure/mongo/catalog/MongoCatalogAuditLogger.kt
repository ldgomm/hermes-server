package com.hermes.infrastructure.mongo.catalog

import com.hermes.application.catalog.CatalogAuditEvent
import com.hermes.application.catalog.CatalogAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.UUID

class MongoCatalogAuditLogger(database: MongoDatabase) : CatalogAuditLogger {
    private val collection = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: CatalogAuditEvent) {
        collection.insertOne(
            Document(MongoDocumentFields.ID, "caud_" + UUID.randomUUID().toString().replace("-", ""))
                .append("module", "catalog")
                .append("action", event.action.name)
                .append("actorUserId", event.actorUserId)
                .append("organizationId", event.organizationId)
                .append("targetId", event.targetId)
                .append("before", Document(event.before))
                .append("after", Document(event.after))
                .append("reason", event.reason)
                .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(event.createdAt))
        )
    }
}
