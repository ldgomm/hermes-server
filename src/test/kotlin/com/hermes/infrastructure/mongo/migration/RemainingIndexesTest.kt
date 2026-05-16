package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import kotlin.test.Test
import kotlin.test.assertTrue

class RemainingIndexesTest {
    @Test
    fun `creates critical operational indexes`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_indexes_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            assertIndexExists(database.getCollection(MongoCollectionNames.SALES), "sales_org_number_unique_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.RESERVATIONS), "reservations_org_branch_activity_start_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.PAYMENTS), "payments_org_sale_status_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.CASH_MOVEMENTS), "cash_movements_org_session_occurred_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.COMMERCIAL_DOCUMENTS), "commercial_documents_org_access_key_unique_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.ELECTRONIC_SIGNATURES), "electronic_signatures_org_status_valid_to_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.STOCK_BALANCES), "stock_balances_org_branch_item_unique_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.OUTBOX_EVENTS), "outbox_events_status_available_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.CATALOG_IDENTIFIER_REGISTRY), "catalog_identifier_registry_type_value_scope_status_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.CATALOG_MEDIA_ASSETS), "catalog_media_assets_object_key_unique_idx")
            assertIndexExists(database.getCollection(MongoCollectionNames.SERVICE_AREAS), "service_areas_center_2dsphere_idx")
        }
    }

    private fun assertIndexExists(collection: com.mongodb.client.MongoCollection<org.bson.Document>, indexName: String) {
        val names = collection
            .listIndexes()
            .into(mutableListOf())
            .mapNotNull { it.getString("name") }
            .toSet()

        assertTrue(indexName in names, "Expected index '$indexName' in '${collection.namespace.collectionName}'. Found: $names")
    }
}
