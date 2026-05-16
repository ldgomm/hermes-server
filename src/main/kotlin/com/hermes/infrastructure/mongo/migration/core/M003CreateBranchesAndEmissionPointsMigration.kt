package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Indexes

object M003CreateBranchesAndEmissionPointsMigration : MongoMigration {
    override val id: String = "M003_create_branches_and_emission_points"
    override val description: String = "Create branches and emission points collections."

    override fun up(database: MongoDatabase) {
        createBranches(database)
        createEmissionPoints(database)
    }

    private fun createBranches(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("code", MongoMigrationSupport.string(maxLength = 16))
            .append("type", MongoMigrationSupport.enum(listOf("main", "branch", "warehouse", "mobile", "virtual")))
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))
            .append("location", MongoMigrationSupport.obj())
            .append("contact", MongoMigrationSupport.obj())
            .append("businessHoursId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("publicDiscovery", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.BRANCHES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "name",
                    "code",
                    "type",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "code"),
            name = "branches_org_code_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "status"),
            name = "branches_org_status_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.geo2dsphere("location.coordinates"),
            name = "branches_location_2dsphere_idx",
        )
    }

    private fun createEmissionPoints(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("branchId", MongoMigrationSupport.id(prefix = "br_"))
            .append("establishmentCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("emissionPointCode", MongoMigrationSupport.string(minLength = 3, maxLength = 3))
            .append("displayName", MongoMigrationSupport.string(maxLength = 128))
            .append("status", MongoMigrationSupport.enum(listOf("active", "inactive", "archived")))
            .append("documentSequences", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.EMISSION_POINTS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "branchId",
                    "establishmentCode",
                    "emissionPointCode",
                    "displayName",
                    "status",
                    "documentSequences",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "establishmentCode", "emissionPointCode"),
            name = "emission_points_org_estab_pto_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "branchId", "status"),
            name = "emission_points_org_branch_status_idx",
        )
    }
}
