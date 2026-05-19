package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M006CreateOrganizationCatalogMigration : MongoMigration {
    override val id: String = "M006_create_organization_catalog"
    override val description: String = "Create local organization catalog items with search and identifier indexes."

    override fun up(database: MongoDatabase) {
        val properties = Document().append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append(MongoDocumentFields.ORGANIZATION_ID, MongoMigrationSupport.id(prefix = "org_"))
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.id(prefix = "act_"))
            .append("templateId", MongoMigrationSupport.string(maxLength = 128))
            .append("globalCatalogId", MongoMigrationSupport.string(maxLength = 128))
            .append("localName", MongoMigrationSupport.string(maxLength = 256))
            .append("searchableText", MongoMigrationSupport.string(maxLength = 4096))
            .append("type", MongoMigrationSupport.enum(CATALOG_ITEM_TYPES))
            .append("status", MongoMigrationSupport.enum(CATALOG_ITEM_STATUSES))
            .append("localPrice", MongoMigrationSupport.moneyObject())
            .append("taxProfileId", MongoMigrationSupport.string(maxLength = 128))
            .append("publicDiscoveryStatus", MongoMigrationSupport.enum(PUBLIC_DISCOVERY_STATUSES))
            .append("productFamilyId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("variantAttributes", MongoMigrationSupport.obj())
            .append("identifiers", MongoMigrationSupport.array()).append("attributes", MongoMigrationSupport.obj())
            .append("media", MongoMigrationSupport.array())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
            validator = MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    MongoDocumentFields.ORGANIZATION_ID,
                    "activityId",
                    "templateId",
                    "globalCatalogId",
                    "localName",
                    "searchableText",
                    "type",
                    "status",
                    "localPrice",
                    "taxProfileId",
                    "publicDiscoveryStatus",
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
            keys = Indexes.ascending("organizationId", "status"),
            name = "organization_catalog_items_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "branchId", "status"),
            name = "organization_catalog_items_org_branch_status_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "activityId", "status"),
            name = "organization_catalog_items_org_activity_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "searchableText"),
            name = "organization_catalog_items_org_searchable_text_ascending_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "identifiers.normalizedValue"),
            name = "organization_catalog_items_org_identifier_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "globalCatalogId"),
            name = "organization_catalog_items_org_global_id_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "templateId"),
            name = "organization_catalog_items_org_template_idx",
            sparse = true,
        )
    }

    private val CATALOG_ITEM_TYPES = listOf("PRODUCT", "SERVICE", "PACKAGE", "RENTAL", "FEE")
    private val CATALOG_ITEM_STATUSES =
        listOf("DRAFT", "ACTIVE", "PAUSED", "OUT_OF_STOCK", "ARCHIVED", "REMOVED_FROM_ACCOUNT")
    private val PUBLIC_DISCOVERY_STATUSES = listOf(
        "PRIVATE",
        "PUBLIC_PENDING_REVIEW",
        "PUBLIC",
        "HIDDEN_TEMPORARILY",
        "SUSPENDED_BY_PLATFORM",
        "ARCHIVED",
    )
}
