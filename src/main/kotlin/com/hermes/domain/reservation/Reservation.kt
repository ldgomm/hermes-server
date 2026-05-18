package com.hermes.domain.reservation

import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

/**
 * Minimal reservation aggregate for Fase 8.
 *
 * It intentionally keeps operational reservation data separated from Sale. A reservation
 * can reference a sale when it monetizes a service/activity, but it is still its own
 * schedulable operation.
 */
data class Reservation(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleId: String?,
    val customerId: String?,
    val customerSnapshot: CustomerSnapshot,
    val resourceId: String?,
    val startAt: Instant,
    val endAt: Instant,
    val partySize: Int,
    val status: ReservationStatus,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Reservation id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Reservation organization id cannot be blank.")
        if (branchId.isBlank()) throw DomainRuleViolation("Reservation branch id cannot be blank.")
        if (activityId.isBlank()) throw DomainRuleViolation("Reservation activity id cannot be blank.")
        if (!endAt.isAfter(startAt)) throw DomainRuleViolation("Reservation end must be after start.")
        if (partySize < 1) throw DomainRuleViolation("Reservation party size must be at least one.")
        if (customerId != null && customerId.isBlank()) throw DomainRuleViolation("Reservation customer id cannot be blank.")
        if (saleId != null && saleId.isBlank()) throw DomainRuleViolation("Reservation sale id cannot be blank.")
        if (resourceId != null && resourceId.isBlank()) throw DomainRuleViolation("Reservation resource id cannot be blank.")
    }

    fun confirm(updatedAt: Instant): Reservation {
        ReservationStateMachine.assertCanTransition(status, ReservationStatus.CONFIRMED)
        return copy(status = ReservationStatus.CONFIRMED, updatedAt = updatedAt)
    }

    fun start(updatedAt: Instant): Reservation {
        ReservationStateMachine.assertCanTransition(status, ReservationStatus.IN_PROGRESS)
        return copy(status = ReservationStatus.IN_PROGRESS, updatedAt = updatedAt)
    }

    fun complete(updatedAt: Instant): Reservation {
        ReservationStateMachine.assertCanTransition(status, ReservationStatus.COMPLETED)
        return copy(status = ReservationStatus.COMPLETED, updatedAt = updatedAt)
    }

    fun cancel(updatedAt: Instant): Reservation {
        ReservationStateMachine.assertCanTransition(status, ReservationStatus.CANCELED)
        return copy(status = ReservationStatus.CANCELED, updatedAt = updatedAt)
    }

    fun reschedule(startAt: Instant, endAt: Instant, updatedAt: Instant): Reservation {
        ReservationStateMachine.assertCanTransition(status, ReservationStatus.RESCHEDULED)
        if (!endAt.isAfter(startAt)) throw DomainRuleViolation("Reservation end must be after start.")
        return copy(
            status = ReservationStatus.RESCHEDULED,
            startAt = startAt,
            endAt = endAt,
            updatedAt = updatedAt,
        )
    }

    companion object {
        fun schedule(
            id: String,
            organizationId: String,
            branchId: String,
            activityId: String,
            saleId: String? = null,
            customerId: String? = null,
            customerSnapshot: CustomerSnapshot = CustomerSnapshot.finalConsumer(),
            resourceId: String? = null,
            startAt: Instant,
            endAt: Instant,
            partySize: Int,
            notes: String? = null,
            createdAt: Instant,
        ): Reservation = Reservation(
            id = id,
            organizationId = organizationId,
            branchId = branchId,
            activityId = activityId,
            saleId = saleId?.trim()?.takeIf { it.isNotBlank() },
            customerId = customerId?.trim()?.takeIf { it.isNotBlank() },
            customerSnapshot = customerSnapshot,
            resourceId = resourceId?.trim()?.takeIf { it.isNotBlank() },
            startAt = startAt,
            endAt = endAt,
            partySize = partySize,
            status = ReservationStatus.SCHEDULED,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }
}
