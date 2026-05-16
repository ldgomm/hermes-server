package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

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
            .append(
                "identifierType",
                MongoMigrationSupport.enum(
                    listOf(
                        "sku_master",
                        "sku_local",
                        "internal_code",
                        "supplier_code",
                        "barcode",
                        "gtin",
                        "ean_8",
                        "ean_13",
                        "upc_a",
                        "isbn",
                        "manufacturer_part_number"
                    )
                )
            )
            .append("value", MongoMigrationSupport.string(maxLength = 256))
            .append("normalizedValue", MongoMigrationSupport.string(maxLength = 256))
            .append("scope", MongoMigrationSupport.enum(listOf("global", "organization", "branch")))
            .append("source", MongoMigrationSupport.enum(listOf("platform", "organization", "supplier", "import")))
            .append(
                "status",
                MongoMigrationSupport.enum(
                    listOf(
                        "proposed",
                        "active",
                        "verified",
                        "conflict",
                        "deprecated",
                        "rejected"
                    )
                )
            )
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
            "catalog_identifier_registry_type_value_scope_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "identifierType", "normalizedValue"),
            "catalog_identifier_registry_org_type_value_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("ownerType", "ownerId"),
            "catalog_identifier_registry_owner_idx"
        )
    }

    private fun createIdentityConflicts(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append(
                "conflictType",
                MongoMigrationSupport.enum(
                    listOf(
                        "duplicate_global_identifier",
                        "duplicate_local_sku",
                        "family_variant_duplicate",
                        "template_merge_candidate"
                    )
                )
            )
            .append(
                "status",
                MongoMigrationSupport.enum(listOf("open", "under_review", "resolved", "rejected", "archived"))
            )
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
                    "ownerRefs"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("status", "conflictType", "updatedAt"),
            "catalog_identity_conflicts_status_type_updated_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("identifierType", "normalizedValue", "status"),
            "catalog_identity_conflicts_identifier_status_idx",
            sparse = true
        )
    }

    private fun createPriceHistory(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.id(prefix = "item_"))
            .append("oldPrice", MongoMigrationSupport.moneyObject())
            .append("newPrice", MongoMigrationSupport.moneyObject())
            .append("priceIncludesTax", MongoMigrationSupport.bool())
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("changedBy", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("reason", MongoMigrationSupport.nullableString(maxLength = 2048))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.CATALOG_PRICE_HISTORY,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "catalogItemId",
                    "oldPrice",
                    "newPrice",
                    "priceIncludesTax",
                    "effectiveFrom"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "catalogItemId", "effectiveFrom"),
            "catalog_price_history_org_item_effective_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "effectiveFrom"),
            "catalog_price_history_org_branch_effective_idx",
            sparse = true
        )
    }
}
