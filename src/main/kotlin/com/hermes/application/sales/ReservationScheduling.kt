package com.hermes.application.sales

import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Duration
import java.time.Instant

data class ReservationSchedulingRules(
    val maxDaysAhead: Long = 365,
    val maxReservationDuration: Duration = Duration.ofHours(12),
    val minReservationDuration: Duration = Duration.ofMinutes(5),
    val defaultActivityCapacity: Int = 50,
    val defaultResourceCapacity: Int = 1,
    val searchLimit: Int = 1_000,
) {
    init {
        require(maxDaysAhead >= 1) { "maxDaysAhead must be positive." }
        require(!maxReservationDuration.isZero && !maxReservationDuration.isNegative) {
            "maxReservationDuration must be positive."
        }
        require(!minReservationDuration.isZero && !minReservationDuration.isNegative) {
            "minReservationDuration must be positive."
        }
        require(minReservationDuration <= maxReservationDuration) {
            "minReservationDuration cannot be greater than maxReservationDuration."
        }
        require(defaultActivityCapacity >= 1) { "defaultActivityCapacity must be at least 1." }
        require(defaultResourceCapacity >= 1) { "defaultResourceCapacity must be at least 1." }
        require(searchLimit in 1..5_000) { "searchLimit must be between 1 and 5000." }
    }
}

data class ReservationAvailabilityCommand(
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val resourceId: String? = null,
    val startAt: Instant,
    val endAt: Instant,
    val partySize: Int = 1,
    val excludeReservationId: String? = null,
)

data class ReservationAvailabilityResult(
    val available: Boolean,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val resourceId: String?,
    val startAt: Instant,
    val endAt: Instant,
    val requestedPartySize: Int,
    val capacityLimit: Int,
    val reservedPartySize: Int,
    val remainingCapacity: Int,
    val conflicts: List<ReservationScheduleConflict>,
    val violations: List<String>,
) {
    fun assertAvailable() {
        if (!available) {
            val details = (violations + conflicts.map { it.reason })
                .distinct()
                .joinToString("; ")
            throw DomainRuleViolation(
                "Reservation slot is not available${if (details.isBlank()) "." else ": $details."}"
            )
        }
    }
}

data class ReservationScheduleConflict(
    val reservationId: String,
    val resourceId: String?,
    val startAt: Instant,
    val endAt: Instant,
    val partySize: Int,
    val status: ReservationStatus,
    val reason: String,
)

