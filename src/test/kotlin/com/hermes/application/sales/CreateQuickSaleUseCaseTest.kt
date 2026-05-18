package com.hermes.application.sales

import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreateQuickSaleUseCaseTest {
    @Test
    fun `creates quick sale from active catalog item with tax snapshots and audit`() {
        val fixture = salesFixture()

        val result = fixture.createQuickSaleUseCase.execute(saleCommand())

        assertEquals("sale_1", result.sale.id)
        assertEquals(SaleOperationalStatus.CONFIRMED, result.sale.operationalStatus)
        assertEquals(1, result.sale.items.size)
        assertEquals("cat_1", result.sale.items.single().catalogItemId)
        assertEquals("iva_current_full", result.sale.items.single().taxProfileSnapshot.code)
        assertEquals(Money.of("20.00"), result.sale.totals.subtotal)
        assertEquals(Money.of("2.60"), result.sale.totals.taxTotal)
        assertEquals(Money.of("22.60"), result.sale.totals.grandTotal)
        assertEquals(result.sale, fixture.saleRepository.findById("org_1", "sale_1"))
        assertEquals(SalesAuditAction.SALE_CREATED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `creates draft sale when auto confirm is disabled`() {
        val fixture = salesFixture()

        val result = fixture.createQuickSaleUseCase.execute(
            saleCommand(autoConfirm = false, items = listOf(saleLine(quantity = 1)))
        )

        assertEquals(SaleOperationalStatus.DRAFT, result.sale.operationalStatus)
        assertEquals(Money.of("11.30"), result.sale.totals.grandTotal)
    }

    @Test
    fun `rejects quick sale without sales create permission`() {
        val fixture = salesFixture()

        assertFailsWith<DomainRuleViolation> {
            fixture.createQuickSaleUseCase.execute(
                saleCommand(permissions = setOf(PermissionCatalog.SALES_VIEW))
            )
        }
    }

    @Test
    fun `rejects sale when catalog item is not active`() {
        val fixture = salesFixture()
        fixture.catalogRepository.update(
            activeCatalogItem(id = "cat_paused", status = CatalogItemStatus.PAUSED)
        )

        assertFailsWith<DomainRuleViolation> {
            fixture.createQuickSaleUseCase.execute(
                saleCommand(items = listOf(saleLine(catalogItemId = "cat_paused")))
            )
        }
    }

    @Test
    fun `rejects sale when tax profile is not enabled for organization`() {
        val fixture = salesFixture()
        fixture.settingsRepository.update(
            activeTaxSettings().copy(
                enabledTaxProfileCodes = setOf("iva_0"),
                defaultTaxProfileCode = "iva_0",
            )
        )

        val error = assertFailsWith<DomainRuleViolation> {
            fixture.createQuickSaleUseCase.execute(saleCommand())
        }

        assertTrue(error.message.orEmpty().contains("not enabled"))
    }
}
