package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.migration.core.*

object HermesMongoMigrations {
    val phase41: List<MongoMigration> = listOf(
        M001CreateOrganizationsMigration,
        M002CreateOrganizationActivitiesMigration,
        M003CreateBranchesAndEmissionPointsMigration,
        M004CreateUsersRolesPermissionsCredentialsMigration,
        M005CreatePlatformCatalogMigration,
        M006CreateOrganizationCatalogMigration,
        M007CreateCatalogRequestsMigration,
        M008CreateTaxEngineMigration,
    )

    val all: List<MongoMigration> = phase41
}
