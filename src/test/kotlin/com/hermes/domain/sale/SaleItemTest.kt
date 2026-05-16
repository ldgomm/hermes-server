package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaleItemTest {

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
            treatment = TaxTreatment.IVA_ZERO,
            legalBasis = "SRI vigente al momento de emisión",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "test_tax_configuration",
        )
    }

    private fun sampleItem(
        id: String = "item_1",
        catalogItemId: String = "cat_1",
        unitPrice: Money = Money.of("12.00"),
        quantity: Quantity = Quantity.units(1),
        discount: Money = Money.zero(unitPrice.currency),
        taxes: List<SaleItemTax> = emptyList(),
    ): SaleItem {
        return SaleItem.create(
            id = id,
            catalogItemId = catalogItemId,
            name = "Parrillada",
            unitPrice = unitPrice,
            quantity = quantity,
            discount = discount,
            catalogSnapshot = sampleCatalogSnapshot(catalogItemId = catalogItemId),
            taxProfileSnapshot = sampleTaxProfileSnapshot(),
            taxes = taxes,
        )
    }

    @Test
    fun `calculates gross total and line total`() {
        val item = sampleItem(
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(2),
            discount = Money.of("1.00"),
        )

        assertEquals("24.00", item.grossTotal.amount.toPlainString())
        assertEquals("23.00", item.lineTotal.amount.toPlainString())
    }

    @Test
    fun `calculates tax total and line total with taxes`() {
        val item = sampleItem(
            unitPrice = Money.of("10.00"),
            quantity = Quantity.units(1),
            taxes = listOf(
                SaleItemTax(
                    taxCode = "2",
                    rateCode = "4",
                    rate = Percentage.of("15.00"),
                    taxableBase = Money.of("10.00"),
                    amount = Money.of("1.50"),
                ),
            ),
        )

        assertEquals("10.00", item.grossTotal.amount.toPlainString())
        assertEquals("1.50", item.taxTotal.amount.toPlainString())
        assertEquals("11.50", item.lineTotal.amount.toPlainString())
    }

    @Test
    fun `rejects discount greater than gross total`() {
        assertFailsWith<DomainRuleViolation> {
            sampleItem(
                unitPrice = Money.of("12.00"),
                quantity = Quantity.units(1),
                discount = Money.of("13.00"),
            )
        }
    }

    @Test
    fun `moves item through operational flow`() {
        val delivered = sampleItem()
            .start()
            .markReady()
            .deliver()

        assertEquals(SaleItemStatus.DELIVERED, delivered.status)
    }

    @Test
    fun `rejects canceling delivered item`() {
        val delivered = sampleItem()
            .start()
            .markReady()
            .deliver()

        assertFailsWith<DomainRuleViolation> {
            delivered.cancel()
        }
    }
}