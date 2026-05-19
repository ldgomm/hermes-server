package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M001CreateOrganizationsMigration : MongoMigration {
    override val id: String = "M001_create_organizations"
    override val description: String = "Create organizations collection with validators and core indexes."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("legalName", MongoMigrationSupport.string(maxLength = 256))
            .append("commercialName", MongoMigrationSupport.string(maxLength = 256))
            .append("taxId", MongoMigrationSupport.string(minLength = 10, maxLength = 13)).append(
                "taxIdType", MongoMigrationSupport.enum(listOf("ruc", "cedula", "passport", "final_consumer_internal"))
            ).append("countryCode", MongoMigrationSupport.enum(listOf("EC")))
            .append("timezone", MongoMigrationSupport.string(maxLength = 64))
            .append("defaultCurrency", MongoMigrationSupport.enum(listOf("USD"))).append(
                "taxRegime", MongoMigrationSupport.enum(
                    listOf(
                        "rimpe_popular", "rimpe_entrepreneur", "general", "unknown", "custom_verified"
                    )
                )
            ).append("businessModel", MongoMigrationSupport.enum(listOf("single_activity", "multi_activity")))
            .append("primaryBusinessType", MongoMigrationSupport.string(maxLength = 64))
            .append("status", MongoMigrationSupport.enum(listOf("onboarding", "active", "suspended", "archived")))
            .append("contact", MongoMigrationSupport.obj()).append("branding", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ORGANIZATIONS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "legalName",
                    "commercialName",
                    "taxId",
                    "taxIdType",
                    "countryCode",
                    "timezone",
                    "defaultCurrency",
                    "taxRegime",
                    "businessModel",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "taxId"),
            name = "organizations_country_tax_id_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status", "createdAt"),
            name = "organizations_status_created_at_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.text("commercialName"),
            name = "organizations_commercial_name_text_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("countryCode", "status"),
            name = "organizations_country_status_idx",
        )
    }
}
