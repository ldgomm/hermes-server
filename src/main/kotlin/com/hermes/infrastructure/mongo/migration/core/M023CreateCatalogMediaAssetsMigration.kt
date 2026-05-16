package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M023CreateCatalogMediaAssetsMigration : MongoMigration {
    override val id: String = "M023_create_catalog_media_assets"
    override val description: String = "Create catalog media asset metadata for object-storage-backed images."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("ownerKind", MongoMigrationSupport.enum(listOf("master", "local")))
            .append(
                "ownerType",
                MongoMigrationSupport.enum(listOf("platform_template", "organization_item", "category", "family"))
            )
            .append("ownerId", MongoMigrationSupport.string(maxLength = 128))
            .append("objectKey", MongoMigrationSupport.string(maxLength = 512))
            .append("publicUrl", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("mimeType", MongoMigrationSupport.enum(listOf("image/jpeg", "image/png", "image/webp")))
            .append(
                "status",
                MongoMigrationSupport.enum(listOf("pending", "approved", "rejected", "hidden", "deleted"))
            )
            .append("isPrimary", MongoMigrationSupport.bool())
            .append("sortOrder", MongoMigrationSupport.int())
            .append("review", MongoMigrationSupport.obj())
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_MEDIA_ASSETS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "ownerKind",
                    "ownerType",
                    "ownerId",
                    "objectKey",
                    "mimeType",
                    "status",
                    "isPrimary",
                    "sortOrder"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("ownerType", "ownerId", "status", "sortOrder"),
            "catalog_media_assets_owner_status_sort_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "ownerType", "ownerId"),
            "catalog_media_assets_org_owner_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("objectKey"),
            "catalog_media_assets_object_key_unique_idx",
            unique = true
        )
    }
}
