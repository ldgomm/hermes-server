package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.repository.core.TransactionRollbackTestSupport.collection
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.*
import org.bson.Document
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class TransactionRollbackTest {
    @Test
    fun `if payment fails after sale confirmation then sale confirmation is rolled back`() {
        TransactionRollbackTestSupport.withMigratedClientAndDatabase("phase_4_4_payment_rollback") { client, database ->
            val sales = database.collection<Document>(MongoCollectionNames.SALES)
            val payments = database.collection<Document>(MongoCollectionNames.PAYMENTS)

            val saleId = "sale_phase44_payment_rollback"
            val duplicatePaymentId = "pay_phase44_duplicate_payment"

            sales.insertOne(
                TransactionRollbackTestSupport.sale(
                    id = saleId,
                    saleNumber = "PH44-PAY-ROLLBACK",
                    operationalStatus = "pending",
                )
            )
            payments.insertOne(
                TransactionRollbackTestSupport.payment(
                    id = duplicatePaymentId,
                    saleId = "sale_phase44_existing_payment_owner",
                )
            )

            val failure = TransactionRollbackTestSupport.runRollbackTransaction(client) { session ->
                sales.updateOne(
                    session,
                    eq("_id", saleId),
                    combine(
                        set("operationalStatus", "confirmed"),
                        set("confirmedAt", Date.from(Instant.parse("2026-05-15T12:30:00Z"))),
                        inc("version", 1),
                    ),
                )

                payments.insertOne(
                    session,
                    TransactionRollbackTestSupport.payment(
                        id = duplicatePaymentId,
                        saleId = saleId,
                    ),
                )
            }

            assertTrue(failure is MongoWriteException)
            assertEquals("pending", sales.find(eq("_id", saleId)).first()!!.getString("operationalStatus"))
            assertEquals(1L, payments.countDocuments(eq("_id", duplicatePaymentId)))
            assertEquals(0L, payments.countDocuments(eq("saleId", saleId)))
        }
    }

    @Test
    fun `if cash movement fails then confirmed payment is rolled back and no orphan payment remains`() {
        TransactionRollbackTestSupport.withMigratedClientAndDatabase("phase_4_4_cash_rollback") { client, database ->
            val payments = database.collection<Document>(MongoCollectionNames.PAYMENTS)
            val cashSessions = database.collection<Document>(MongoCollectionNames.CASH_SESSIONS)
            val cashMovements = database.collection<Document>(MongoCollectionNames.CASH_MOVEMENTS)

            val saleId = "sale_phase44_cash_rollback"
            val paymentId = "pay_phase44_cash_rollback"
            val duplicateMovementId = "cmov_phase44_duplicate_cash"

            cashSessions.insertOne(TransactionRollbackTestSupport.cashSession())
            cashMovements.insertOne(
                TransactionRollbackTestSupport.cashMovement(
                    id = duplicateMovementId,
                    referenceId = "pay_phase44_existing",
                )
            )

            val failure = TransactionRollbackTestSupport.runRollbackTransaction(client) { session ->
                payments.insertOne(
                    session,
                    TransactionRollbackTestSupport.payment(
                        id = paymentId,
                        saleId = saleId,
                    ),
                )
                cashSessions.updateOne(
                    session,
                    eq("_id", TransactionRollbackTestSupport.CASH_SESSION_ID),
                    combine(
                        set("expectedCashAmount", TransactionRollbackTestSupport.money("74.00")),
                        inc("version", 1),
                    ),
                )
                cashMovements.insertOne(
                    session,
                    TransactionRollbackTestSupport.cashMovement(
                        id = duplicateMovementId,
                        referenceId = paymentId,
                    ),
                )
            }

            assertTrue(failure is MongoWriteException)
            assertEquals(0L, payments.countDocuments(eq("_id", paymentId)))
            assertEquals(1L, cashMovements.countDocuments(eq("_id", duplicateMovementId)))
            val cashSession = cashSessions.find(eq("_id", TransactionRollbackTestSupport.CASH_SESSION_ID)).first()!!
            assertEquals(
                BigDecimal("50.00"),
                cashSession.get("expectedCashAmount", Document::class.java).get("amount", Decimal128::class.java)
                    .bigDecimalValue()
            )
        }
    }

    @Test
    fun `if stock movement fails then sale confirmation and stock balance update are rolled back`() {
        TransactionRollbackTestSupport.withMigratedClientAndDatabase("phase_4_4_stock_rollback") { client, database ->
            val sales = database.collection<Document>(MongoCollectionNames.SALES)
            val stockBalances = database.collection<Document>(MongoCollectionNames.STOCK_BALANCES)
            val stockMovements = database.collection<Document>(MongoCollectionNames.STOCK_MOVEMENTS)

            val saleId = "sale_phase44_stock_rollback"
            val stockBalanceId = "stock_phase44_rollback"
            val duplicateMovementId = "stmov_phase44_duplicate_stock"

            sales.insertOne(
                TransactionRollbackTestSupport.sale(
                    id = saleId,
                    saleNumber = "PH44-STOCK-ROLLBACK",
                    operationalStatus = "pending",
                )
            )
            stockBalances.insertOne(
                TransactionRollbackTestSupport.stockBalance(
                    id = stockBalanceId,
                    quantityOnHand = "10.000000",
                    quantityAvailable = "10.000000",
                )
            )
            stockMovements.insertOne(
                TransactionRollbackTestSupport.stockMovement(
                    id = duplicateMovementId,
                    referenceId = "sale_phase44_existing_stock_owner",
                )
            )

            val failure = TransactionRollbackTestSupport.runRollbackTransaction(client) { session ->
                sales.updateOne(
                    session,
                    eq("_id", saleId),
                    combine(
                        set("operationalStatus", "confirmed"),
                        set("confirmedAt", Date.from(Instant.parse("2026-05-15T12:32:00Z"))),
                        inc("version", 1),
                    ),
                )
                stockBalances.updateOne(
                    session,
                    eq("_id", stockBalanceId),
                    combine(
                        set("quantityOnHand", TransactionRollbackTestSupport.decimal("9.000000")),
                        set("quantityAvailable", TransactionRollbackTestSupport.decimal("9.000000")),
                        set("lastMovementAt", Date.from(Instant.parse("2026-05-15T12:32:05Z"))),
                        inc("version", 1),
                    ),
                )
                stockMovements.insertOne(
                    session,
                    TransactionRollbackTestSupport.stockMovement(
                        id = duplicateMovementId,
                        referenceId = saleId,
                    ),
                )
            }

            assertTrue(failure is MongoWriteException)
            assertEquals("pending", sales.find(eq("_id", saleId)).first()!!.getString("operationalStatus"))

            val stock = stockBalances.find(eq("_id", stockBalanceId)).first()!!
            assertEquals(BigDecimal("10.000000"), stock.get("quantityOnHand", Decimal128::class.java).bigDecimalValue())
            assertEquals(
                BigDecimal("10.000000"),
                stock.get("quantityAvailable", Decimal128::class.java).bigDecimalValue()
            )
            assertEquals(1L, stockMovements.countDocuments(eq("_id", duplicateMovementId)))
            assertEquals(0L, stockMovements.countDocuments(eq("referenceId", saleId)))
        }
    }

    @Test
    fun `critical audit log failure rolls back the business operation`() {
        TransactionRollbackTestSupport.withMigratedClientAndDatabase("phase_4_4_audit_rollback") { client, database ->
            val sales = database.collection<Document>(MongoCollectionNames.SALES)
            val auditLogs = database.collection<Document>(MongoCollectionNames.AUDIT_LOGS)

            val saleId = "sale_phase44_audit_rollback"
            val duplicateAuditId = "audit_phase44_duplicate"

            sales.insertOne(
                TransactionRollbackTestSupport.sale(
                    id = saleId,
                    saleNumber = "PH44-AUDIT-ROLLBACK",
                    operationalStatus = "pending",
                )
            )
            auditLogs.insertOne(
                TransactionRollbackTestSupport.auditLog(
                    id = duplicateAuditId,
                    action = "existing_action",
                    entityType = "sale",
                    entityId = "sale_phase44_existing_audit_owner",
                )
            )

            val failure = TransactionRollbackTestSupport.runRollbackTransaction(client) { session ->
                sales.updateOne(
                    session,
                    eq("_id", saleId),
                    combine(
                        set("operationalStatus", "confirmed"),
                        set("confirmedAt", Date.from(Instant.parse("2026-05-15T12:33:00Z"))),
                        inc("version", 1),
                    ),
                )
                auditLogs.insertOne(
                    session,
                    TransactionRollbackTestSupport.auditLog(
                        id = duplicateAuditId,
                        action = "sale.confirm",
                        entityType = "sale",
                        entityId = saleId,
                    ),
                )
            }

            assertTrue(failure is MongoWriteException)
            assertEquals("pending", sales.find(eq("_id", saleId)).first()!!.getString("operationalStatus"))
            assertEquals(1L, auditLogs.countDocuments(eq("_id", duplicateAuditId)))
            assertEquals(0L, auditLogs.countDocuments(eq("entityId", saleId)))
        }
    }

    @Test
    fun `non critical outbox failure does not roll back committed operation and is visible for repair`() {
        TransactionRollbackTestSupport.withMigratedClientAndDatabase("phase_4_4_outbox_policy") { client, database ->
            val sales = database.collection<Document>(MongoCollectionNames.SALES)
            val auditLogs = database.collection<Document>(MongoCollectionNames.AUDIT_LOGS)
            val domainEvents = database.collection<Document>(MongoCollectionNames.DOMAIN_EVENTS)
            val outboxEvents = database.collection<Document>(MongoCollectionNames.OUTBOX_EVENTS)

            val saleId = "sale_phase44_outbox_policy"
            val outboxId = "out_phase44_duplicate"

            sales.insertOne(
                TransactionRollbackTestSupport.sale(
                    id = saleId,
                    saleNumber = "PH44-OUTBOX-POLICY",
                    operationalStatus = "pending",
                )
            )
            outboxEvents.insertOne(
                TransactionRollbackTestSupport.outboxEvent(
                    id = outboxId,
                    eventId = "evt_phase44_existing_outbox",
                    aggregateType = "sale",
                    aggregateId = "sale_phase44_existing_outbox_owner",
                )
            )

            TransactionRollbackTestSupport.runCommitTransaction(client) { session ->
                sales.updateOne(
                    session,
                    eq("_id", saleId),
                    combine(
                        set("operationalStatus", "confirmed"),
                        set("confirmedAt", Date.from(Instant.parse("2026-05-15T12:34:00Z"))),
                        inc("version", 1),
                    ),
                )
                auditLogs.insertOne(
                    session,
                    TransactionRollbackTestSupport.auditLog(
                        id = "audit_phase44_outbox_policy",
                        action = "sale.confirm",
                        entityType = "sale",
                        entityId = saleId,
                    ),
                )
                domainEvents.insertOne(
                    session,
                    TransactionRollbackTestSupport.domainEvent(
                        id = "evt_phase44_outbox_policy",
                        eventType = "SaleConfirmed",
                        aggregateType = "sale",
                        aggregateId = saleId,
                    ),
                )
            }

            val nonCriticalOutboxFailure = assertThrows(MongoWriteException::class.java) {
                outboxEvents.insertOne(
                    TransactionRollbackTestSupport.outboxEvent(
                        id = outboxId,
                        eventId = "evt_phase44_outbox_policy",
                        aggregateType = "sale",
                        aggregateId = saleId,
                    )
                )
            }

            assertNotNull(nonCriticalOutboxFailure)
            assertEquals("confirmed", sales.find(eq("_id", saleId)).first()!!.getString("operationalStatus"))
            assertEquals(1L, auditLogs.countDocuments(eq("entityId", saleId)))
            assertEquals(1L, domainEvents.countDocuments(eq("aggregateId", saleId)))
            assertEquals(0L, outboxEvents.countDocuments(eq("aggregateId", saleId)))
            assertTrue(outboxEvents.countDocuments(eq("_id", outboxId)) == 1L)
            assertFalse(outboxEvents.find(eq("_id", outboxId)).first()!!.getString("aggregateId") == saleId)
        }
    }
}
