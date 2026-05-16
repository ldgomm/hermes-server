package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M026CreateServiceAreasMigration : MongoMigration {
    override val id: String = "M026_create_service_areas"
    override val description: String =
        "Create service areas for delivery, mobile services, tourism and branch coverage."

    override fun up(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("activityId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "coverageType",
                MongoMigrationSupport.enum(listOf("radius", "polygon", "province_city_sector", "manual_list"))
            )
            .append("center", MongoMigrationSupport.obj())
            .append("radiusMeters", MongoMigrationSupport.int())
            .append("geometry", MongoMigrationSupport.obj())
            .append("rules", MongoMigrationSupport.obj())
            .append("status", MongoMigrationSupport.enum(listOf("draft", "active", "inactive", "archived")))

        val collection = MongoMigrationSupport.ensureCollection(
            database,
            MongoCollectionNames.SERVICE_AREAS,
            MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "name",
                    "coverageType",
                    "rules",
                    "status"
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection,
            Indexes.ascending("organizationId", "branchId", "activityId", "status"),
            "service_areas_org_branch_activity_status_idx"
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.geo2dsphere("center"),
            "service_areas_center_2dsphere_idx",
            sparse = true
        )
        MongoMigrationSupport.createIndex(
            collection,
            Indexes.geo2dsphere("geometry"),
            "service_areas_geometry_2dsphere_idx",
            sparse = true
        )
    }
}
