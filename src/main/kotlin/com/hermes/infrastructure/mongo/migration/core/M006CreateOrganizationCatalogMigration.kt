package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

object M006CreateOrganizationCatalogMigration : MongoMigration {
    override val id: String = "M006_create_organization_catalog"
    override val description: String = "Create local organization catalog items with search and identifier indexes."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("globalCatalogId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("templateId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("productFamilyId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("localName", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedName", MongoMigrationSupport.string(maxLength = 256))
            .append("description", MongoMigrationSupport.nullableString(maxLength = 4096))
            .append(
                "itemType",
                MongoMigrationSupport.enum(listOf("product", "service", "activity", "package", "rental", "fee"))
            )
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "inactive", "archived")))
            .append("localSku", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("identifiers", MongoMigrationSupport.array())
            .append("price", MongoMigrationSupport.moneyObject())
            .append("taxProfileId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("attributes", MongoMigrationSupport.obj())
            .append("variantAttributes", MongoMigrationSupport.obj())
            .append("media", MongoMigrationSupport.array())
            .append("searchableText", MongoMigrationSupport.string(maxLength = 4096))
            .append("publicDiscovery", MongoMigrationSupport.obj())
            .append("inventoryPolicy", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "activityId",
                    "localName",
                    "normalizedName",
                    "itemType",
                    "status",
                    "price",
                    "searchableText",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "activityId", "status"),
            name = "organization_catalog_items_org_activity_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "globalCatalogId"),
            name = "organization_catalog_items_org_global_id_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "identifiers.normalizedValue"),
            name = "organization_catalog_items_org_identifier_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "localSku"),
            name = "organization_catalog_items_org_local_sku_unique_idx",
            unique = true,
            partialFilterExpression = Filters.exists("localSku", true),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.text("searchableText")),
            name = "organization_catalog_items_org_searchable_text_idx",
        )
    }
}
