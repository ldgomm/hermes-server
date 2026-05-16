package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentStatusResolverTest {

    @Test
    fun `resolves unpaid when nothing has been paid`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero()
        )

        assertEquals(PaymentStatus.UNPAID, status)
    }

    @Test
    fun `resolves partially paid when paid amount is lower than total due`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("10.00")
        )

        assertEquals(PaymentStatus.PARTIALLY_PAID, status)
    }

    @Test
    fun `resolves paid when paid amount equals total due`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("24.00")
        )

        assertEquals(PaymentStatus.PAID, status)
    }

    @Test
    fun `resolves overpaid when paid amount is greater than total due`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("25.00")
        )

        assertEquals(PaymentStatus.OVERPAID, status)
    }

    @Test
    fun `resolves paid when total due is zero`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.zero(),
            paidAmount = Money.zero()
        )

        assertEquals(PaymentStatus.PAID, status)
    }

    @Test
    fun `resolves voided when payment is voided`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.zero(),
            isVoided = true
        )

        assertEquals(PaymentStatus.VOIDED, status)
    }

    @Test
    fun `resolves refunded when payment is refunded`() {
        val status = PaymentStatusResolver.resolve(
            totalDue = Money.of("24.00"),
            paidAmount = Money.of("24.00"),
            isRefunded = true
        )

        assertEquals(PaymentStatus.REFUNDED, status)
    }

    @Test
    fun `rejects payment being voided and refunded at the same time`() {
        assertFailsWith<DomainRuleViolation> {
            PaymentStatusResolver.resolve(
                totalDue = Money.of("24.00"),
                paidAmount = Money.of("24.00"),
                isVoided = true,
                isRefunded = true
            )
        }
    }

    @Test
    fun `rejects resolving payment with different currencies`() {
        assertFailsWith<DomainRuleViolation> {
            PaymentStatusResolver.resolve(
                totalDue = Money.of("24.00", "USD"),
                paidAmount = Money.of("24.00", "EUR")
            )
        }
    }
}
