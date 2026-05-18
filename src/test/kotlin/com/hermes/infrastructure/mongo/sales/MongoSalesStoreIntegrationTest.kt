package com.hermes.infrastructure.mongo.sales

import com.hermes.application.sales.ReservationSearchQuery
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import org.bson.Document
import org.bson.types.Decimal128
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MongoSalesStoreIntegrationTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeEach
    fun setUp() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_sales_store_test")
    }

    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `stores and reads sale preserving nested Decimal128 money`() {
        val database = client.getDatabase(databaseName)
        val store = MongoSalesStore(database)
        val sale = mongoConfirmedSale()

        store.saleRepository.create(sale)

        val persisted = store.saleRepository.findById("org_1", "sale_1")
        assertNotNull(persisted)
        assertEquals(sale.id, persisted.id)
        assertEquals(SaleOperationalStatus.CONFIRMED, persisted.operationalStatus)
        assertEquals(sale.totals.grandTotal, persisted.totals.grandTotal)

        val raw = database.getCollection(MongoCollectionNames.SALES)
            .find(Document("_id", "sale_1"))
            .first()
        assertNotNull(raw)
        val totals = raw["totals"] as Document
        val grandTotal = totals["grandTotal"] as Document
        assertTrue(grandTotal["amount"] is Decimal128)
    }

    @Test
    fun `searches sales by organization status customer and activity`() {
        val database = client.getDatabase(databaseName)
        val store = MongoSalesStore(database)
        store.saleRepository.create(mongoConfirmedSale(id = "sale_1", customerId = "cust_1"))
        store.saleRepository.create(mongoDraftSale(id = "sale_2", customerId = "cust_1"))
        store.saleRepository.create(mongoConfirmedSale(id = "sale_3", customerId = "cust_2", activityId = "act_retail"))

        val result = store.saleRepository.search(
            SaleSearchQuery(
                organizationId = "org_1",
                statuses = setOf(SaleOperationalStatus.CONFIRMED),
                customerId = "cust_1",
                activityId = "act_restaurant",
            )
        )

        assertEquals(listOf("sale_1"), result.map { it.id })
    }

    @Test
    fun `stores reads and searches reservations`() {
        val database = client.getDatabase(databaseName)
        val store = MongoSalesStore(database)
        store.reservationRepository.create(mongoReservation(id = "res_1", activityId = "act_tourism"))
        store.reservationRepository.create(mongoReservation(id = "res_2", activityId = "act_restaurant"))

        val persisted = store.reservationRepository.findById("org_1", "res_1")
        assertNotNull(persisted)
        assertEquals(ReservationStatus.SCHEDULED, persisted.status)
        assertEquals("sale_1", persisted.saleId)

        val result = store.reservationRepository.search(
            ReservationSearchQuery(
                organizationId = "org_1",
                statuses = setOf(ReservationStatus.SCHEDULED),
                activityId = "act_tourism",
                from = MongoSalesTestNow,
                to = MongoSalesTestNow.plusSeconds(10_000),
            )
        )

        assertEquals(listOf("res_1"), result.map { it.id })
    }
}
