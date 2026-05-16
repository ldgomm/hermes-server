package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MongoMigrationsTest {
    @Test
    fun `applies phase 4 1 migrations once and creates core collections`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_1_migrations_test"))
            val runner = MongoMigrationRunner(database)

            val firstRun = runner.migrate(HermesMongoMigrations.phase41)
            val secondRun = runner.migrate(HermesMongoMigrations.phase41)

            assertEquals(8, firstRun.appliedCount)
            assertEquals(0, secondRun.appliedCount)
            assertEquals(8, secondRun.alreadyApplied)

            val collectionNames = database.listCollectionNames().into(mutableListOf()).toSet()
            expectedCollections.forEach { collectionName ->
                assertTrue(collectionName in collectionNames, "Expected collection '$collectionName' to exist.")
            }
        }
    }

    private val expectedCollections = setOf(
        MongoCollectionNames.ORGANIZATIONS,
        MongoCollectionNames.ORGANIZATION_ACTIVITIES,
        MongoCollectionNames.BRANCHES,
        MongoCollectionNames.EMISSION_POINTS,
        MongoCollectionNames.USERS,
        MongoCollectionNames.MEMBERSHIPS,
        MongoCollectionNames.ROLES,
        MongoCollectionNames.PERMISSIONS,
        MongoCollectionNames.CREDENTIAL_EVENTS,
        MongoCollectionNames.USER_SESSIONS,
        MongoCollectionNames.PLATFORM_CATALOG_FAMILIES,
        MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES,
        MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
        MongoCollectionNames.CATALOG_ITEM_REQUESTS,
        MongoCollectionNames.TAX_RATES,
        MongoCollectionNames.TAX_PROFILES,
        MongoCollectionNames.ORGANIZATION_TAX_SETTINGS,
    )
}
