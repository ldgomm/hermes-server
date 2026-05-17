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
        val occurredAt = MongoInstantMapper.toDate(event.createdAt)
        val organizationId = event.organizationId ?: PLATFORM_AUDIT_ORGANIZATION_ID
        val targetId = event.targetId ?: organizationId
        val actorUserId = event.actorUserId?.takeIf { it.isNotBlank() }

        collection.insertOne(
            Document(MongoDocumentFields.ID, "aud_" + UUID.randomUUID().toString().replace("-", ""))
                .append(MongoDocumentFields.ORGANIZATION_ID, organizationId)
                .append(MongoDocumentFields.CREATED_AT, occurredAt)
                .append(MongoDocumentFields.CREATED_BY, actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, occurredAt)
                .append(MongoDocumentFields.UPDATED_BY, actorUserId)
                .append(MongoDocumentFields.VERSION, 1)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1)
                .append("module", "catalog")
                .append("actorUserId", actorUserId)
                .append("actorType", if (actorUserId == null) "system" else "user")
                .append("action", event.action.name)
                .append("entityType", "catalog")
                .append("entityId", targetId)
                .append("targetId", targetId)
                .append("occurredAt", occurredAt)
                .append("createdAt", occurredAt)
                .append("ipAddress", null)
                .append("requestId", null)
                .append("reason", event.reason)
                .append("before", Document(event.before))
                .append("after", Document(event.after))
                .append("metadata", Document("module", "catalog")),
        )
    }

    private companion object {
        const val PLATFORM_AUDIT_ORGANIZATION_ID = "org_platform"
    }
}
