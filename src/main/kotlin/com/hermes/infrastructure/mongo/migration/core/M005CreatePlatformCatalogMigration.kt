package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M005CreatePlatformCatalogMigration : MongoMigration {
    override val id: String = "M005_create_platform_catalog"
    override val description: String = "Create platform catalog family and template collections."

    override fun up(database: MongoDatabase) {
        createFamilies(database)
        createTemplates(database)
    }

    private fun createFamilies(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("globalFamilyId", MongoMigrationSupport.string(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("categoryCode", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "itemType",
                MongoMigrationSupport.enum(listOf("product", "service", "activity", "package", "rental", "fee"))
            )
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "deprecated", "archived")))
            .append("searchKeywords", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append("semanticTags", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PLATFORM_CATALOG_FAMILIES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "globalFamilyId",
                    "canonicalName",
                    "normalizedName",
                    "categoryCode",
                    "itemType",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("globalFamilyId"),
            name = "platform_catalog_families_global_id_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("categoryCode", "status"),
            name = "platform_catalog_families_category_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.text("canonicalName"),
            name = "platform_catalog_families_name_text_idx",
        )
    }

    private fun createTemplates(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("globalCatalogId", MongoMigrationSupport.string(maxLength = 128))
            .append("productFamilyId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("brand", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("categoryCode", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "itemType",
                MongoMigrationSupport.enum(listOf("product", "service", "activity", "package", "rental", "fee"))
            )
            .append("status", MongoMigrationSupport.enum(listOf("draft", "published", "deprecated", "archived")))
            .append("identifiers", MongoMigrationSupport.array())
            .append("variantAttributes", MongoMigrationSupport.obj())
            .append("attributes", MongoMigrationSupport.obj())
            .append("media", MongoMigrationSupport.array())
            .append("searchKeywords", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append("semanticTags", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "globalCatalogId",
                    "canonicalName",
                    "normalizedName",
                    "categoryCode",
                    "itemType",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("globalCatalogId"),
            name = "platform_catalog_templates_global_id_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("productFamilyId", "status"),
            name = "platform_catalog_templates_family_status_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("identifiers.normalizedValue"),
            name = "platform_catalog_templates_identifier_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.text("canonicalName"),
            name = "platform_catalog_templates_name_text_idx",
        )
    }
}
