package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoDatabase
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexesTest {
    @Test
    fun `creates critical indexes for core collections`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(
                MongoIntegrationTestSupport.databaseName("phase_4_indexes_test"),
            )

            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            assertHasIndex(database, MongoCollectionNames.ORGANIZATIONS, "organizations_country_tax_id_unique_idx")
            assertHasIndex(
                database,
                MongoCollectionNames.ORGANIZATION_ACTIVITIES,
                "organization_activities_org_code_unique_idx"
            )
            assertHasIndex(database, MongoCollectionNames.BRANCHES, "branches_location_2dsphere_idx")
            assertHasIndex(database, MongoCollectionNames.EMISSION_POINTS, "emission_points_org_estab_pto_unique_idx")
            assertHasIndex(database, MongoCollectionNames.USERS, "users_email_unique_idx")
            assertHasIndex(database, MongoCollectionNames.MEMBERSHIPS, "memberships_org_user_unique_idx")
            assertHasIndex(
                database,
                MongoCollectionNames.PLATFORM_CATALOG_TEMPLATES,
                "platform_catalog_templates_global_id_unique_idx"
            )
            assertHasIndex(
                database,
                MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS,
                "organization_catalog_items_org_identifier_idx"
            )
            assertHasIndex(
                database,
                MongoCollectionNames.CATALOG_ITEM_REQUESTS,
                "catalog_item_requests_org_status_created_at_idx"
            )
            assertHasIndex(database, MongoCollectionNames.TAX_RATES, "tax_rates_country_sri_codes_effective_unique_idx")
            assertHasIndex(database, MongoCollectionNames.TAX_PROFILES, "tax_profiles_org_code_unique_idx")
            assertHasIndex(
                database,
                MongoCollectionNames.ORGANIZATION_TAX_SETTINGS,
                "organization_tax_settings_org_active_unique_idx"
            )
        }
    }

    private fun assertHasIndex(
        database: MongoDatabase,
        collectionName: String,
        indexName: String,
    ) {
        val names = database.getCollection(collectionName)
            .listIndexes()
            .into(mutableListOf())
            .mapNotNull { it.getString("name") }
            .toSet()

        assertTrue(
            actual = indexName in names,
            message = "Expected index '$indexName' in collection '$collectionName'. Found: $names",
        )
    }
}