class ReservationSchedulingGuard(
    private val reservationRepository: OperationalReservationRepository,
    private val rules: ReservationSchedulingRules = ReservationSchedulingRules(),
) {
    fun checkAvailability(command: ReservationAvailabilityCommand, now: Instant): ReservationAvailabilityResult {
        val violations = mutableListOf<String>()
        val organizationId = command.organizationId.requiredScheduling("Reservation organization id")
        val branchId = command.branchId.requiredScheduling("Reservation branch id")
        val activityId = command.activityId.requiredScheduling("Reservation activity id")
        val resourceId = command.resourceId?.trim()?.takeIf { it.isNotBlank() }
        val excludeReservationId = command.excludeReservationId?.trim()?.takeIf { it.isNotBlank() }
        val requestedPartySize = command.partySize.coerceAtLeast(0)

        if (!command.endAt.isAfter(command.startAt)) {
            violations += "Reservation end must be after start."
        }
        if (command.partySize < 1) {
            violations += "Reservation party size must be at least one."
        }
        if (command.startAt.isBefore(now)) {
            violations += "Reservation cannot start in the past."
        }
        if (command.startAt.isAfter(now.plus(Duration.ofDays(rules.maxDaysAhead)))) {
            violations += "Reservation cannot be scheduled more than ${rules.maxDaysAhead} days ahead."
        }

        val duration = Duration.between(command.startAt, command.endAt)
        if (!duration.isNegative && !duration.isZero) {
            if (duration < rules.minReservationDuration) {
                violations += "Reservation duration must be at least ${rules.minReservationDuration.toMinutes()} minutes."
            }
            if (duration > rules.maxReservationDuration) {
                violations += "Reservation duration cannot exceed ${rules.maxReservationDuration.toHours()} hours."
            }
        }

        if (violations.isNotEmpty()) {
            return ReservationAvailabilityResult(
                available = false,
                organizationId = organizationId,
                branchId = branchId,
                activityId = activityId,
                resourceId = resourceId,
                startAt = command.startAt,
                endAt = command.endAt,
                requestedPartySize = requestedPartySize,
                capacityLimit = capacityLimit(resourceId),
                reservedPartySize = 0,
                remainingCapacity = 0,
                conflicts = emptyList(),
                violations = violations,
            )
        }

        val candidates = reservationRepository.search(
            ReservationSearchQuery(
                organizationId = organizationId,
                statuses = activeStatuses,
                activityId = activityId,
                from = command.startAt.minus(rules.maxReservationDuration),
                to = command.endAt,
                limit = rules.searchLimit,
            )
        )
            .asSequence()
            .filter { it.id != excludeReservationId }
            .filter { it.organizationId == organizationId }
            .filter { it.branchId == branchId }
            .filter { it.activityId == activityId }
            .filter { it.status in activeStatuses }
            .filter { it.overlaps(command.startAt, command.endAt) }
            .toList()

        val capacityLimit = capacityLimit(resourceId)

        val conflicts = mutableListOf<ReservationScheduleConflict>()
        val reservedPartySize: Int

        if (resourceId == null) {
            reservedPartySize = candidates.sumOf { it.partySize }
            val remainingCapacity = capacityLimit - reservedPartySize

            if (requestedPartySize > remainingCapacity) {
                conflicts += ReservationScheduleConflict(
                    reservationId = "capacity",
                    resourceId = null,
                    startAt = command.startAt,
                    endAt = command.endAt,
                    partySize = requestedPartySize,
                    status = ReservationStatus.SCHEDULED,
                    reason = "Requested party size exceeds remaining capacity. Limit=$capacityLimit, reserved=$reservedPartySize, requested=$requestedPartySize.",
                )
            }

            return ReservationAvailabilityResult(
                available = conflicts.isEmpty(),
                organizationId = organizationId,
                branchId = branchId,
                activityId = activityId,
                resourceId = null,
                startAt = command.startAt,
                endAt = command.endAt,
                requestedPartySize = requestedPartySize,
                capacityLimit = capacityLimit,
                reservedPartySize = reservedPartySize,
                remainingCapacity = remainingCapacity.coerceAtLeast(0),
                conflicts = conflicts,
                violations = emptyList(),
            )
        }
        val sameResourceReservations = candidates.filter { it.resourceId == resourceId }
        reservedPartySize = sameResourceReservations.sumOf { it.partySize }

        conflicts += sameResourceReservations.map {
            ReservationScheduleConflict(
                reservationId = it.id,
                resourceId = it.resourceId,
                startAt = it.startAt,
                endAt = it.endAt,
                partySize = it.partySize,
                status = it.status,
                reason = "Resource is already reserved in the requested time range.",
            )
        }

        /**
         * Important:
         *
         * For a specific resource, defaultResourceCapacity means:
         * "how many simultaneous reservations can use this resource",
         * not "how many people can be inside the reservation".
         *
         * Example:
         * - quad_1 can have only one reservation at 11:00.
         * - that reservation can still have partySize = 2.
         *
         * Passenger/person capacity must be modeled later as resource metadata,
         * not inferred from defaultResourceCapacity.
         */
        val reservedResourceSlots = sameResourceReservations.size
        val remainingCapacity = (capacityLimit - reservedResourceSlots).coerceAtLeast(0)

        return ReservationAvailabilityResult(
            available = conflicts.isEmpty(),
            organizationId = organizationId,
            branchId = branchId,
            activityId = activityId,
            resourceId = resourceId,
            startAt = command.startAt,
            endAt = command.endAt,
            requestedPartySize = requestedPartySize,
            capacityLimit = capacityLimit,
            reservedPartySize = reservedPartySize,
            remainingCapacity = remainingCapacity,
            conflicts = conflicts,
            violations = emptyList(),
        )
    }

    fun assertCanSchedule(command: ReservationAvailabilityCommand, now: Instant) {
        checkAvailability(command, now).assertAvailable()
    }

    private fun capacityLimit(resourceId: String?): Int =
        if (resourceId == null) rules.defaultActivityCapacity else rules.defaultResourceCapacity

    companion object {
        val activeStatuses: Set<ReservationStatus> = setOf(
            ReservationStatus.SCHEDULED,
            ReservationStatus.CONFIRMED,
            ReservationStatus.IN_PROGRESS,
            ReservationStatus.RESCHEDULED,
        )
    }
}

private fun Reservation.overlaps(startAt: Instant, endAt: Instant): Boolean =
    this.startAt.isBefore(endAt) && startAt.isBefore(this.endAt)

private fun String.requiredScheduling(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")