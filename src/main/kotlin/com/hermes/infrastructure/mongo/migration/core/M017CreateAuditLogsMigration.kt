package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M017CreateAuditLogsMigration : MongoMigration {
    override val id: String = "M017_create_audit_logs"
    override val description: String = "Create audit logs and domain events as append-only operational history."

    override fun up(database: MongoDatabase) {
        createAuditLogs(database)
        createDomainEvents(database)
    }

    private fun createAuditLogs(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("actorUserId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("actorType", MongoMigrationSupport.enum(listOf("user", "system", "worker", "platform_admin")))
            .append("action", MongoMigrationSupport.string(maxLength = 128))
            .append("entityType", MongoMigrationSupport.string(maxLength = 128))
            .append("entityId", MongoMigrationSupport.string(maxLength = 128))
            .append("occurredAt", MongoMigrationSupport.date())
            .append("ipAddress", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("requestId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reason", MongoMigrationSupport.nullableString(maxLength = 2048))
            .append("before", MongoMigrationSupport.obj())
            .append("after", MongoMigrationSupport.obj())
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.AUDIT_LOGS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "actorType",
                    "action",
                    "entityType",
                    "entityId",
                    "occurredAt"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "entityType", "entityId", "occurredAt"),
            "audit_logs_org_entity_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "actorUserId", "occurredAt"),
            "audit_logs_org_actor_occurred_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "action", "occurredAt"),
            "audit_logs_org_action_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("requestId"),
            "audit_logs_request_id_idx",
            sparse = true
        )
    }

    private fun createDomainEvents(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("aggregateType", MongoMigrationSupport.string(maxLength = 128))
            .append("aggregateId", MongoMigrationSupport.string(maxLength = 128))
            .append("eventType", MongoMigrationSupport.string(maxLength = 128))
            .append("eventVersion", MongoMigrationSupport.int())
            .append("occurredAt", MongoMigrationSupport.date())
            .append("payload", MongoMigrationSupport.obj())
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.DOMAIN_EVENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "aggregateType",
                    "aggregateId",
                    "eventType",
                    "eventVersion",
                    "occurredAt",
                    "payload"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "aggregateType", "aggregateId", "occurredAt"),
            "domain_events_org_aggregate_occurred_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "eventType", "occurredAt"),
            "domain_events_org_type_occurred_idx"
        )
    }
}
