package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemainingMongoMigrationsTest {
    @Test
    fun `applies full phase 4 migrations once and creates operational collections`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_migrations_test"))
            val runner = MongoMigrationRunner(database)

            val firstRun = runner.migrate(HermesMongoMigrations.all)
            val secondRun = runner.migrate(HermesMongoMigrations.all)

            assertEquals(28, firstRun.appliedCount)
            assertEquals(0, secondRun.appliedCount)
            assertEquals(28, secondRun.alreadyApplied)

            val collectionNames = database.listCollectionNames().into(mutableListOf()).toSet()
            expectedCollections.forEach { collectionName ->
                assertTrue(collectionName in collectionNames, "Expected collection '$collectionName' to exist.")
            }
        }
    }

    private val expectedCollections = setOf(
        MongoCollectionNames.CUSTOMERS,
        MongoCollectionNames.SALES,
        MongoCollectionNames.SERVICE_ORDERS,
        MongoCollectionNames.RESERVATIONS,
        MongoCollectionNames.RESERVATION_SLOTS,
        MongoCollectionNames.CAPACITY_RESOURCES,
        MongoCollectionNames.PAYMENTS,
        MongoCollectionNames.RECEIVABLES,
        MongoCollectionNames.CASH_SESSIONS,
        MongoCollectionNames.CASH_MOVEMENTS,
        MongoCollectionNames.COMMERCIAL_DOCUMENTS,
        MongoCollectionNames.ELECTRONIC_DOCUMENT_PAYLOADS,
        MongoCollectionNames.SRI_SUBMISSIONS,
        MongoCollectionNames.ELECTRONIC_SIGNATURES,
        MongoCollectionNames.ELECTRONIC_SIGNATURE_EVENTS,
        MongoCollectionNames.STOCK_BALANCES,
        MongoCollectionNames.STOCK_MOVEMENTS,
        MongoCollectionNames.STOCK_RESERVATIONS,
        MongoCollectionNames.AUDIT_LOGS,
        MongoCollectionNames.DOMAIN_EVENTS,
        MongoCollectionNames.ORGANIZATION_SETTINGS,
        MongoCollectionNames.FEATURE_FLAGS,
        MongoCollectionNames.COUNTERS,
        MongoCollectionNames.OUTBOX_EVENTS,
        MongoCollectionNames.CATALOG_IDENTIFIER_REGISTRY,
        MongoCollectionNames.CATALOG_IDENTITY_CONFLICTS,
        MongoCollectionNames.CATALOG_PRICE_HISTORY,
        MongoCollectionNames.CATALOG_ATTRIBUTE_DEFINITIONS,
        MongoCollectionNames.CATALOG_MEDIA_ASSETS,
        MongoCollectionNames.UNITS,
        MongoCollectionNames.UNIT_CONVERSIONS,
        MongoCollectionNames.BUSINESS_HOURS,
        MongoCollectionNames.SPECIAL_HOURS,
        MongoCollectionNames.TEMPORARY_CLOSURES,
        MongoCollectionNames.SERVICE_AREAS,
        MongoCollectionNames.CATALOG_IMPORT_JOBS,
        MongoCollectionNames.RETURN_POLICIES,
        MongoCollectionNames.WARRANTY_POLICIES,
    )
}
