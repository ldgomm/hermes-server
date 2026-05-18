package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceivableEntityTest {
    private val now = Instant.parse("2026-05-18T20:00:00Z")

    @Test
    fun `creates receivable for sale`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("14.00"),
            dueAt = now.plusSeconds(86_400),
            createdAt = now,
        )

        assertEquals(Money.of("14.00"), receivable.balanceDue)
        assertEquals(CollectionStatus.PENDING, receivable.status(now))
    }

    @Test
    fun `registers partial collection`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("14.00"),
            dueAt = now.plusSeconds(86_400),
            createdAt = now,
        )

        val updated = receivable.registerCollection(
            amount = Money.of("4.00"),
            collectedAt = now.plusSeconds(60),
        )

        assertEquals(Money.of("4.00"), updated.paidAmount)
        assertEquals(Money.of("10.00"), updated.balanceDue)
        assertEquals(CollectionStatus.PARTIALLY_COLLECTED, updated.status(now.plusSeconds(60)))
    }

    @Test
    fun `rejects over collection`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("14.00"),
            dueAt = now.plusSeconds(86_400),
            createdAt = now,
        )

        assertFailsWith<DomainRuleViolation> {
            receivable.registerCollection(
                amount = Money.of("15.00"),
                collectedAt = now.plusSeconds(60),
            )
        }
    }
}
