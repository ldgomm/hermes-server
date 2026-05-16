package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

object M009CreateCustomersMigration : MongoMigration {
    override val id: String = "M009_create_customers"
    override val description: String =
        "Create customers collection for organization-owned billing and receivables customers."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("displayName", MongoMigrationSupport.string(maxLength = 256))
            .append("type", MongoMigrationSupport.enum(listOf("final_consumer", "person", "company", "foreign")))
            .append("identity", MongoMigrationSupport.obj())
            .append("contact", MongoMigrationSupport.obj())
            .append("billingProfiles", MongoMigrationSupport.array())
            .append("credit", MongoMigrationSupport.obj())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "blocked", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.CUSTOMERS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "displayName", "type", "identity", "contact", "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status", "displayName"),
            name = "customers_org_status_display_name_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "identity.idType", "identity.idNumber"),
            name = "customers_org_identity_unique_idx",
            unique = true,
            partialFilterExpression = Filters.exists("identity.idNumber", true),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.text("displayName")),
            name = "customers_org_display_name_text_idx",
        )
    }
}
