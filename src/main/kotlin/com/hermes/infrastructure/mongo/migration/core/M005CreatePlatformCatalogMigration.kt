package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M005CreatePlatformCatalogMigration : MongoMigration {
    override val id: String = "M005_create_platform_catalog"
    override val description: String = "Create platform catalog family and template collections."

    override fun up(database: MongoDatabase) {
        createFamilies(database)
        createTemplates(database)
    }

    private fun createFamilies(database: MongoDatabase) {
        val properties = Document()
            .append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append("globalFamilyId", MongoMigrationSupport.string(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("categoryCode", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("type", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("status", MongoMigrationSupport.enum(TEMPLATE_STATUSES))
            .append("attributes", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PLATFORM_CATALOG_FAMILIES,
            validator = MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    "globalFamilyId",
                    "canonicalName",
                    "normalizedName",
                    "type",
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
            keys = Indexes.ascending("normalizedName"),
            name = "platform_catalog_families_normalized_name_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status"),
            name = "platform_catalog_families_status_idx",
        )
    }

    private fun createTemplates(database: MongoDatabase) {
        val properties = Document()
            .append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append("globalCatalogId", MongoMigrationSupport.string(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("type", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("status", MongoMigrationSupport.enum(TEMPLATE_STATUSES))
            .append("productFamilyId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("variantAttributes", MongoMigrationSupport.obj())
            .append("identifiers", MongoMigrationSupport.array())
            .append("attributes", MongoMigrationSupport.obj())
            .append("media", MongoMigrationSupport.array())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES,
            validator = MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    "globalCatalogId",
                    "canonicalName",
                    "normalizedName",
                    "type",
                    "status",
                    "variantAttributes",
                    "identifiers",
                    "attributes",
                    "media",
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
            keys = Indexes.ascending("type", "status"),
            name = "platform_catalog_templates_type_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("normalizedName"),
            name = "platform_catalog_templates_normalized_name_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("identifiers.normalizedValue"),
            name = "platform_catalog_templates_identifier_idx",
            sparse = true,
        )
    }

    private val CATALOG_ITEM_TYPES = listOf("PRODUCT", "SERVICE", "PACKAGE", "RENTAL", "FEE")
    private val TEMPLATE_STATUSES = listOf("DRAFT", "ACTIVE", "PAUSED", "ARCHIVED")
}
