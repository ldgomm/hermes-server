package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M028CreateReturnAndWarrantyPoliciesFutureMigration : MongoMigration {
    override val id: String = "M028_create_return_and_warranty_policies_future"
    override val description: String =
        "Create future return and warranty policy collections without turning returns into cancellation flows."

    override fun up(database: MongoDatabase) {
        createReturnPolicies(database)
        createWarrantyPolicies(database)
    }

    private fun createReturnPolicies(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("scope", MongoMigrationSupport.enum(listOf("organization", "category", "catalog_item")))
            .append("categoryCode", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "policyType",
                MongoMigrationSupport.enum(listOf("final_sale", "exchange_only", "refund_allowed", "warranty_only"))
            )
            .append("returnWindowDays", MongoMigrationSupport.int())
            .append("requiresOriginalReceipt", MongoMigrationSupport.bool())
            .append("restockingAllowed", MongoMigrationSupport.bool())
            .append("conditions", MongoMigrationSupport.obj())
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.RETURN_POLICIES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "name",
                    "scope",
                    "policyType",
                    "returnWindowDays",
                    "requiresOriginalReceipt",
                    "restockingAllowed",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "scope", "categoryCode", "catalogItemId", "status"),
            "return_policies_org_scope_refs_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "policyType", "status"),
            "return_policies_org_type_status_idx"
        )
    }

    private fun createWarrantyPolicies(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("scope", MongoMigrationSupport.enum(listOf("organization", "category", "catalog_item")))
            .append("categoryCode", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("warrantyType", MongoMigrationSupport.enum(listOf("none", "days", "months", "manufacturer")))
            .append("durationDays", MongoMigrationSupport.int())
            .append("provider", MongoMigrationSupport.nullableString(maxLength = 256))
            .append("conditions", MongoMigrationSupport.obj())
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.WARRANTY_POLICIES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "name",
                    "scope",
                    "warrantyType",
                    "durationDays",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "scope", "categoryCode", "catalogItemId", "status"),
            "warranty_policies_org_scope_refs_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "warrantyType", "status"),
            "warranty_policies_org_type_status_idx"
        )
    }
}
