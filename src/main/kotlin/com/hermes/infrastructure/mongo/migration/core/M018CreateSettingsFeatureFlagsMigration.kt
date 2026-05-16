package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

object M018CreateSettingsFeatureFlagsMigration : MongoMigration {
    override val id: String = "M018_create_settings_feature_flags"
    override val description: String = "Create organization settings and feature flags collections."

    override fun up(database: MongoDatabase) {
        createOrganizationSettings(database)
        createFeatureFlags(database)
    }

    private fun createOrganizationSettings(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append(
                "settingsType",
                MongoMigrationSupport.enum(
                    listOf(
                        "general",
                        "sales",
                        "documents",
                        "cash",
                        "inventory",
                        "catalog",
                        "tax",
                        "public_discovery",
                        "assisted_commerce"
                    )
                )
            )
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))
            .append("payload", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.ORGANIZATION_SETTINGS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "settingsType",
                    "status",
                    "payload"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "settingsType"),
            "organization_settings_org_type_unique_idx",
            unique = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status"),
            "organization_settings_org_status_idx"
        )
    }

    private fun createFeatureFlags(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("key", MongoMigrationSupport.string(maxLength = 128))
            .append("scope", MongoMigrationSupport.enum(listOf("platform", "organization")))
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("enabled", MongoMigrationSupport.bool())
            .append("description", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("payload", MongoMigrationSupport.obj())
            .append("expiresAt", MongoMigrationSupport.nullableDate())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.FEATURE_FLAGS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "key",
                    "scope",
                    "enabled",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("key", "scope", "organizationId"),
            "feature_flags_key_scope_org_unique_idx",
            unique = true,
            partialFilterExpression = Filters.exists("key", true)
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("scope", "organizationId", "enabled"),
            "feature_flags_scope_org_enabled_idx"
        )
    }
}
