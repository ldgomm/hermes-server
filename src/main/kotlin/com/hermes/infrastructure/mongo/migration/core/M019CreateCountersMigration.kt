package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M019CreateCountersMigration : MongoMigration {
    override val id: String = "M019_create_counters"
    override val description: String = "Create atomic counters for sale, cash and document sequences."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("scope", MongoMigrationSupport.enum(listOf("organization", "branch", "emission_point")))
            .append("scopeId", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "counterType",
                MongoMigrationSupport.enum(
                    listOf(
                        "sale",
                        "internal_ticket",
                        "physical_sale_note_registry",
                        "electronic_invoice",
                        "credit_note",
                        "debit_note",
                        "cash_session",
                        "catalog_request"
                    )
                )
            )
            .append("current", MongoMigrationSupport.int())
            .append("padding", MongoMigrationSupport.int())
            .append("prefix", MongoMigrationSupport.nullableString(maxLength = 32))
            .append("lastIssuedAt", MongoMigrationSupport.nullableDate())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.COUNTERS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "scope",
                    "scopeId",
                    "counterType",
                    "current",
                    "padding",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "scope", "scopeId", "counterType"),
            "counters_org_scope_type_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "counterType", "status"),
            "counters_org_type_status_idx"
        )
    }
}
