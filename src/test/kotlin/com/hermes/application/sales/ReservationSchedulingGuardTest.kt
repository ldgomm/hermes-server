package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class ReservationSchedulingGuardTest {
    @Test
    fun `availability is true when there are no overlaps`() {
        val repository = InMemoryOperationalReservationRepository()
        val guard = ReservationSchedulingGuard(repository)

        val result = guard.checkAvailability(
            availabilityCommand(),
            now = SalesTestNow,
        )

        assertTrue(result.available)
        assertEquals(0, result.reservedPartySize)
        assertEquals(emptyList(), result.conflicts)
    }

    @Test
    fun `rejects same resource overlap`() {
        val repository = InMemoryOperationalReservationRepository()
        repository.create(existingReservation(resourceId = "quad_1"))
        val guard = ReservationSchedulingGuard(repository)

        val result = guard.checkAvailability(
            availabilityCommand(resourceId = "quad_1"),
            now = SalesTestNow,
        )

        assertFalse(result.available)
        assertEquals(listOf("res_existing"), result.conflicts.map { it.reservationId })
    }

    @Test
    fun `allows overlapping different resource`() {
        val repository = InMemoryOperationalReservationRepository()
        repository.create(existingReservation(resourceId = "quad_1"))
        val guard = ReservationSchedulingGuard(repository)

        val result = guard.checkAvailability(
            availabilityCommand(resourceId = "quad_2"),
            now = SalesTestNow,
        )

        assertTrue(result.available)
    }

    @Test
    fun `rejects activity capacity overflow`() {
        val repository = InMemoryOperationalReservationRepository()
        repository.create(existingReservation(resourceId = null, partySize = 4))
        val guard = ReservationSchedulingGuard(
            reservationRepository = repository,
            rules = ReservationSchedulingRules(defaultActivityCapacity = 5),
        )

        val result = guard.checkAvailability(
            availabilityCommand(resourceId = null, partySize = 2),
            now = SalesTestNow,
        )

        assertFalse(result.available)
        assertEquals(4, result.reservedPartySize)
        assertEquals(1, result.remainingCapacity)
    }

    @Test
    fun `rejects reservation in the past`() {
        val repository = InMemoryOperationalReservationRepository()
        val guard = ReservationSchedulingGuard(repository)

        val result = guard.checkAvailability(
            availabilityCommand(
                startAt = SalesTestNow.minusSeconds(3_600),
                endAt = SalesTestNow.minusSeconds(1_800),
            ),
            now = SalesTestNow,
        )

        assertFalse(result.available)
        assertTrue(result.violations.any { it.contains("past") })
    }
}

class ReservationSchedulingUseCasesTest {
    @Test
    fun `confirms scheduled reservation`() {
        val repository = InMemoryOperationalReservationRepository()
        repository.create(existingReservation())
        val auditLogger = RecordingSalesAuditLogger()
        val useCase = ChangeReservationStatusUseCase(
            reservationRepository = repository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        )

        val result = useCase.execute(
            ChangeReservationStatusCommand(
                organizationId = "org_1",
                reservationId = "res_existing",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_CONFIRM),
                targetStatus = ReservationStatus.CONFIRMED,
                reason = "Cliente confirmó",
            )
        )

        assertEquals(ReservationStatus.CONFIRMED, result.reservation.status)
        assertEquals(SalesAuditAction.RESERVATION_STATUS_CHANGED, auditLogger.events.single().action)
    }

    @Test
    fun `reschedule rejects overlapping same resource`() {
        val repository = InMemoryOperationalReservationRepository()

        repository.create(
            existingReservation(
                id = "res_1",
                resourceId = "quad_1",
                startOffsetSeconds = 3_600,
                endOffsetSeconds = 5_400,
            )
        )

        repository.create(
            existingReservation(
                id = "res_2",
                resourceId = "quad_1",
                startOffsetSeconds = 7_200,
                endOffsetSeconds = 10_800,
            )
        )

        val guard = ReservationSchedulingGuard(repository)
        val useCase = RescheduleReservationUseCase(
            reservationRepository = repository,
            schedulingGuard = guard,
            clock = SalesTestClock,
        )

        val error = assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                RescheduleReservationCommand(
                    organizationId = "org_1",
                    reservationId = "res_2",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.SALES_CREATE),
                    startAt = SalesTestNow.plusSeconds(3_600),
                    endAt = SalesTestNow.plusSeconds(5_400),
                    reason = "Mover horario",
                )
            )
        }

        assertTrue(error.message.orEmpty().contains("not available"))
    }
}

private fun availabilityCommand(
    resourceId: String? = "quad_1",
    partySize: Int = 1,
    startAt: java.time.Instant = SalesTestNow.plusSeconds(3_600),
    endAt: java.time.Instant = SalesTestNow.plusSeconds(5_400),
): ReservationAvailabilityCommand = ReservationAvailabilityCommand(
    organizationId = "org_1",
    branchId = "br_1",
    activityId = "act_tourism",
    resourceId = resourceId,
    startAt = startAt,
    endAt = endAt,
    partySize = partySize,
)

private fun existingReservation(
    id: String = "res_existing",
    resourceId: String? = "quad_1",
    partySize: Int = 1,
    startOffsetSeconds: Long = 3_600,
    endOffsetSeconds: Long = 5_400,
): Reservation = Reservation.schedule(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    activityId = "act_tourism",
    saleId = null,
    customerId = null,
    customerSnapshot = CustomerSnapshot.finalConsumer(),
    resourceId = resourceId,
    startAt = SalesTestNow.plusSeconds(startOffsetSeconds),
    endAt = SalesTestNow.plusSeconds(endOffsetSeconds),
    partySize = partySize,
    notes = null,
    createdAt = SalesTestNow,
)
