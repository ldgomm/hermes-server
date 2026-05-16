package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CollectionStatusResolverTest {

    private val now = Instant.parse("2026-05-15T19:30:00Z")

    @Test
    fun `resolves not required when total due is zero`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.zero(),
            paidAmount = Money.zero(),
            now = now
        )

        assertEquals(CollectionStatus.NOT_REQUIRED, status)
    }

    @Test
    fun `resolves pending when nothing has been collected and due date has not passed`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero(),
            dueAt = Instant.parse("2026-05-16T19:30:00Z"),
            now = now
        )

        assertEquals(CollectionStatus.PENDING, status)
    }

    @Test
    fun `resolves overdue when nothing has been collected and due date passed`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero(),
            dueAt = Instant.parse("2026-05-14T19:30:00Z"),
            now = now
        )

        assertEquals(CollectionStatus.OVERDUE, status)
    }

    @Test
    fun `resolves partially collected when paid amount is greater than zero but lower than total`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("10.00"),
            dueAt = Instant.parse("2026-05-14T19:30:00Z"),
            now = now
        )

        assertEquals(CollectionStatus.PARTIALLY_COLLECTED, status)
    }

    @Test
    fun `resolves collected when paid amount equals total due`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("24.00"),
            now = now
        )

        assertEquals(CollectionStatus.COLLECTED, status)
    }

    @Test
    fun `resolves collected when paid amount is greater than total due`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("25.00"),
            now = now
        )

        assertEquals(CollectionStatus.COLLECTED, status)
    }

    @Test
    fun `resolves voided when collection is voided`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero(),
            now = now,
            isVoided = true
        )

        assertEquals(CollectionStatus.VOIDED, status)
    }

    @Test
    fun `resolves written off when collection is written off`() {
        val status = CollectionStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero(),
            now = now,
            isWrittenOff = true
        )

        assertEquals(CollectionStatus.WRITTEN_OFF, status)
    }

    @Test
    fun `rejects collection being voided and written off at the same time`() {
        assertFailsWith<DomainRuleViolation> {
            CollectionStatusResolver.resolve(
                totalDue = Money.of("24.00"),
                paidAmount = Money.zero(),
                now = now,
                isVoided = true,
                isWrittenOff = true
            )
        }
    }

    @Test
    fun `rejects resolving collection with different currencies`() {
        assertFailsWith<DomainRuleViolation> {
            CollectionStatusResolver.resolve(
                totalDue = Money.of("24.00", "USD"),
                paidAmount = Money.of("10.00", "EUR"),
                now = now
            )
        }
    }
}
