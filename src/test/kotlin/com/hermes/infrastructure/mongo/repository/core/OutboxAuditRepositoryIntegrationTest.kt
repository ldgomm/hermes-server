package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class OutboxAuditRepositoryIntegrationTest {
    @Test
    fun `outbox repository finds only pending events whose availability time has arrived`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_outbox_repository") { database ->
            val outbox = OutboxEventRepository(database)
            val now = Instant.parse("2026-05-15T13:00:00Z")

            outbox.insert(
                RepositoryTestSupport.outboxEvent(
                    id = "out_phase43_ready_late",
                    eventId = "evt_ready_late",
                    status = "pending",
                    availableAt = Instant.parse("2026-05-15T12:30:00Z"),
                )
            )
            outbox.insert(
                RepositoryTestSupport.outboxEvent(
                    id = "out_phase43_ready_early",
                    eventId = "evt_ready_early",
                    status = "pending",
                    availableAt = Instant.parse("2026-05-15T12:00:00Z"),
                )
            )
            outbox.insert(
                RepositoryTestSupport.outboxEvent(
                    id = "out_phase43_future",
                    eventId = "evt_future",
                    status = "pending",
                    availableAt = Instant.parse("2026-05-15T14:00:00Z"),
                )
            )
            outbox.insert(
                RepositoryTestSupport.outboxEvent(
                    id = "out_phase43_failed",
                    eventId = "evt_failed",
                    status = "failed",
                    availableAt = Instant.parse("2026-05-15T11:00:00Z"),
                )
            )

            assertEquals(
                listOf("out_phase43_ready_early", "out_phase43_ready_late"),
                outbox.findReady(Date.from(now)).map { it.getString("_id") }
            )
            assertEquals("out_phase43_ready_early", outbox.findByEventId("evt_ready_early")?.getString("_id"))
        }
    }

    @Test
    fun `audit and domain event repositories preserve append only operational history queries`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_audit_repository") { database ->
            val audits = AuditLogRepository(database)
            val domainEvents = DomainEventRepository(database)

            audits.insert(
                RepositoryTestSupport.auditLog(
                    id = "audit_phase43_sale_created",
                    action = "sale.created",
                    entityType = "sale",
                    entityId = RepositoryTestSupport.SALE_ID,
                    occurredAt = Instant.parse("2026-05-15T12:10:00Z"),
                )
            )
            audits.insert(
                RepositoryTestSupport.auditLog(
                    id = "audit_phase43_sale_paid",
                    action = "payment.collected",
                    entityType = "sale",
                    entityId = RepositoryTestSupport.SALE_ID,
                    occurredAt = Instant.parse("2026-05-15T12:20:00Z"),
                )
            )
            audits.insert(
                RepositoryTestSupport.auditLog(
                    id = "audit_phase43_other_entity",
                    action = "cash.opened",
                    entityType = "cash_session",
                    entityId = RepositoryTestSupport.CASH_SESSION_ID,
                    occurredAt = Instant.parse("2026-05-15T12:30:00Z"),
                )
            )

            domainEvents.insert(
                RepositoryTestSupport.domainEvent(
                    id = "evt_phase43_sale_created",
                    sequence = 1,
                    eventType = "SaleCreated",
                    occurredAt = Instant.parse("2026-05-15T12:10:00Z"),
                )
            )
            domainEvents.insert(
                RepositoryTestSupport.domainEvent(
                    id = "evt_phase43_payment_collected",
                    sequence = 2,
                    eventType = "PaymentCollected",
                    occurredAt = Instant.parse("2026-05-15T12:20:00Z"),
                )
            )

            assertEquals(
                listOf("audit_phase43_sale_paid", "audit_phase43_sale_created"),
                audits.findByEntity(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "sale",
                    RepositoryTestSupport.SALE_ID,
                ).map { it.getString("_id") }
            )

            assertEquals(
                setOf("audit_phase43_sale_created", "audit_phase43_sale_paid", "audit_phase43_other_entity"),
                audits.findByActor(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.USER_ID,
                ).map { it.getString("_id") }.toSet()
            )

            assertEquals(
                listOf("evt_phase43_sale_created", "evt_phase43_payment_collected"),
                domainEvents.findByAggregate(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "sale",
                    RepositoryTestSupport.SALE_ID,
                ).map { it.getString("_id") }
            )

            assertEquals(
                listOf("evt_phase43_payment_collected"),
                domainEvents.findByEventType("PaymentCollected").map { it.getString("_id") }
            )
        }
    }
}
