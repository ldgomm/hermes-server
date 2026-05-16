package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M022CreateCatalogAttributeDefinitionsMigration : MongoMigration {
    override val id: String = "M022_create_catalog_attribute_definitions"
    override val description: String =
        "Create catalog attribute definitions for filterable search and future public discovery."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("key", MongoMigrationSupport.string(maxLength = 128))
            .append("label", MongoMigrationSupport.string(maxLength = 256))
            .append("categoryCode", MongoMigrationSupport.string(maxLength = 128))
            .append("businessTypeTags", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))
            .append(
                "attributeType",
                MongoMigrationSupport.enum(listOf("text", "decimal", "integer", "boolean", "enum"))
            )
            .append("required", MongoMigrationSupport.bool())
            .append("filterable", MongoMigrationSupport.bool())
            .append("allowedValues", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 256)))
            .append("unitCode", MongoMigrationSupport.nullableString(maxLength = 32))
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "deprecated", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_ATTRIBUTE_DEFINITIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "key",
                    "label",
                    "categoryCode",
                    "attributeType",
                    "required",
                    "filterable",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("categoryCode", "key"),
            "catalog_attribute_definitions_category_key_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("categoryCode", "filterable", "status"),
            "catalog_attribute_definitions_category_filterable_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("businessTypeTags", "status"),
            "catalog_attribute_definitions_business_tags_status_idx"
        )
    }
}
