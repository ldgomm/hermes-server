package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.*

internal object TransactionRollbackTestSupport {
    const val ORGANIZATION_ID = "org_phase44"
    const val BRANCH_ID = "br_phase44_main"
    const val ACTIVITY_ID = "act_phase44_restaurant"
    const val CUSTOMER_ID = "cus_phase44_customer"
    const val USER_ID = "usr_phase44_operator"
    const val CASH_SESSION_ID = "cash_phase44_open"
    const val CATALOG_ITEM_ID = "item_phase44_cuy"

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

    fun <T> MongoDatabase.collection(name: String): MongoCollection<Document> = getCollection(name)

    fun runRollbackTransaction(
        client: MongoClient,
        operation: (ClientSession) -> Unit,
    ): Throwable {
        var failure: Throwable? = null

        client.startSession().use { session ->
            try {
                session.startTransaction()
                operation(session)
                session.commitTransaction()
            } catch (error: Throwable) {
                failure = error
                if (session.hasActiveTransaction()) {
                    session.abortTransaction()
                }
            }
        }

        return requireNotNull(failure) { "Expected the transaction operation to fail." }
    }

    fun runCommitTransaction(
        client: MongoClient,
        operation: (ClientSession) -> Unit,
    ) {
        client.startSession().use { session ->
            try {
                session.startTransaction()
                operation(session)
                session.commitTransaction()
            } catch (error: Throwable) {
                if (session.hasActiveTransaction()) {
                    session.abortTransaction()
                }
                throw error
            }
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

    fun payment(
        id: String,
        saleId: String,
        cashSessionId: String? = CASH_SESSION_ID,
        externalReference: String? = null,
        paidAt: Instant = Instant.parse("2026-05-15T12:10:00Z"),
    ): Document = root(id, instant = paidAt)
        .append("branchId", BRANCH_ID)
        .append("saleId", saleId)
        .append("customerId", CUSTOMER_ID)
        .append("cashSessionId", cashSessionId)
        .append("method", "cash")
        .append("status", "confirmed")
        .append("amount", money("24.00"))
        .append("paidAt", Date.from(paidAt))
        .append("externalReference", externalReference)
        .append("allocations", listOf(Document("saleId", saleId).append("amount", money("24.00"))))

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
        .append("expectedCashAmount", money("50.00"))
        .append("countedCashAmount", money("50.00"))
        .append("differenceAmount", money("0.00"))
        .append("closingStartedAt", null)
        .append("closedAt", null)
        .append("canceledAt", null)
        .append("summary", Document("payments", 0))

    fun cashMovement(
        id: String,
        referenceId: String,
        occurredAt: Instant = Instant.parse("2026-05-15T12:11:00Z"),
    ): Document = root(id, instant = occurredAt)
        .append("cashSessionId", CASH_SESSION_ID)
        .append("branchId", BRANCH_ID)
        .append("type", "sale_payment")
        .append("direction", "in")
        .append("amount", money("24.00"))
        .append("occurredAt", Date.from(occurredAt))
        .append("referenceType", "payment")
        .append("referenceId", referenceId)
        .append("notes", null)

    fun stockBalance(
        id: String,
        quantityOnHand: String = "10.000000",
        quantityAvailable: String = "10.000000",
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
        .append("locationCode", null)

    fun stockMovement(
        id: String,
        referenceId: String,
        occurredAt: Instant = Instant.parse("2026-05-15T12:12:00Z"),
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
        .append("reason", "transaction rollback test")

    fun auditLog(
        id: String,
        action: String,
        entityType: String,
        entityId: String,
        occurredAt: Instant = Instant.parse("2026-05-15T12:13:00Z"),
    ): Document = root(id, instant = occurredAt)
        .append("actorUserId", USER_ID)
        .append("actorType", "user")
        .append("action", action)
        .append("entityType", entityType)
        .append("entityId", entityId)
        .append("occurredAt", Date.from(occurredAt))
        .append("ipAddress", "127.0.0.1")
        .append("requestId", "req_phase44")
        .append("reason", null)
        .append("before", Document("status", "pending"))
        .append("after", Document("status", "confirmed"))
        .append("metadata", Document("source", "transaction_rollback_test"))

    fun domainEvent(
        id: String,
        eventType: String,
        aggregateType: String,
        aggregateId: String,
        sequence: Int = 1,
        occurredAt: Instant = Instant.parse("2026-05-15T12:14:00Z"),
    ): Document = root(id, instant = occurredAt)
        .append("aggregateType", aggregateType)
        .append("aggregateId", aggregateId)
        .append("eventType", eventType)
        .append("eventVersion", 1)
        .append("sequence", sequence)
        .append("occurredAt", Date.from(occurredAt))
        .append("payload", Document("aggregateId", aggregateId))
        .append("metadata", Document("source", "transaction_rollback_test"))

    fun outboxEvent(
        id: String,
        eventId: String,
        aggregateType: String,
        aggregateId: String,
        status: String = "pending",
        availableAt: Instant = Instant.parse("2026-05-15T12:15:00Z"),
    ): Document = root(id, instant = availableAt)
        .append("eventId", eventId)
        .append("eventType", "SaleConfirmed")
        .append("aggregateType", aggregateType)
        .append("aggregateId", aggregateId)
        .append("payload", Document("aggregateId", aggregateId))
        .append("status", status)
        .append("availableAt", Date.from(availableAt))
        .append("publishedAt", null)
        .append("attempts", 0)
        .append("lastError", null)
}
