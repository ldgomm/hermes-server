package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            paidAt = now
        )

        assertEquals(PaymentStatus.PAID, payment.status)
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
            paidAt = now
        ).void()

        assertEquals(PaymentStatus.VOIDED, payment.status)
        assertFalse(payment.isEffective)
    }

    @Test
    fun `refunded payment cannot be voided`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now
        ).refund()

        assertFailsWith<DomainRuleViolation> {
            payment.void()
        }
    }
}
