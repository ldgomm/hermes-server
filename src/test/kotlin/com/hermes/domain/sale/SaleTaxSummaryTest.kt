package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.TaxTreatment
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDate

class SaleTaxSummaryTest {
    @Test
    fun `summarizes taxable and zero rate bases from sale item snapshots`() {
        val taxedItem = saleItem(
            id = "sitem_1",
            catalogItemId = "ocat_1",
            name = "Producto gravado",
            unitPrice = Money.of("10.00"),
            quantity = Quantity.units(2),
            treatment = TaxTreatment.IVA_FULL,
            rate = "15.00",
            sriRateCode = "4",
            taxBase = Money.of("20.00"),
            taxAmount = Money.of("3.00"),
        )
        val zeroRateItem = saleItem(
            id = "sitem_2",
            catalogItemId = "ocat_2",
            name = "Producto tarifa cero",
            unitPrice = Money.of("5.00"),
            quantity = Quantity.units(1),
            treatment = TaxTreatment.IVA_ZERO,
            rate = "0.00",
            sriRateCode = "0",
            taxBase = Money.of("5.00"),
            taxAmount = Money.of("0.00"),
        )

        val summary = SaleTaxSummary.fromItems(listOf(taxedItem, zeroRateItem))

        assertEquals("20.00", summary.subtotalTaxable.amount.toPlainString())
        assertEquals("5.00", summary.subtotalZeroRate.amount.toPlainString())
        assertEquals("3.00", summary.taxTotal.amount.toPlainString())
        assertEquals(2, summary.taxesByRate.size)
    }

    @Test
    fun `ignores canceled sale items in tax summary`() {
        val activeItem = saleItem(
            id = "sitem_1",
            catalogItemId = "ocat_1",
            name = "Activo",
            unitPrice = Money.of("10.00"),
            quantity = Quantity.units(1),
            treatment = TaxTreatment.IVA_FULL,
            rate = "15.00",
            sriRateCode = "4",
            taxBase = Money.of("10.00"),
            taxAmount = Money.of("1.50"),
        )
        val canceledItem = saleItem(
            id = "sitem_2",
            catalogItemId = "ocat_2",
            name = "Cancelado",
            unitPrice = Money.of("100.00"),
            quantity = Quantity.units(1),
            treatment = TaxTreatment.IVA_FULL,
            rate = "15.00",
            sriRateCode = "4",
            taxBase = Money.of("100.00"),
            taxAmount = Money.of("15.00"),
        ).cancel()

        val summary = SaleTaxSummary.fromItems(listOf(activeItem, canceledItem))

        assertEquals("10.00", summary.subtotalTaxable.amount.toPlainString())
        assertEquals("1.50", summary.taxTotal.amount.toPlainString())
    }

    private fun saleItem(
        id: String,
        catalogItemId: String,
        name: String,
        unitPrice: Money,
        quantity: Quantity,
        treatment: TaxTreatment,
        rate: String,
        sriRateCode: String,
        taxBase: Money,
        taxAmount: Money,
    ): SaleItem =
        SaleItem.create(
            id = id,
            catalogItemId = catalogItemId,
            name = name,
            unitPrice = unitPrice,
            quantity = quantity,
            discount = Money.zero(unitPrice.currency),
            catalogSnapshot = CatalogItemSnapshot(
                catalogItemId = catalogItemId,
                sourceTemplateId = "tpl_$catalogItemId",
                globalCatalogId = "global_$catalogItemId",
                productFamilyId = null,
                name = name,
                type = CatalogItemType.PRODUCT,
                taxProfileId = "tax_$catalogItemId",
                unitCode = "unit",
            ),
            taxProfileSnapshot = TaxProfileSnapshotForSale(
                code = "profile_$catalogItemId",
                taxName = "IVA",
                rate = Percentage.of(rate),
                sriTaxCode = "2",
                sriRateCode = sriRateCode,
                treatment = treatment,
                legalBasis = "Test",
                effectiveFrom = LocalDate.parse("2026-01-01"),
                source = "TEST",
            ),
            taxes = listOf(
                SaleItemTax(
                    taxCode = "2",
                    rateCode = sriRateCode,
                    rate = Percentage.of(rate),
                    taxableBase = taxBase,
                    amount = taxAmount,
                )
            ),
        )
}
