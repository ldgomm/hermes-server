package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReservationUseCasesTest {
    @Test
    fun `creates reservation without converting it into a sale`() {
        val fixture = salesFixture()

        val result = fixture.createReservationUseCase.execute(scheduledReservationCommand())

        assertEquals("res_1", result.reservation.id)
        assertEquals(ReservationStatus.SCHEDULED, result.reservation.status)
        assertEquals("act_tourism", result.reservation.activityId)
        assertEquals("quad_1", result.reservation.resourceId)
        assertEquals(null, result.linkedSale)
        assertEquals(null, result.reservation.saleId)
        assertEquals(SalesAuditAction.RESERVATION_CREATED, fixture.auditLogger.events.single().action)
    }

    @Test
    fun `rejects reservation creation when same resource already overlaps`() {
        val fixture = salesFixture()

        fixture.createReservationUseCase.execute(scheduledReservationCommand())

        val error = assertFailsWith<DomainRuleViolation> {
            fixture.createReservationUseCase.execute(scheduledReservationCommand())
        }

        assertTrue(error.message.orEmpty().contains("not available"))
        assertEquals(listOf("res_1"), fixture.reservationRepository.reservations.keys.toList())
        assertEquals(1, fixture.auditLogger.events.count { it.action == SalesAuditAction.RESERVATION_CREATED })
    }

    @Test
    fun `allows overlapping reservations for different resources`() {
        val fixture = salesFixture()

        val first = fixture.createReservationUseCase.execute(
            scheduledReservationCommand(resourceId = "quad_1")
        )
        val second = fixture.createReservationUseCase.execute(
            scheduledReservationCommand(resourceId = "quad_2")
        )

        assertEquals("res_1", first.reservation.id)
        assertEquals("res_2", second.reservation.id)
        assertEquals(listOf("quad_1", "quad_2"), fixture.reservationRepository.reservations.values.map { it.resourceId })
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
    fun `gets reservation by id and audits view`() {
        val fixture = salesFixture()
        fixture.reservationRepository.create(scheduledReservation(id = "res_1", activityId = "act_tourism"))

        val result = fixture.getReservationUseCase.execute(
            GetReservationCommand(
                organizationId = "org_1",
                reservationId = "res_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
            )
        )

        assertEquals("res_1", result.reservation.id)
        assertEquals(SalesAuditAction.RESERVATION_VIEWED, fixture.auditLogger.events.single().action)
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
