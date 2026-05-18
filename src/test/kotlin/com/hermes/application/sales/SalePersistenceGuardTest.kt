package com.hermes.application.sales

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.*
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SalePersistenceGuardTest {
    private val guard = SalePersistenceGuard()
    private val now: Instant = Instant.parse("2026-05-18T00:00:00Z")

    @Test
    fun `accepts sale with coherent totals and tax lines`() {
        val sale = Sale.createDraft(
            id = "sale_1",
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            createdAt = now,
        ).addItem(taxedItem(), now)

        guard.assertReadyToPersist(sale)
    }

    @Test
    fun `rejects active sale without active items`() {
        val sale = Sale.createDraft(
            id = "sale_empty",
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            createdAt = now,
        )

        assertFailsWith<DomainRuleViolation> {
            guard.assertReadyToPersist(sale)
        }
    }

    @Test
    fun `rejects taxed sale item without tax line`() {
        val sale = Sale.createDraft(
            id = "sale_without_tax_line",
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            createdAt = now,
        ).addItem(taxedItemWithoutTaxLine(), now)

        assertFailsWith<DomainRuleViolation> {
            guard.assertReadyToPersist(sale)
        }
    }

    private fun taxedItem(): SaleItem =
        SaleItem.create(
            id = "sitem_1",
            catalogItemId = "ocat_1",
            name = "Cuy entero",
            unitPrice = Money.of("24.00"),
            quantity = Quantity.of("1", "unit", allowsDecimal = false),
            discount = Money.zero(),
            catalogSnapshot = catalogSnapshot(),
            taxProfileSnapshot = taxSnapshot(),
            taxes = listOf(
                SaleItemTax(
                    taxCode = "2",
                    rateCode = "4",
                    rate = Percentage.of("15.0000"),
                    taxableBase = Money.of("24.00"),
                    amount = Money.of("3.60"),
                )
            ),
        )

    private fun taxedItemWithoutTaxLine(): SaleItem =
        SaleItem.create(
            id = "sitem_2",
            catalogItemId = "ocat_1",
            name = "Cuy entero",
            unitPrice = Money.of("24.00"),
            quantity = Quantity.of("1", "unit", allowsDecimal = false),
            discount = Money.zero(),
            catalogSnapshot = catalogSnapshot(),
            taxProfileSnapshot = taxSnapshot(),
            taxes = emptyList(),
        )

    private fun catalogSnapshot(): CatalogItemSnapshot =
        CatalogItemSnapshot(
            catalogItemId = "ocat_1",
            sourceTemplateId = "tpl_1",
            globalCatalogId = "restaurant_cuy_entero",
            productFamilyId = "pfam_1",
            name = "Cuy entero",
            type = CatalogItemType.PRODUCT,
            taxProfileId = "tax_iva_current_full",
            unitCode = "unit",
        )

    private fun taxSnapshot(): TaxProfileSnapshotForSale =
        TaxProfileSnapshotForSale(
            code = "iva_current_full",
            taxName = "IVA",
            rate = Percentage.of("15.0000"),
            sriTaxCode = "2",
            sriRateCode = "4",
            treatment = TaxTreatment.IVA_FULL,
            legalBasis = "SRI vigente al momento de emisión",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "SYSTEM_SEED",
        )
}
