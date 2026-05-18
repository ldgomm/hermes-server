package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

/** Read-only check used by mobile calendars before creating or rescheduling. */
data class CheckReservationAvailabilityCommand(
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val resourceId: String? = null,
    val startAt: Instant,
    val endAt: Instant,
    val partySize: Int = 1,
    val excludeReservationId: String? = null,
)

data class ChangeReservationStatusCommand(
    val organizationId: String,
    val reservationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val targetStatus: ReservationStatus,
    val reason: String,
)

data class RescheduleReservationCommand(
    val organizationId: String,
    val reservationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val startAt: Instant,
    val endAt: Instant,
    val reason: String,
)

class CheckReservationAvailabilityUseCase(
    private val schedulingGuard: ReservationSchedulingGuard,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CheckReservationAvailabilityCommand): ReservationAvailabilityResult {
        val canViewCalendar = PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW) ||
            PermissionRules.canPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)
        if (!canViewCalendar) throw DomainRuleViolation("Missing any required permission: ${PermissionCatalog.SALES_VIEW}, ${PermissionCatalog.SALES_CREATE}.")

        return schedulingGuard.checkAvailability(
            ReservationAvailabilityCommand(
                organizationId = command.organizationId,
                branchId = command.branchId,
                activityId = command.activityId,
                resourceId = command.resourceId,
                startAt = command.startAt,
                endAt = command.endAt,
                partySize = command.partySize,
                excludeReservationId = command.excludeReservationId,
            ),
            now = Instant.now(clock),
        )
    }
}

class ChangeReservationStatusUseCase(
    private val reservationRepository: OperationalReservationRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ChangeReservationStatusCommand): ReservationResult {
        val permission = when (command.targetStatus) {
            ReservationStatus.CANCELED -> PermissionCatalog.SALES_CANCEL
            ReservationStatus.CONFIRMED,
            ReservationStatus.IN_PROGRESS,
            ReservationStatus.COMPLETED -> PermissionCatalog.SALES_CONFIRM
            else -> throw DomainRuleViolation("Reservation status transition is not supported by this use case: ${command.targetStatus}.")
        }
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, permission)

        val reason = command.reason.requiredReservation("Reservation status change reason")
        val reservation = reservationRepository.findById(
            organizationId = command.organizationId.requiredReservation("Organization id"),
            reservationId = command.reservationId.requiredReservation("Reservation id"),
        ) ?: throw DomainRuleViolation("Reservation does not exist.")

        val now = Instant.now(clock)
        val before = reservation.status
        val updated = when (command.targetStatus) {
            ReservationStatus.CONFIRMED -> reservation.confirm(now)
            ReservationStatus.IN_PROGRESS -> reservation.start(now)
            ReservationStatus.COMPLETED -> reservation.complete(now)
            ReservationStatus.CANCELED -> reservation.cancel(now)
            else -> throw DomainRuleViolation("Reservation status transition is not supported by this use case: ${command.targetStatus}.")
        }

        reservationRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.RESERVATION_STATUS_CHANGED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = reservation.id,
                before = mapOf("status" to before.name),
                after = mapOf("status" to updated.status.name),
                reason = reason,
                createdAt = now,
            )
        )
        return ReservationResult(updated)
    }
}

class RescheduleReservationUseCase(
    private val reservationRepository: OperationalReservationRepository,
    private val schedulingGuard: ReservationSchedulingGuard,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RescheduleReservationCommand): ReservationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)
        val reason = command.reason.requiredReservation("Reservation reschedule reason")
        val reservation = reservationRepository.findById(
            organizationId = command.organizationId.requiredReservation("Organization id"),
            reservationId = command.reservationId.requiredReservation("Reservation id"),
        ) ?: throw DomainRuleViolation("Reservation does not exist.")

        val now = Instant.now(clock)
        schedulingGuard.assertCanSchedule(
            ReservationAvailabilityCommand(
                organizationId = reservation.organizationId,
                branchId = reservation.branchId,
                activityId = reservation.activityId,
                resourceId = reservation.resourceId,
                startAt = command.startAt,
                endAt = command.endAt,
                partySize = reservation.partySize,
                excludeReservationId = reservation.id,
            ),
            now = now,
        )

        val updated = reservation.reschedule(
            startAt = command.startAt,
            endAt = command.endAt,
            updatedAt = now,
        )
        reservationRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.RESERVATION_STATUS_CHANGED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = reservation.id,
                before = mapOf(
                    "status" to reservation.status.name,
                    "startAt" to reservation.startAt.toString(),
                    "endAt" to reservation.endAt.toString(),
                ),
                after = mapOf(
                    "status" to updated.status.name,
                    "startAt" to updated.startAt.toString(),
                    "endAt" to updated.endAt.toString(),
                ),
                reason = reason,
                createdAt = now,
            )
        )
        return ReservationResult(updated)
    }
}

private fun String.requiredReservation(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
