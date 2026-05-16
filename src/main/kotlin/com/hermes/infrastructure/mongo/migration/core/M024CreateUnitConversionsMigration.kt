package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M024CreateUnitConversionsMigration : MongoMigration {
    override val id: String = "M024_create_unit_conversions"
    override val description: String = "Create units and unit conversion rules for purchase, sale and stock units."

    override fun up(database: MongoDatabase) {
        createUnits(database)
        createUnitConversions(database)
    }

    private fun createUnits(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("code", MongoMigrationSupport.string(maxLength = 32))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "type",
                MongoMigrationSupport.enum(
                    listOf(
                        "count",
                        "weight",
                        "volume",
                        "length",
                        "time",
                        "service",
                        "capacity",
                        "package"
                    )
                )
            )
            .append("allowsDecimal", MongoMigrationSupport.bool())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.UNITS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "code",
                    "name",
                    "type",
                    "allowsDecimal",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(collection, Indexes.ascending("code"), "units_code_unique_idx", unique = true)
        MongoMigrationSupport.createIndex(collection, Indexes.ascending("type", "status"), "units_type_status_idx")
    }

    private fun createUnitConversions(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("fromUnitCode", MongoMigrationSupport.string(maxLength = 32))
            .append("toUnitCode", MongoMigrationSupport.string(maxLength = 32))
            .append("factor", MongoMigrationSupport.decimal())
            .append("scope", MongoMigrationSupport.enum(listOf("platform", "organization", "catalog_item")))
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))
            .append("effectiveFrom", MongoMigrationSupport.date())
            .append("effectiveTo", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.UNIT_CONVERSIONS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "fromUnitCode",
                    "toUnitCode",
                    "factor",
                    "scope",
                    "status",
                    "effectiveFrom"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("scope", "organizationId", "catalogItemId", "fromUnitCode", "toUnitCode", "status"),
            "unit_conversions_scope_org_item_units_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("catalogItemId", "status"),
            "unit_conversions_catalog_item_status_idx",
            sparse = true
        )
    }
}
