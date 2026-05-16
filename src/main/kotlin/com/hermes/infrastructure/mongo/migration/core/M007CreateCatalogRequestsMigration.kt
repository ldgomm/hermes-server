package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M007CreateCatalogRequestsMigration : MongoMigration {
    override val id: String = "M007_create_catalog_requests"
    override val description: String = "Create catalog item requests collection."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("requestedName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append(
                "itemType",
                MongoMigrationSupport.enum(listOf("product", "service", "activity", "package", "rental", "fee"))
            )
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "draft",
                        "submitted",
                        "in_review",
                        "approved",
                        "rejected",
                        "canceled"
                    )
                )
            )
            .append("requestedBy", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reviewedBy", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reviewedAt", MongoMigrationSupport.nullableDate())
            .append("resultTemplateId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("notes", MongoMigrationSupport.nullableString(maxLength = 4096))
            .append("payload", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.CATALOG_ITEM_REQUESTS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "requestedName",
                    "normalizedName",
                    "itemType",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status", "createdAt"),
            name = "catalog_item_requests_org_status_created_at_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status", "createdAt"),
            name = "catalog_item_requests_status_created_at_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.text("requestedName"),
            name = "catalog_item_requests_requested_name_text_idx",
        )
    }
}
