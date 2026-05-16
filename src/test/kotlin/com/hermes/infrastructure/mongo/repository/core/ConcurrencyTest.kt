package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.repository.MongoOptimisticLockException
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.*
import org.bson.Document
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class ConcurrencyTest {

    @Test
    fun `last stock unit can be consumed only once`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_stock_race") { _, database ->
            val stockBalances = database.getCollection(MongoCollectionNames.STOCK_BALANCES)
            val stockMovements = database.getCollection(MongoCollectionNames.STOCK_MOVEMENTS)

            val stockBalanceId = "stock_phase45_last_unit"
            val saleId = "sale_phase45_last_unit"
            val now = Instant.parse("2026-05-15T12:10:00Z")

            stockBalances.insertOne(
                ConcurrencyTestSupport.stockBalance(
                    id = stockBalanceId,
                    quantityOnHand = "1.000000",
                    quantityAvailable = "1.000000",
                )
            )

            val results = ConcurrencyTestSupport.runConcurrently(workers = 2) { worker ->
                val claimed = stockBalances.findOneAndUpdate(
                    and(
                        eq("_id", stockBalanceId),
                        eq("quantityAvailable", ConcurrencyTestSupport.decimal("1.000000")),
                    ),
                    combine(
                        set("quantityOnHand", ConcurrencyTestSupport.decimal("0.000000")),
                        set("quantityAvailable", ConcurrencyTestSupport.decimal("0.000000")),
                        set("status", "out_of_stock"),
                        set("lastMovementAt", Date.from(now.plusMillis(worker.toLong()))),
                        inc("version", 1),
                    ),
                    FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
                )

                if (claimed != null) {
                    stockMovements.insertOne(
                        ConcurrencyTestSupport.stockMovement(
                            id = "stmov_phase45_last_unit_$worker",
                            referenceId = saleId,
                            occurredAt = now.plusMillis(worker.toLong()),
                        )
                    )
                    true
                } else {
                    false
                }
            }

            assertEquals(1, results.count { it }, "Only one worker may consume the last available unit.")
            assertEquals(1L, stockMovements.countDocuments(eq("referenceId", saleId)))

            val finalStock = stockBalances.find(eq("_id", stockBalanceId)).first()!!
            assertEquals("out_of_stock", finalStock.getString("status"))
            assertEquals(
                BigDecimal("0.000000"),
                finalStock.get("quantityAvailable", Decimal128::class.java).bigDecimalValue(),
            )
        }
    }

    @Test
    fun `cash session can be closed only once under concurrent close attempts`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_cash_close_race") { _, database ->
            val cashSessions = database.getCollection(MongoCollectionNames.CASH_SESSIONS)

            cashSessions.insertOne(ConcurrencyTestSupport.cashSession())

            val results = ConcurrencyTestSupport.runConcurrently(workers = 2) { worker ->
                val closed = cashSessions.findOneAndUpdate(
                    and(
                        eq("_id", ConcurrencyTestSupport.CASH_SESSION_ID),
                        eq("status", "open"),
                        eq("version", 1),
                    ),
                    combine(
                        set("status", "closed"),
                        set("closedAt", Date.from(Instant.parse("2026-05-15T12:20:00Z").plusMillis(worker.toLong()))),
                        inc("version", 1),
                    ),
                    FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
                )

                closed != null
            }

            assertEquals(1, results.count { it }, "Only one worker may close the cash session.")

            val finalSession = cashSessions.find(eq("_id", ConcurrencyTestSupport.CASH_SESSION_ID)).first()!!
            assertEquals("closed", finalSession.getString("status"))
            assertEquals(2, (finalSession["version"] as Number).toInt())
            assertNotNull(finalSession.getDate("closedAt"))
        }
    }

    @Test
    fun `counter increments issue unique contiguous numbers under concurrency`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_counter_race") { _, database ->
            val counters = database.getCollection(MongoCollectionNames.COUNTERS)
            val repository = CounterRepository(database)
            val workerCount = 20

            counters.insertOne(
                ConcurrencyTestSupport.counter(
                    id = "counter_phase45_branch_sale",
                    current = 0,
                )
            )

            val issuedNumbers = ConcurrencyTestSupport.runConcurrently(workers = workerCount) {
                repository.incrementAndGet(
                    organizationId = ConcurrencyTestSupport.ORGANIZATION_ID,
                    scope = "branch",
                    scopeId = ConcurrencyTestSupport.BRANCH_ID,
                    counterType = "sale",
                )
            }

            assertEquals((1L..workerCount.toLong()).toList(), issuedNumbers.sorted())

            val finalCounter = counters.find(eq("_id", "counter_phase45_branch_sale")).first()!!
            assertEquals(workerCount, (finalCounter["current"] as Number).toInt())
            assertEquals(workerCount + 1, (finalCounter["version"] as Number).toInt())
            assertNotNull(finalCounter.getDate("lastIssuedAt"))
        }
    }

    @Test
    fun `idempotent payment capture persists only one payment for the same gateway reference`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_payment_idempotency") { _, database ->
            val sales = database.getCollection(MongoCollectionNames.SALES)
            val payments = database.getCollection(MongoCollectionNames.PAYMENTS)

            val saleId = "sale_phase45_payment_idempotency"
            val paymentId = "pay_phase45_gateway_ref_001"
            val externalReference = "gateway:test:phase45:001"

            sales.insertOne(
                ConcurrencyTestSupport.sale(
                    id = saleId,
                    saleNumber = "PH45-PAY-001",
                )
            )

            val results = ConcurrencyTestSupport.runConcurrently(workers = 2) { worker ->
                try {
                    payments.insertOne(
                        ConcurrencyTestSupport.payment(
                            id = paymentId,
                            saleId = saleId,
                            externalReference = externalReference,
                            paidAt = Instant.parse("2026-05-15T12:25:00Z").plusMillis(worker.toLong()),
                        )
                    )
                    true
                } catch (_: MongoWriteException) {
                    false
                }
            }

            assertEquals(1, results.count { it }, "Only one idempotent payment insert should win.")
            assertEquals(1L, payments.countDocuments(eq("_id", paymentId)))
            assertEquals(1L, payments.countDocuments(eq("externalReference", externalReference)))
            assertEquals(saleId, payments.find(eq("_id", paymentId)).first()!!.getString("saleId"))
        }
    }

    @Test
    fun `outbox event can be claimed by only one worker`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_outbox_claim_race") { _, database ->
            val outboxEvents = database.getCollection(MongoCollectionNames.OUTBOX_EVENTS)

            val outboxId = "out_phase45_claim_once"
            val eventId = "evt_phase45_claim_once"
            val saleId = "sale_phase45_claim_once"
            val now = Date.from(Instant.parse("2026-05-15T12:40:00Z"))

            outboxEvents.insertOne(
                ConcurrencyTestSupport.outboxEvent(
                    id = outboxId,
                    eventId = eventId,
                    aggregateId = saleId,
                    availableAt = Instant.parse("2026-05-15T12:30:00Z"),
                )
            )

            val claimedBy = ConcurrencyTestSupport.runConcurrently(workers = 2) { worker ->
                val workerId = "worker_$worker"
                val claimed = outboxEvents.findOneAndUpdate(
                    and(
                        eq("_id", outboxId),
                        eq("status", "pending"),
                        lte("availableAt", now),
                    ),
                    combine(
                        set("status", "processing"),
                        set("lockedBy", workerId),
                        set("lockedAt", now),
                        inc("attempts", 1),
                        inc("version", 1),
                    ),
                    FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
                )

                claimed?.getString("lockedBy")
            }

            val winners = claimedBy.filterNotNull()
            assertEquals(1, winners.size, "Only one outbox worker may claim a pending event.")

            val finalOutbox = outboxEvents.find(eq("_id", outboxId)).first()!!
            assertEquals("processing", finalOutbox.getString("status"))
            assertEquals(winners.single(), finalOutbox.getString("lockedBy"))
            assertEquals(1, (finalOutbox["attempts"] as Number).toInt())
            assertEquals(2, (finalOutbox["version"] as Number).toInt())
        }
    }

    @Test
    fun `optimistic locking rejects stale repository writers`() {
        ConcurrencyTestSupport.withMigratedClientAndDatabase("phase_4_5_optimistic_lock_race") { _, database ->
            val repository = SaleRepository(database)
            val saleId = "sale_phase45_optimistic_lock"

            val baseSale = ConcurrencyTestSupport.sale(
                id = saleId,
                saleNumber = "PH45-LOCK-001",
                operationalStatus = "pending",
            )

            repository.insert(baseSale)

            val staleConfirmed = Document(baseSale)
                .append("operationalStatus", "confirmed")
                .append("confirmedAt", Date.from(Instant.parse("2026-05-15T12:45:00Z")))
                .append("version", 2)

            val staleCanceled = Document(baseSale)
                .append("operationalStatus", "canceled")
                .append("canceledAt", Date.from(Instant.parse("2026-05-15T12:45:01Z")))
                .append("version", 2)

            val results = ConcurrencyTestSupport.runConcurrently(workers = 2) { worker ->
                val staleDocument = if (worker == 0) staleConfirmed else staleCanceled
                try {
                    repository.replace(staleDocument, expectedVersion = 1L)
                    true
                } catch (_: MongoOptimisticLockException) {
                    false
                }
            }

            assertEquals(1, results.count { it }, "Only one stale writer may replace version 1.")

            val finalSale = repository.requireById(saleId)
            assertEquals(2, (finalSale["version"] as Number).toInt())
            assertTrue(finalSale.getString("operationalStatus") in setOf("confirmed", "canceled"))
        }
    }
}
