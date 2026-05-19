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
    override val description: String = "Create platform catalog category, family and template collections."

    override fun up(database: MongoDatabase) {
        createCategories(database)
        createFamilies(database)
        createTemplates(database)
    }

    private fun createCategories(database: MongoDatabase) {
        val properties = Document().append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append("parentId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("code", MongoMigrationSupport.string(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("description", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("businessTypeTags", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append("activityTags", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append("status", MongoMigrationSupport.enum(CATEGORY_STATUSES))
            .append("sortOrder", MongoMigrationSupport.int())
            .append(MongoDocumentFields.CREATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.UPDATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.VERSION, MongoMigrationSupport.int())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PLATFORM_CATEGORIES,
            validator = MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    "code",
                    "name",
                    "normalizedName",
                    "businessTypeTags",
                    "activityTags",
                    "status",
                    "sortOrder",
                    MongoDocumentFields.CREATED_AT,
                    MongoDocumentFields.UPDATED_AT,
                    MongoDocumentFields.VERSION,
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("code"), "platform_categories_code_unique_idx", unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("parentId", "status", "sortOrder"),
            "platform_categories_parent_status_sort_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("status", "sortOrder"), "platform_categories_status_sort_idx"
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("normalizedName"), "platform_categories_normalized_name_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("businessTypeTags"),
            "platform_categories_business_type_tags_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("activityTags"), "platform_categories_activity_tags_idx", sparse = true
        )
    }

    private fun createFamilies(database: MongoDatabase) {
        val properties = Document().append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append("globalFamilyId", MongoMigrationSupport.string(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("categoryId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("brand", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("type", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("aliases", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append("attributes", MongoMigrationSupport.obj())
            .append("status", MongoMigrationSupport.enum(TEMPLATE_STATUSES))
            .append(MongoDocumentFields.CREATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.UPDATED_AT, MongoMigrationSupport.date())
            .append(MongoDocumentFields.VERSION, MongoMigrationSupport.int())

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
                    "aliases",
                    "attributes",
                    "status",
                    MongoDocumentFields.CREATED_AT,
                    MongoDocumentFields.UPDATED_AT,
                    MongoDocumentFields.VERSION,
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("globalFamilyId"),
            "platform_catalog_families_global_id_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("categoryId", "status"),
            "platform_catalog_families_category_status_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("type", "status"), "platform_catalog_families_type_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("normalizedName"), "platform_catalog_families_normalized_name_idx"
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("aliases"), "platform_catalog_families_aliases_idx", sparse = true
        )
    }

    private fun createTemplates(database: MongoDatabase) {
        val properties = Document().append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append("globalCatalogId", MongoMigrationSupport.string(maxLength = 128))
            .append("canonicalName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("type", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("status", MongoMigrationSupport.enum(TEMPLATE_STATUSES))
            .append("productFamilyId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("variantAttributes", MongoMigrationSupport.obj())
            .append("identifiers", MongoMigrationSupport.array()).append("attributes", MongoMigrationSupport.obj())
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
            collection,
            Indexes.ascending("globalCatalogId"),
            "platform_catalog_templates_global_id_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("productFamilyId", "status"),
            "platform_catalog_templates_family_status_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("type", "status"), "platform_catalog_templates_type_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection, Indexes.ascending("normalizedName"), "platform_catalog_templates_normalized_name_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("identifiers.normalizedValue"),
            "platform_catalog_templates_identifier_idx",
            sparse = true
        )
    }

    private val CATALOG_ITEM_TYPES = listOf("PRODUCT", "SERVICE", "PACKAGE", "RENTAL", "FEE")
    private val TEMPLATE_STATUSES = listOf("DRAFT", "ACTIVE", "PAUSED", "ARCHIVED")
    private val CATEGORY_STATUSES = listOf("DRAFT", "ACTIVE", "PAUSED", "ARCHIVED")
}
