package com.hermes.domain.payment

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PaymentEntityTest {
    private val now = Instant.parse("2026-05-18T20:00:00Z")

    @Test
    fun `records cash payment without reference`() {
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
        assertEquals(null, payment.reference)
    }

    @Test
    fun `rejects bank transfer without reference`() {
        assertFailsWith<DomainRuleViolation> {
            Payment.record(
                id = "pay_1",
                organizationId = "org_1",
                saleId = "sale_1",
                amount = Money.of("24.00"),
                method = PaymentMethod.BANK_TRANSFER,
                paidAt = now,
            )
        }
    }

    @Test
    fun `allocates confirmed payment`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        assertEquals(PaymentLifecycleStatus.ALLOCATED, payment.allocate().status)
    }

    @Test
    fun `reverses confirmed payment`() {
        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        assertEquals(PaymentLifecycleStatus.REVERSED, payment.reverse().status)
    }
}
