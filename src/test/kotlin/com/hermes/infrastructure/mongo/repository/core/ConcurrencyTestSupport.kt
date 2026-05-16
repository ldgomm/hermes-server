package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object ConcurrencyTestSupport {
    const val ORGANIZATION_ID = "org_phase45"
    const val BRANCH_ID = "br_phase45_main"
    const val ACTIVITY_ID = "act_phase45_restaurant"
    const val CUSTOMER_ID = "cus_phase45_customer"
    const val USER_ID = "usr_phase45_operator"
    const val CASH_SESSION_ID = "cash_phase45_open"
    const val CATALOG_ITEM_ID = "item_phase45_cuy"

    fun withMigratedClientAndDatabase(
        prefix: String,
        block: (MongoClient, MongoDatabase) -> Unit,
    ) {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName(prefix))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
            block(client, database)
        }
    }

    fun <T> runConcurrently(
        workers: Int,
        timeoutSeconds: Long = 15,
        operation: (workerIndex: Int) -> T,
    ): List<T> {
        require(workers > 0) { "workers must be greater than zero." }

        val pool = Executors.newFixedThreadPool(workers)
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)

        try {
            val futures = (0 until workers).map { workerIndex ->
                pool.submit(
                    Callable {
                        ready.countDown()
                        check(start.await(timeoutSeconds, TimeUnit.SECONDS)) {
                            "Concurrent test did not start within ${timeoutSeconds}s."
                        }
                        operation(workerIndex)
                    }
                )
            }

            check(ready.await(timeoutSeconds, TimeUnit.SECONDS)) {
                "Concurrent workers were not ready within ${timeoutSeconds}s."
            }
            start.countDown()

            return futures.map { future -> future.get(timeoutSeconds, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    fun root(
        id: String,
        organizationId: String? = ORGANIZATION_ID,
        instant: Instant = Instant.parse("2026-05-15T12:00:00Z"),
    ): Document {
        val document = Document("_id", id)
            .append("createdAt", Date.from(instant))
            .append("createdBy", USER_ID)
            .append("updatedAt", Date.from(instant))
            .append("updatedBy", USER_ID)
            .append("version", 1)
            .append("schemaVersion", 1)

        if (organizationId != null) {
            document.append("organizationId", organizationId)
        }

        return document
    }

    fun money(amount: String): Document =
        Document("amount", Decimal128(BigDecimal(amount)))
            .append("currency", "USD")

    fun decimal(amount: String): Decimal128 = Decimal128(BigDecimal(amount))

    fun sale(
        id: String,
        saleNumber: String,
        operationalStatus: String = "pending",
        paymentStatus: String = "unpaid",
        collectionStatus: String = "pending_receivable",
        documentStatus: String = "not_required",
        createdAt: Instant = Instant.parse("2026-05-15T12:00:00Z"),
    ): Document = root(id, instant = createdAt)
        .append("branchId", BRANCH_ID)
        .append("activityId", ACTIVITY_ID)
        .append("saleNumber", saleNumber)
        .append("saleType", "standard_sale")
        .append("workflowMode", "quick_sale")
        .append("operationalStatus", operationalStatus)
        .append("paymentStatus", paymentStatus)
        .append("collectionStatus", collectionStatus)
        .append("documentStatus", documentStatus)
        .append("customerId", CUSTOMER_ID)
        .append("customerSnapshot", Document("displayName", "Cliente Prueba"))
        .append(
            "items",
            listOf(
                Document("lineId", "line_1")
                    .append("catalogItemId", CATALOG_ITEM_ID)
                    .append("name", "Cuy entero")
                    .append("quantity", decimal("1.000000"))
                    .append("unitCode", "unit")
                    .append("unitPrice", money("24.00"))
                    .append("lineTotal", money("24.00"))
                    .append("status", "pending")
            )
        )
        .append("totals", Document("grandTotal", money("24.00")).append("currency", "USD"))
        .append("taxSummary", Document("totalTax", money("3.60")).append("subtotalTaxable", money("24.00")))
        .append("paymentRefs", emptyList<Document>())
        .append("documentRefs", emptyList<Document>())
        .append("reservationRef", null)
        .append("cashSessionId", CASH_SESSION_ID)
        .append("confirmedAt", null)
        .append("closedAt", null)
        .append("canceledAt", null)

    fun stockBalance(
        id: String,
        quantityOnHand: String = "1.000000",
        quantityAvailable: String = "1.000000",
    ): Document = root(id)
        .append("branchId", BRANCH_ID)
        .append("catalogItemId", CATALOG_ITEM_ID)
        .append("stockUnit", "unit")
        .append("quantityOnHand", decimal(quantityOnHand))
        .append("quantityReserved", decimal("0.000000"))
        .append("quantityAvailable", decimal(quantityAvailable))
        .append("lowStockThreshold", decimal("2.000000"))
        .append("status", "available")
        .append("lastMovementAt", null)

    fun stockMovement(
        id: String,
        referenceId: String,
        occurredAt: Instant = Instant.parse("2026-05-15T12:10:00Z"),
    ): Document = root(id, instant = occurredAt)
        .append("branchId", BRANCH_ID)
        .append("catalogItemId", CATALOG_ITEM_ID)
        .append("type", "sale")
        .append("direction", "out")
        .append("quantity", decimal("1.000000"))
        .append("unitCode", "unit")
        .append("occurredAt", Date.from(occurredAt))
        .append("referenceType", "sale")
        .append("referenceId", referenceId)
        .append("reason", "concurrency test")

    fun cashSession(
        id: String = CASH_SESSION_ID,
        status: String = "open",
        openedAt: Instant = Instant.parse("2026-05-15T11:00:00Z"),
    ): Document = root(id, instant = openedAt)
        .append("branchId", BRANCH_ID)
        .append("openedBy", USER_ID)
        .append("openedAt", Date.from(openedAt))
        .append("status", status)
        .append("openingBalance", money("50.00"))
        .append("expectedCashAmount", money("74.00"))
        .append("countedCashAmount", money("74.00"))
        .append("differenceAmount", money("0.00"))
        .append("closingStartedAt", null)
        .append("closedAt", null)
        .append("canceledAt", null)
        .append("summary", Document("payments", 1))

    fun counter(
        id: String,
        current: Int = 0,
    ): Document = root(id)
        .append("scope", "branch")
        .append("scopeId", BRANCH_ID)
        .append("counterType", "sale")
        .append("current", current)
        .append("padding", 6)
        .append("prefix", "S-")
        .append("lastIssuedAt", null)
        .append("status", "active")

    fun payment(
        id: String,
        saleId: String,
        externalReference: String,
        paidAt: Instant = Instant.parse("2026-05-15T12:20:00Z"),
    ): Document = root(id, instant = paidAt)
        .append("branchId", BRANCH_ID)
        .append("saleId", saleId)
        .append("customerId", CUSTOMER_ID)
        .append("cashSessionId", CASH_SESSION_ID)
        .append("method", "cash")
        .append("status", "confirmed")
        .append("amount", money("24.00"))
        .append("paidAt", Date.from(paidAt))
        .append("externalReference", externalReference)
        .append("allocations", listOf(Document("saleId", saleId).append("amount", money("24.00"))))

    fun outboxEvent(
        id: String,
        eventId: String,
        aggregateId: String,
        availableAt: Instant = Instant.parse("2026-05-15T12:30:00Z"),
    ): Document = root(id, instant = availableAt)
        .append("eventId", eventId)
        .append("eventType", "SaleConfirmed")
        .append("aggregateType", "sale")
        .append("aggregateId", aggregateId)
        .append("payload", Document("aggregateId", aggregateId))
        .append("status", "pending")
        .append("availableAt", Date.from(availableAt))
        .append("publishedAt", null)
        .append("attempts", 0)
        .append("lastError", null)
}
