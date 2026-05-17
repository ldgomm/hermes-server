package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes
import org.bson.Document

object M021CreateCatalogIdentityFoundationMigration : MongoMigration {
    override val id: String = "M021_create_catalog_identity_foundation"
    override val description: String =
        "Create catalog identity registry, conflict tracking and price history foundation."

    override fun up(database: MongoDatabase) {
        createIdentifierRegistry(database)
        createIdentityConflicts(database)
        createPriceHistory(database)
    }

    private fun createIdentifierRegistry(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("ownerType", MongoMigrationSupport.enum(listOf("platform_template", "organization_item")))
            .append("ownerId", MongoMigrationSupport.string(maxLength = 128))
            .append("identifierType", MongoMigrationSupport.enum(IDENTIFIER_TYPES))
            .append("value", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedValue", MongoMigrationSupport.string(maxLength = 256))
            .append("scope", MongoMigrationSupport.enum(IDENTIFIER_SCOPES))
            .append("source", MongoMigrationSupport.enum(IDENTIFIER_SOURCES))
            .append("status", MongoMigrationSupport.enum(IDENTIFIER_STATUSES))
            .append("isPrimary", MongoMigrationSupport.bool())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_IDENTIFIER_REGISTRY,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "ownerType",
                    "ownerId",
                    "identifierType",
                    "value",
                    "normalizedValue",
                    "scope",
                    "source",
                    "status",
                    "isPrimary",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("identifierType", "normalizedValue", "scope", "status"),
            "catalog_identifier_registry_type_value_scope_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "identifierType", "normalizedValue"),
            "catalog_identifier_registry_org_type_value_idx",
            sparse = true,
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("ownerType", "ownerId"),
            "catalog_identifier_registry_owner_idx",
        )
    }

    private fun createIdentityConflicts(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("conflictType", MongoMigrationSupport.enum(CONFLICT_TYPES))
            .append("status", MongoMigrationSupport.enum(CONFLICT_STATUSES))
            .append("identifierType", MongoMigrationSupport.nullableString(maxLength = 64))
            .append("normalizedValue", MongoMigrationSupport.nullableString(maxLength = 256))
            .append("ownerRefs", MongoMigrationSupport.array())
            .append("resolution", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_IDENTITY_CONFLICTS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "conflictType",
                    "status",
                    "ownerRefs",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("status", "conflictType", "updatedAt"),
            "catalog_identity_conflicts_status_type_updated_idx",
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("identifierType", "normalizedValue", "status"),
            "catalog_identity_conflicts_identifier_status_idx",
            sparse = true,
        )
    }

    private fun createPriceHistory(database: MongoDatabase) {
        val properties = Document()
            .append(MongoDocumentFields.ID, MongoMigrationSupport.id())
            .append(MongoDocumentFields.ORGANIZATION_ID, MongoMigrationSupport.id(prefix = "org_"))
            .append("catalogItemId", MongoMigrationSupport.id())
            .append("oldPrice", MongoMigrationSupport.moneyObject())
            .append("newPrice", MongoMigrationSupport.moneyObject())
            .append("changedByUserId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("reason", MongoMigrationSupport.string(maxLength = 2048))
            .append("changedAt", MongoMigrationSupport.date())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_PRICE_HISTORY,
            MongoMigrationSupport.jsonSchema(
                required = listOf(
                    MongoDocumentFields.ID,
                    MongoDocumentFields.ORGANIZATION_ID,
                    "catalogItemId",
                    "oldPrice",
                    "newPrice",
                    "changedByUserId",
                    "reason",
                    "changedAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "catalogItemId", "changedAt"),
            "catalog_price_history_org_item_changed_at_idx",
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "changedAt"),
            "catalog_price_history_org_changed_at_idx",
        )
    }

    private val IDENTIFIER_TYPES = listOf(
        "SKU_MASTER",
        "SKU_LOCAL",
        "INTERNAL_CODE",
        "SUPPLIER_CODE",
        "BARCODE",
        "GTIN",
        "EAN_8",
        "EAN_13",
        "UPC_A",
        "ISBN",
        "MANUFACTURER_PART_NUMBER",
    )
    private val IDENTIFIER_SCOPES = listOf("GLOBAL", "ORGANIZATION", "BRANCH")
    private val IDENTIFIER_SOURCES = listOf("PLATFORM", "ORGANIZATION", "SUPPLIER", "IMPORT")
    private val IDENTIFIER_STATUSES = listOf("PROPOSED", "ACTIVE", "VERIFIED", "CONFLICT", "DEPRECATED", "REJECTED")
    private val CONFLICT_TYPES = listOf("duplicate_global_identifier", "duplicate_local_sku", "family_variant_duplicate", "template_merge_candidate")
    private val CONFLICT_STATUSES = listOf("open", "under_review", "resolved", "rejected", "archived")
}
