package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.MongoWriteException
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.bson.Document
import org.bson.types.Decimal128

class RemainingValidatorsTest {
    @Test
    fun `customer validator rejects documents without organization data`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_customer_validator_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            val customers = database.getCollection(MongoCollectionNames.CUSTOMERS)

            assertFailsWith<MongoWriteException> {
                customers.insertOne(Document("_id", "cus_invalid"))
            }

            customers.insertOne(validCustomer("cus_001", "0503638371001"))
            assertEquals(1, customers.countDocuments())
        }
    }

    @Test
    fun `sales validator requires separated states and financial snapshots`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_sales_validator_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            val sales = database.getCollection(MongoCollectionNames.SALES)

            assertFailsWith<MongoWriteException> {
                sales.insertOne(baseRoot("sale_invalid", "org_001"))
            }

            sales.insertOne(validSale("sale_001"))
            assertEquals(1, sales.countDocuments())
        }
    }

    @Test
    fun `stock balance validator requires decimal quantities`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_stock_validator_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            val stockBalances = database.getCollection(MongoCollectionNames.STOCK_BALANCES)

            assertFailsWith<MongoWriteException> {
                stockBalances.insertOne(
                    baseRoot("stock_invalid", "org_001")
                        .append("branchId", "br_001")
                        .append("catalogItemId", "item_001")
                        .append("stockUnit", "unit")
                        .append("quantityOnHand", "10")
                        .append("quantityReserved", decimal("0"))
                        .append("quantityAvailable", decimal("10"))
                        .append("lowStockThreshold", decimal("2"))
                        .append("status", "available"),
                )
            }

            stockBalances.insertOne(validStockBalance("stock_001"))
            assertEquals(1, stockBalances.countDocuments())
        }
    }

    @Test
    fun `outbox validator requires payload and delivery state`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_remaining_outbox_validator_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            val outbox = database.getCollection(MongoCollectionNames.OUTBOX_EVENTS)

            assertFailsWith<MongoWriteException> {
                outbox.insertOne(baseRoot("out_invalid", "org_001"))
            }

            outbox.insertOne(validOutboxEvent("out_001"))
            assertEquals(1, outbox.countDocuments())
        }
    }

    private fun validCustomer(id: String, idNumber: String): Document =
        baseRoot(id, "org_001")
            .append("displayName", "Cliente de prueba")
            .append("type", "person")
            .append("identity", Document("idType", "cedula").append("idNumber", idNumber).append("legalName", "Cliente de prueba"))
            .append("contact", Document("email", "cliente@example.com"))
            .append("billingProfiles", emptyList<Document>())
            .append("credit", Document("allowCredit", false))
            .append("status", "active")

    private fun validSale(id: String): Document =
        baseRoot(id, "org_001")
            .append("branchId", "br_001")
            .append("activityId", "act_001")
            .append("saleNumber", "S-2026-000001")
            .append("saleType", "standard_sale")
            .append("workflowMode", "quick_sale")
            .append("operationalStatus", "draft")
            .append("paymentStatus", "unpaid")
            .append("collectionStatus", "not_applicable")
            .append("documentStatus", "not_required")
            .append("customerId", null)
            .append("customerSnapshot", Document())
            .append("items", emptyList<Document>())
            .append("totals", Document("grandTotal", money("0.00")))
            .append("taxSummary", Document("taxLines", emptyList<Document>()))
            .append("paymentRefs", emptyList<Document>())
            .append("documentRefs", emptyList<Document>())
            .append("reservationRef", null)
            .append("cashSessionId", null)
            .append("confirmedAt", null)
            .append("closedAt", null)
            .append("canceledAt", null)

    private fun validStockBalance(id: String): Document =
        baseRoot(id, "org_001")
            .append("branchId", "br_001")
            .append("catalogItemId", "item_001")
            .append("stockUnit", "unit")
            .append("quantityOnHand", decimal("10"))
            .append("quantityReserved", decimal("0"))
            .append("quantityAvailable", decimal("10"))
            .append("lowStockThreshold", decimal("2"))
            .append("status", "available")
            .append("lastMovementAt", null)

    private fun validOutboxEvent(id: String): Document =
        baseRoot(id, "org_001")
            .append("eventId", "evt_001")
            .append("eventType", "SaleCreated")
            .append("aggregateType", "Sale")
            .append("aggregateId", "sale_001")
            .append("payload", Document("saleId", "sale_001"))
            .append("status", "pending")
            .append("availableAt", now())
            .append("publishedAt", null)
            .append("attempts", 0)
            .append("lastError", null)

    private fun baseRoot(id: String, organizationId: String): Document =
        Document("_id", id)
            .append("organizationId", organizationId)
            .append("createdAt", now())
            .append("createdBy", "usr_001")
            .append("updatedAt", now())
            .append("updatedBy", "usr_001")
            .append("version", 1)
            .append("schemaVersion", 1)

    private fun now(): Date = Date.from(Instant.parse("2026-05-16T00:00:00Z"))

    private fun decimal(value: String): Decimal128 = Decimal128.parse(value)

    private fun money(amount: String): Document = Document("amount", decimal(amount)).append("currency", "USD")
}
