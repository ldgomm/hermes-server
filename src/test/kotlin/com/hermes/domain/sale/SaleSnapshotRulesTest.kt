package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.TaxTreatment
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SaleSnapshotRulesTest {

    @Test
    fun `sale item keeps catalog and tax snapshots`() {
        val catalogSnapshot = CatalogItemSnapshot(
            catalogItemId = "item_001",
            sourceTemplateId = "tpl_001",
            globalCatalogId = "gcat_cuy_entero",
            productFamilyId = "pfam_cuy",
            name = "Cuy entero",
            type = CatalogItemType.PRODUCT,
            taxProfileId = "taxp_iva_full",
            unitCode = "unit",
        )

        val taxSnapshot = TaxProfileSnapshotForSale(
            code = "iva_current_full",
            taxName = "IVA",
            rate = Percentage.of("15.00"),
            sriTaxCode = "2",
            sriRateCode = "4",
            treatment = TaxTreatment.IVA_FULL,
            legalBasis = "SRI vigente al momento de emisión",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "admin_tax_configuration",
        )

        val tax = SaleItemTax(
            taxCode = "2",
            rateCode = "4",
            rate = Percentage.of("15.00"),
            taxableBase = Money.of("10.00"),
            amount = Money.of("1.50"),
        )

        val item = SaleItem.create(
            id = "sitem_001",
            catalogItemId = "item_001",
            name = "Cuy entero",
            unitPrice = Money.of("10.00"),
            quantity = Quantity.units(1),
            catalogSnapshot = catalogSnapshot,
            taxProfileSnapshot = taxSnapshot,
            taxes = listOf(tax),
        )

        assertEquals("gcat_cuy_entero", item.catalogSnapshot.globalCatalogId)
        assertEquals("iva_current_full", item.taxProfileSnapshot.code)
        assertEquals(TaxTreatment.IVA_FULL, item.taxProfileSnapshot.treatment)
        assertEquals(Money.of("11.50"), item.lineTotal)
    }
}