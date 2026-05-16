package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.payment.CollectionStatus
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.PaymentStatus
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaleEntityTest {

    private val now = Instant.parse("2026-05-15T20:00:00Z")

    private fun sampleItem(id: String = "item_1"): SaleItem {
        return SaleItem.create(
            id = id,
            catalogItemId = "cat_1",
            name = "Parrillada",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(2)
        )
    }

    @Test
    fun `creates draft sale and adds item`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now)

        assertEquals("24.00", sale.total.amount.toPlainString())
        assertEquals(SaleOperationalStatus.DRAFT, sale.operationalStatus)
    }

    @Test
    fun `confirms sale with active items`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now).confirm(now)

        assertEquals(SaleOperationalStatus.CONFIRMED, sale.operationalStatus)
    }

    @Test
    fun `rejects confirming sale without items`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        )

        assertFailsWith<DomainRuleViolation> {
            sale.confirm(now)
        }
    }

    @Test
    fun `registers payment and resolves paid status`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now).confirm(now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now
        )

        val paidSale = sale.registerPayment(payment, now)

        assertEquals("24.00", paidSale.paidAmount.amount.toPlainString())
        assertEquals(PaymentStatus.PAID, paidSale.paymentStatus)
        assertEquals(CollectionStatus.COLLECTED, paidSale.collectionStatus(now))
    }

    @Test
    fun `rejects payment while sale is draft`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now
        )

        assertFailsWith<DomainRuleViolation> {
            sale.registerPayment(payment, now)
        }
    }

    @Test
    fun `rejects closing unpaid sale`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now).confirm(now)
            .transitionTo(SaleOperationalStatus.IN_PROGRESS, now)
            .transitionTo(SaleOperationalStatus.READY, now)
            .transitionTo(SaleOperationalStatus.DELIVERED, now)

        assertFailsWith<DomainRuleViolation> {
            sale.transitionTo(SaleOperationalStatus.CLOSED, now)
        }
    }

    @Test
    fun `closes paid sale`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            activityId = "act_1",
            createdAt = now
        ).addItem(sampleItem(), now).confirm(now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now
        )

        val closed = sale.registerPayment(payment, now)
            .transitionTo(SaleOperationalStatus.IN_PROGRESS, now)
            .transitionTo(SaleOperationalStatus.READY, now)
            .transitionTo(SaleOperationalStatus.DELIVERED, now)
            .transitionTo(SaleOperationalStatus.CLOSED, now)

        assertEquals(SaleOperationalStatus.CLOSED, closed.operationalStatus)
        assertEquals(PaymentStatus.PAID, closed.paymentStatus)
    }
}
