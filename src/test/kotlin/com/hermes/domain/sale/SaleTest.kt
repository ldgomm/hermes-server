package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.TaxTreatment
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.time.LocalDate

class SaleTest {
    private val now: Instant = Instant.parse("2026-05-18T12:00:00Z")

    @Test
    fun `changes sale item status without exposing Sale copy outside domain`() {
        val sale = draftSale()
            .addItem(sampleItem(), now)
            .confirm(now)

        val updated = sale.changeItemStatus("sitem_1", SaleItemStatus.READY, now.plusSeconds(60))

        assertEquals(SaleItemStatus.READY, updated.items.single().status)
        assertEquals(now.plusSeconds(60), updated.updatedAt)
    }

    @Test
    fun `restores sale from persistence snapshot`() {
        val restored = Sale.restore(
            id = "sale_1",
            organizationId = "org_1",
            branchId = "branch_1",
            activityId = "act_1",
            saleNumber = "S-000001",
            saleType = SaleType.SALE,
            workflowMode = SaleWorkflowMode.QUICK_SALE,
            customerId = null,
            customerSnapshot = CustomerSnapshot.finalConsumer(),
            items = listOf(sampleItem()),
            operationalStatus = SaleOperationalStatus.CONFIRMED,
            dueAt = null,
            cashSessionId = null,
            createdAt = now,
            updatedAt = now,
        )

        assertEquals(SaleOperationalStatus.CONFIRMED, restored.operationalStatus)
        assertEquals("12.00", restored.total.amount.toPlainString())
    }

    private fun draftSale(): Sale =
        Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            branchId = "branch_1",
            activityId = "act_1",
            createdAt = now,
        )

    private fun sampleItem(): SaleItem =
        SaleItem.create(
            id = "sitem_1",
            catalogItemId = "cat_1",
            name = "Producto prueba",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(1),
            catalogSnapshot = CatalogItemSnapshot(
                catalogItemId = "cat_1",
                sourceTemplateId = "tpl_1",
                globalCatalogId = "gcat_1",
                productFamilyId = null,
                name = "Producto prueba",
                type = CatalogItemType.PRODUCT,
                taxProfileId = "tax_iva_0",
                unitCode = "unit",
            ),
            taxProfileSnapshot = TaxProfileSnapshotForSale(
                code = "iva_0",
                taxName = "IVA",
                rate = Percentage.zero(),
                sriTaxCode = "2",
                sriRateCode = "0",
                treatment = TaxTreatment.IVA_ZERO,
                legalBasis = "Test",
                effectiveFrom = LocalDate.parse("2026-01-01"),
                source = "test",
            ),
        )
}
