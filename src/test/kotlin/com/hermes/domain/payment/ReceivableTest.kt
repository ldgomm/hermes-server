package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceivableTest {

    private val now = Instant.parse("2026-05-15T20:00:00Z")

    @Test
    fun `creates pending receivable`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("24.00"),
            dueAt = Instant.parse("2026-05-16T20:00:00Z"),
            createdAt = now
        )

        assertEquals(CollectionStatus.PENDING, receivable.status(now))
    }

    @Test
    fun `registers partial collection`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("24.00"),
            dueAt = null,
            createdAt = now
        ).registerCollection(
            amount = Money.of("10.00"),
            collectedAt = now
        )

        assertEquals("10.00", receivable.paidAmount.amount.toPlainString())
        assertEquals(CollectionStatus.PARTIALLY_COLLECTED, receivable.status(now))
    }

    @Test
    fun `rejects collection greater than balance`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("24.00"),
            dueAt = null,
            createdAt = now
        )

        assertFailsWith<DomainRuleViolation> {
            receivable.registerCollection(
                amount = Money.of("25.00"),
                collectedAt = now
            )
        }
    }

    @Test
    fun `writes off uncollected receivable`() {
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = "org_1",
            saleId = "sale_1",
            customerId = "cus_1",
            totalDue = Money.of("24.00"),
            dueAt = null,
            createdAt = now
        ).writeOff(now)

        assertEquals(CollectionStatus.WRITTEN_OFF, receivable.status(now))
    }
}
