package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M002CreateOrganizationActivitiesMigration : MongoMigration {
    override val id: String = "M002_create_organization_activities"
    override val description: String = "Create organization activities for multi-activity operations."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("code", MongoMigrationSupport.string(maxLength = 64))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("description", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append(
                "activityType",
                MongoMigrationSupport.enum(
                    listOf(
                        "restaurant",
                        "retail",
                        "services",
                        "tourism",
                        "rental",
                        "mixed",
                        "custom"
                    )
                )
            )
            .append(
                "workflowMode",
                MongoMigrationSupport.enum(listOf("quick_sale", "order", "reservation", "service_order", "rental"))
            )
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "paused", "archived")))
            .append("requiresScheduling", MongoMigrationSupport.bool())
            .append("tracksInventory", MongoMigrationSupport.bool())
            .append("allowsReceivables", MongoMigrationSupport.bool())
            .append("sortOrder", MongoMigrationSupport.int())
            .append("publicDiscovery", MongoMigrationSupport.obj())
            .append("assistedCommerce", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATION_ACTIVITIES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "code",
                    "name",
                    "activityType",
                    "workflowMode",
                    "status",
                    "requiresScheduling",
                    "tracksInventory",
                    "allowsReceivables",
                    "sortOrder",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "code"),
            name = "organization_activities_org_code_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status"),
            name = "organization_activities_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "activityType"),
            name = "organization_activities_org_type_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "sortOrder"),
            name = "organization_activities_org_sort_order_idx",
        )
    }
}
