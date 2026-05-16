package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.*

class PaymentTest {

    private val now = Instant.parse("2026-05-15T20:00:00Z")

    @Test
    fun `records effective payment`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        assertEquals(PaymentLifecycleStatus.CONFIRMED, payment.status)
        assertTrue(payment.isEffective)
    }

    @Test
    fun `voided payment is not effective`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        ).void()

        assertEquals(PaymentLifecycleStatus.VOIDED, payment.status)
        assertFalse(payment.isEffective)
    }

    @Test
    fun `reversed payment is not effective`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        ).reverse()

        assertEquals(PaymentLifecycleStatus.REVERSED, payment.status)
        assertFalse(payment.isEffective)
    }

    @Test
    fun `reversed payment cannot be voided`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        ).reverse()

        assertFailsWith<DomainRuleViolation> {
            payment.void()
        }
    }

    @Test
    fun `allocated payment cannot be voided`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        ).allocate()

        assertEquals(PaymentLifecycleStatus.ALLOCATED, payment.status)
        assertTrue(payment.isEffective)

        assertFailsWith<DomainRuleViolation> {
            payment.void()
        }
    }

    @Test
    fun `voided payment cannot be voided again`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        ).void()

        assertFailsWith<DomainRuleViolation> {
            payment.void()
        }
    }
}