package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M020CreateOutboxEventsMigration : MongoMigration {
    override val id: String = "M020_create_outbox_events"
    override val description: String =
        "Create outbox events for reliable async projections, integrations and future public discovery sync."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("eventId", MongoMigrationSupport.string(maxLength = 128))
            .append("eventType", MongoMigrationSupport.string(maxLength = 128))
            .append("aggregateType", MongoMigrationSupport.string(maxLength = 128))
            .append("aggregateId", MongoMigrationSupport.string(maxLength = 128))
            .append("payload", MongoMigrationSupport.obj())
            .append(
                "status",
                MongoMigrationSupport.enum(listOf("pending", "processing", "published", "failed", "dead_letter"))
            )
            .append("availableAt", MongoMigrationSupport.date())
            .append("publishedAt", MongoMigrationSupport.nullableDate())
            .append("attempts", MongoMigrationSupport.int())
            .append("lastError", MongoMigrationSupport.nullableString(maxLength = 4096))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.OUTBOX_EVENTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "eventId",
                    "eventType",
                    "aggregateType",
                    "aggregateId",
                    "payload",
                    "status",
                    "availableAt",
                    "attempts"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("eventId"),
            "outbox_events_event_id_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("status", "availableAt"),
            "outbox_events_status_available_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "aggregateType", "aggregateId"),
            "outbox_events_org_aggregate_idx"
        )
    }
}
