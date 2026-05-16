package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M025CreateBusinessHoursSpecialHoursMigration : MongoMigration {
    override val id: String = "M025_create_business_hours_special_hours"
    override val description: String = "Create normal hours, special hours and temporary closure collections."

    override fun up(database: MongoDatabase) {
        createBusinessHours(database)
        createSpecialHours(database)
        createTemporaryClosures(database)
    }

    private fun createBusinessHours(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("scope", MongoMigrationSupport.enum(listOf("organization", "branch", "activity", "catalog_item")))
            .append("timezone", MongoMigrationSupport.string(maxLength = 64))
            .append("weeklySchedule", MongoMigrationSupport.array())
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.BUSINESS_HOURS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "scope",
                    "timezone",
                    "weeklySchedule",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "scope", "branchId", "activityId", "catalogItemId"),
            "business_hours_org_scope_refs_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status"),
            "business_hours_org_status_idx"
        )
    }

    private fun createSpecialHours(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("date", MongoMigrationSupport.date())
            .append("timezone", MongoMigrationSupport.string(maxLength = 64))
            .append("intervals", MongoMigrationSupport.array())
            .append("reason", MongoMigrationSupport.nullableString(maxLength = 1024))
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.SPECIAL_HOURS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "date",
                    "timezone",
                    "intervals",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "activityId", "date"),
            "special_hours_org_branch_activity_date_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "catalogItemId", "date"),
            "special_hours_org_item_date_idx",
            sparse = true
        )
    }

    private fun createTemporaryClosures(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("catalogItemId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("startsAt", MongoMigrationSupport.date())
            .append("endsAt", MongoMigrationSupport.date())
            .append("reason", MongoMigrationSupport.string(maxLength = 1024))
            .append("status", MongoMigrationSupport.enum(listOf("scheduled", "active", "finished", "canceled")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.TEMPORARY_CLOSURES,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "startsAt",
                    "endsAt",
                    "reason",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "startsAt", "endsAt"),
            "temporary_closures_org_branch_time_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "status", "startsAt"),
            "temporary_closures_org_status_start_idx"
        )
    }
}
