package com.hermes.backend.sales

import com.hermes.application.sales.ChangeReservationStatusCommand
import com.hermes.application.sales.CheckReservationAvailabilityCommand
import com.hermes.application.sales.RescheduleReservationCommand
import com.hermes.application.sales.ReservationAvailabilityResult
import com.hermes.application.sales.ReservationScheduleConflict
import com.hermes.domain.reservation.ReservationStatus
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CheckReservationAvailabilityRequest(
    val branchId: String,
    val activityId: String,
    val resourceId: String? = null,
    val startAt: String,
    val endAt: String,
    val partySize: Int = 1,
    val excludeReservationId: String? = null,
)

@Serializable
data class ChangeReservationStatusRequest(
    val targetStatus: String,
    val reason: String,
)

@Serializable
data class RescheduleReservationRequest(
    val startAt: String,
    val endAt: String,
    val reason: String,
)

@Serializable
data class ReservationAvailabilityResponse(
    val available: Boolean,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val resourceId: String?,
    val startAt: String,
    val endAt: String,
    val requestedPartySize: Int,
    val capacityLimit: Int,
    val reservedPartySize: Int,
    val remainingCapacity: Int,
    val conflicts: List<ReservationScheduleConflictResponse>,
    val violations: List<String>,
)

@Serializable
data class ReservationScheduleConflictResponse(
    val reservationId: String,
    val resourceId: String?,
    val startAt: String,
    val endAt: String,
    val partySize: Int,
    val status: String,
    val reason: String,
)

fun CheckReservationAvailabilityRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CheckReservationAvailabilityCommand =
    CheckReservationAvailabilityCommand(
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        resourceId = resourceId,
        startAt = Instant.parse(startAt),
        endAt = Instant.parse(endAt),
        partySize = partySize,
        excludeReservationId = excludeReservationId,
    )

fun ChangeReservationStatusRequest.toCommand(
    organizationId: String,
    reservationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): ChangeReservationStatusCommand =
    ChangeReservationStatusCommand(
        organizationId = organizationId,
        reservationId = reservationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        targetStatus = ReservationStatus.valueOf(targetStatus.trim().uppercase()),
        reason = reason,
    )

fun RescheduleReservationRequest.toCommand(
    organizationId: String,
    reservationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): RescheduleReservationCommand =
    RescheduleReservationCommand(
        organizationId = organizationId,
        reservationId = reservationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        startAt = Instant.parse(startAt),
        endAt = Instant.parse(endAt),
        reason = reason,
    )

fun ReservationAvailabilityResult.toResponse(): ReservationAvailabilityResponse =
    ReservationAvailabilityResponse(
        available = available,
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        resourceId = resourceId,
        startAt = startAt.toString(),
        endAt = endAt.toString(),
        requestedPartySize = requestedPartySize,
        capacityLimit = capacityLimit,
        reservedPartySize = reservedPartySize,
        remainingCapacity = remainingCapacity,
        conflicts = conflicts.map { it.toResponse() },
        violations = violations,
    )

private fun ReservationScheduleConflict.toResponse(): ReservationScheduleConflictResponse =
    ReservationScheduleConflictResponse(
        reservationId = reservationId,
        resourceId = resourceId,
        startAt = startAt.toString(),
        endAt = endAt.toString(),
        partySize = partySize,
        status = status.name,
        reason = reason,
    )
