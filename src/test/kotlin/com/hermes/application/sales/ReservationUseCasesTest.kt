package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ReservationUseCasesTest {
    @Test
    fun `creates reservation without converting it into a sale`() {
        val fixture = salesFixture()

        val result = fixture.createReservationUseCase.execute(scheduledReservationCommand())

        assertEquals("res_1", result.reservation.id)
        assertEquals(ReservationStatus.SCHEDULED, result.reservation.status)
        assertEquals("act_tourism", result.reservation.activityId)
        assertEquals(null, result.linkedSale)
        assertEquals(null, result.reservation.saleId)
        assertEquals(SalesAuditAction.RESERVATION_CREATED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `creates reservation with linked sale when service line is provided`() {
        val fixture = salesFixture()
        fixture.catalogRepository.create(
            activeCatalogItem(
                id = "cat_quad",
                name = "Ruta en cuadron",
                price = "20.00",
                activityId = "act_tourism",
            )
        )

        val result = fixture.createReservationUseCase.execute(
            scheduledReservationCommand(linkedSaleItem = saleLine(catalogItemId = "cat_quad"))
        )

        assertNotNull(result.linkedSale)
        assertEquals("sale_1", result.linkedSale.id)
        assertEquals("sale_1", result.reservation.saleId)
        assertEquals("res_1", result.reservation.id)
        assertEquals(2, fixture.auditLogger.events.size)
        assertEquals(SalesAuditAction.SALE_CREATED, fixture.auditLogger.events.first().action)
        assertEquals(SalesAuditAction.RESERVATION_CREATED, fixture.auditLogger.events.last().action)
    }

    @Test
    fun `searches reservations by activity and date range`() {
        val fixture = salesFixture()
        fixture.reservationRepository.create(scheduledReservation(id = "res_1", activityId = "act_tourism"))
        fixture.reservationRepository.create(scheduledReservation(id = "res_2", activityId = "act_restaurant"))

        val result = fixture.searchReservationsUseCase.execute(
            SearchReservationsCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                activityId = "act_tourism",
                from = SalesTestNow,
                to = SalesTestNow.plusSeconds(10_000),
            )
        )

        assertEquals(listOf("res_1"), result.reservations.map { it.id })
        assertEquals(SalesAuditAction.RESERVATION_LISTED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `rejects reservation creation without sales create permission`() {
        val fixture = salesFixture()

        assertFailsWith<DomainRuleViolation> {
            fixture.createReservationUseCase.execute(
                scheduledReservationCommand(permissions = setOf(PermissionCatalog.SALES_VIEW))
            )
        }
    }
}
