package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.SaleItemStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SalesLifecycleUseCasesTest {
    @Test
    fun `gets sale by organization and writes view audit`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(confirmedSale())

        val result = fixture.getSaleUseCase.execute(
            GetSaleCommand(
                organizationId = "org_1",
                saleId = "sale_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
            )
        )

        assertEquals("sale_1", result.sale.id)
        assertEquals(SalesAuditAction.SALE_VIEWED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `searches sales by status and activity`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(confirmedSale(id = "sale_1", createdAt = SalesTestNow.minusSeconds(60)))
        fixture.saleRepository.create(draftSale(id = "sale_2", createdAt = SalesTestNow))

        val result = fixture.searchSalesUseCase.execute(
            SearchSalesCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                statuses = setOf(SaleOperationalStatus.CONFIRMED),
                activityId = "act_restaurant",
            )
        )

        assertEquals(listOf("sale_1"), result.sales.map { it.id })
        assertEquals(SalesAuditAction.SALE_LISTED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `changes sale item status and persists updated aggregate`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(confirmedSale())

        val result = fixture.changeSaleItemStatusUseCase.execute(
            ChangeSaleItemStatusCommand(
                organizationId = "org_1",
                saleId = "sale_1",
                saleItemId = "sitem_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_ITEMS_CHANGE_STATUS),
                targetStatus = SaleItemStatus.READY,
                reason = "Kitchen finished item",
            )
        )

        assertEquals(SaleItemStatus.READY, result.sale.items.single().status)
        assertEquals(SaleItemStatus.READY, fixture.saleRepository.findById("org_1", "sale_1")!!.items.single().status)
        assertEquals(SalesAuditAction.SALE_STATUS_CHANGED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `cancels unpaid confirmed sale with cancel permission`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(confirmedSale())

        val result = fixture.cancelSaleUseCase.execute(
            CancelSaleCommand(
                organizationId = "org_1",
                saleId = "sale_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_CANCEL),
                reason = "Client changed order",
            )
        )

        assertEquals(SaleOperationalStatus.CANCELED, result.sale.operationalStatus)
        assertEquals(SalesAuditAction.SALE_CANCELED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `adds item only while sale is mutable`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(draftSale(item = basicSaleItem(id = "seed_item")))

        val result = fixture.addSaleItemUseCase.execute(
            AddSaleItemCommand(
                organizationId = "org_1",
                saleId = "sale_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_CREATE),
                occurredAt = SalesTestNow,
                item = saleLine(quantity = 1),
            )
        )

        assertEquals(2, result.sale.items.size)
        assertEquals(Money.of("22.60"), result.sale.totals.grandTotal)
        assertTrue(fixture.auditLogger.events.any { it.action == SalesAuditAction.SALE_ITEM_ADDED })
    }

    @Test
    fun `rejects item status change without permission`() {
        val fixture = salesFixture()
        fixture.saleRepository.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.changeSaleItemStatusUseCase.execute(
                ChangeSaleItemStatusCommand(
                    organizationId = "org_1",
                    saleId = "sale_1",
                    saleItemId = "sitem_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                    targetStatus = SaleItemStatus.READY,
                    reason = "No permission",
                )
            )
        }
    }
}
