package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant

class PaymentReceivableRepositoryIntegrationTest {
    @Test
    fun `payment repository queries payments by sale cash session and external reference`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_payment_repository") { database ->
            val payments = PaymentRepository(database)

            payments.insert(
                RepositoryTestSupport.paymentDocument(
                    id = "pay_phase43_001",
                    externalReference = "TRANSFER-001",
                    paidAt = Instant.parse("2026-05-15T12:10:00Z"),
                )
            )
            payments.insert(
                RepositoryTestSupport.paymentDocument(
                    id = "pay_phase43_002",
                    externalReference = "TRANSFER-002",
                    paidAt = Instant.parse("2026-05-15T12:20:00Z"),
                )
            )
            payments.insert(
                RepositoryTestSupport.paymentDocument(
                    id = "pay_phase43_other_org",
                    organizationId = RepositoryTestSupport.OTHER_ORGANIZATION_ID,
                    externalReference = "TRANSFER-001",
                    paidAt = Instant.parse("2026-05-15T12:30:00Z"),
                )
            )

            assertEquals(
                listOf("pay_phase43_002", "pay_phase43_001"),
                payments.findBySale(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.SALE_ID,
                ).map { it.getString("_id") }
            )

            assertEquals(
                listOf("pay_phase43_002", "pay_phase43_001"),
                payments.findByCashSession(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.CASH_SESSION_ID,
                ).map { it.getString("_id") }
            )

            val byReference = payments.findByExternalReference(
                RepositoryTestSupport.ORGANIZATION_ID,
                " TRANSFER-001 ",
            )

            assertNotNull(byReference)
            assertEquals("pay_phase43_001", byReference?.getString("_id"))
        }
    }

    @Test
    fun `receivable repository queries open receivables by organization customer and sale`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_receivable_repository") { database ->
            val receivables = ReceivableRepository(database)

            receivables.insert(RepositoryTestSupport.receivableDocument(id = "recv_phase43_open", status = "open"))
            receivables.insert(
                RepositoryTestSupport.receivableDocument(
                    id = "recv_phase43_partial",
                    saleId = "sale_phase43_002",
                    status = "partially_collected",
                )
            )
            receivables.insert(
                RepositoryTestSupport.receivableDocument(
                    id = "recv_phase43_overdue",
                    saleId = "sale_phase43_003",
                    status = "overdue",
                    dueAt = Instant.parse("2026-05-01T12:00:00Z"),
                )
            )
            receivables.insert(
                RepositoryTestSupport.receivableDocument(
                    id = "recv_phase43_settled",
                    saleId = "sale_phase43_004",
                    status = "settled",
                )
            )

            assertEquals(
                setOf("recv_phase43_open", "recv_phase43_partial", "recv_phase43_overdue"),
                receivables.findOpenByOrganization(RepositoryTestSupport.ORGANIZATION_ID)
                    .map { it.getString("_id") }
                    .toSet()
            )

            assertEquals(
                setOf("recv_phase43_open", "recv_phase43_partial", "recv_phase43_overdue", "recv_phase43_settled"),
                receivables.findByCustomer(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    RepositoryTestSupport.CUSTOMER_ID,
                ).map { it.getString("_id") }.toSet()
            )

            val bySale = receivables.findBySale(
                RepositoryTestSupport.ORGANIZATION_ID,
                RepositoryTestSupport.SALE_ID,
            )

            assertNotNull(bySale)
            assertEquals("recv_phase43_open", bySale?.getString("_id"))
        }
    }
}
