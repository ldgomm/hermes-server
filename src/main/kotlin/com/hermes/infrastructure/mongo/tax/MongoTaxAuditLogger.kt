package com.hermes.infrastructure.mongo.tax

import com.hermes.application.tax.TaxAuditEvent
import com.hermes.application.tax.TaxAuditLogger
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.util.UUID

class MongoTaxAuditLogger(
    database: MongoDatabase,
) : TaxAuditLogger {
    private val auditLogs = database.getCollection(MongoCollectionNames.AUDIT_LOGS)

    override fun log(event: TaxAuditEvent) {
        val organizationId = event.organizationId ?: "org_platform"
        val entityId = event.targetId ?: organizationId
        val now = event.createdAt

        auditLogs.insertOne(
            Document(MongoDocumentFields.ID, "audit_tax_${UUID.randomUUID().toString().replace("-", "")}")
                .append(MongoDocumentFields.ORGANIZATION_ID, organizationId)
                .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(now))
                .append(MongoDocumentFields.CREATED_BY, event.actorUserId)
                .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(now))
                .append(MongoDocumentFields.UPDATED_BY, event.actorUserId)
                .append(MongoDocumentFields.VERSION, 1)
                .append(MongoDocumentFields.SCHEMA_VERSION, 1)
                .append("actorUserId", event.actorUserId)
                .append("actorType", if (event.actorUserId == null) "system" else "user")
                .append("action", event.action.name)
                .append("entityType", "tax")
                .append("entityId", entityId)
                .append("occurredAt", MongoInstantMapper.toDate(now))
                .append("ipAddress", null)
                .append("requestId", null)
                .append("reason", event.reason)
                .append("before", event.before.toDocument())
                .append("after", event.after.toDocument())
                .append(
                    "metadata",
                    Document()
                        .append("source", "tax_engine")
                        .append("targetId", event.targetId)
                        .append("organizationId", event.organizationId),
                ),
        )
    }

    private fun Map<String, String?>.toDocument(): Document =
        Document().also { document ->
            entries.sortedBy { it.key }.forEach { (key, value) ->
                document[key] = value
            }
        }
}
