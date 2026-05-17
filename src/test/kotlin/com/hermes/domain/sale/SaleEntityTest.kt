package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.ReceivableStatus
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaleEntityTest {

    private val now = Instant.parse("2026-05-15T20:00:00Z")

    private fun draftSale(
        id: String = "sale_1",
        dueAt: Instant? = null,
    ): Sale {
        return Sale.createDraft(
            id = id,
            organizationId = "org_1",
            branchId = "branch_1",
            activityId = "act_1",
            createdAt = now,
            dueAt = dueAt,
        )
    }

    private fun sampleCatalogSnapshot(
        catalogItemId: String = "cat_1",
        name: String = "Parrillada",
    ): CatalogItemSnapshot {
        return CatalogItemSnapshot(
            catalogItemId = catalogItemId,
            sourceTemplateId = "tpl_1",
            globalCatalogId = "gcat_parrillada",
            productFamilyId = null,
            name = name,
            type = CatalogItemType.PRODUCT,
            taxProfileId = "taxp_iva_0",
            unitCode = "unit",
        )
    }

    private fun sampleTaxProfileSnapshot(): TaxProfileSnapshotForSale {
        return TaxProfileSnapshotForSale(
            code = "iva_0",
            taxName = "IVA",
            rate = Percentage.zero(),
            sriTaxCode = "2",
            sriRateCode = "0",
            treatment = TaxTreatment.IVA_ZERO, //Unresolved reference 'IVA_FULL'.
            legalBasis = "SRI vigente al momento de emisión",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "test_tax_configuration",
        )
    }

    private fun sampleItem(
        id: String = "item_1",
        catalogItemId: String = "cat_1",
    ): SaleItem {
        return SaleItem.create(
            id = id,
            catalogItemId = catalogItemId,
            name = "Parrillada",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(2),
            catalogSnapshot = sampleCatalogSnapshot(catalogItemId = catalogItemId),
            taxProfileSnapshot = sampleTaxProfileSnapshot(),
        )
    }

    @Test
    fun `creates draft sale and adds item`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)

        assertEquals("24.00", sale.total.amount.toPlainString())
        assertEquals(SaleOperationalStatus.DRAFT, sale.operationalStatus)
    }

    @Test
    fun `confirms sale with active items`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)
            .confirm(now)

        assertEquals(SaleOperationalStatus.CONFIRMED, sale.operationalStatus)
    }

    @Test
    fun `rejects confirming sale without items`() {
        val sale = draftSale()

        assertFailsWith<DomainRuleViolation> {
            sale.confirm(now)
        }
    }

    @Test
    fun `registers payment and resolves paid status`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)
            .confirm(now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        val paidSale = sale.registerPayment(payment, now)

        assertEquals("24.00", paidSale.paidAmount.amount.toPlainString())
        assertEquals(SalePaymentStatus.PAID, paidSale.paymentStatus)
        assertEquals(ReceivableStatus.SETTLED, paidSale.receivableStatus(now))
    }

    @Test
    fun `rejects payment while sale is draft`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        assertFailsWith<DomainRuleViolation> {
            sale.registerPayment(payment, now)
        }
    }

    @Test
    fun `rejects closing unpaid sale`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)
            .confirm(now)
            .transitionTo(SaleOperationalStatus.IN_PROGRESS, now)
            .transitionTo(SaleOperationalStatus.READY, now)
            .transitionTo(SaleOperationalStatus.DELIVERED, now)

        assertFailsWith<DomainRuleViolation> {
            sale.transitionTo(SaleOperationalStatus.CLOSED, now)
        }
    }

    @Test
    fun `closes paid sale`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)
            .confirm(now)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = "org_1",
            saleId = "sale_1",
            amount = Money.of("24.00"),
            method = PaymentMethod.CASH,
            paidAt = now,
        )

        val closed = sale
            .registerPayment(payment, now)
            .transitionTo(SaleOperationalStatus.IN_PROGRESS, now)
            .transitionTo(SaleOperationalStatus.READY, now)
            .transitionTo(SaleOperationalStatus.DELIVERED, now)
            .transitionTo(SaleOperationalStatus.CLOSED, now)

        assertEquals(SaleOperationalStatus.CLOSED, closed.operationalStatus)
        assertEquals(SalePaymentStatus.PAID, closed.paymentStatus)
    }
}