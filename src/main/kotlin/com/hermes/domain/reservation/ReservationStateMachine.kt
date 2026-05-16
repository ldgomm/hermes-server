package com.hermes.domain.reservation

import com.hermes.domain.shared.StateTransitionValidator

object ReservationStateMachine {
    private val validator = StateTransitionValidator(
        entityName = "reservation",
        transitions = mapOf(
            ReservationStatus.DRAFT to setOf(
                ReservationStatus.SCHEDULED,
                ReservationStatus.CANCELED,
            ),
            ReservationStatus.SCHEDULED to setOf(
                ReservationStatus.CONFIRMED,
                ReservationStatus.RESCHEDULED,
                ReservationStatus.NO_SHOW,
                ReservationStatus.CANCELED,
            ),
            ReservationStatus.CONFIRMED to setOf(
                ReservationStatus.IN_PROGRESS,
                ReservationStatus.RESCHEDULED,
                ReservationStatus.NO_SHOW,
                ReservationStatus.CANCELED,
            ),
            ReservationStatus.IN_PROGRESS to setOf(
                ReservationStatus.COMPLETED,
                ReservationStatus.CANCELED,
            ),
            ReservationStatus.RESCHEDULED to setOf(
                ReservationStatus.SCHEDULED,
                ReservationStatus.CANCELED,
            ),
            ReservationStatus.COMPLETED to emptySet(),
            ReservationStatus.NO_SHOW to emptySet(),
            ReservationStatus.CANCELED to emptySet(),
        ),
    )

    fun canTransition(from: ReservationStatus, to: ReservationStatus): Boolean =
        validator.canTransition(from, to)

    fun assertCanTransition(from: ReservationStatus, to: ReservationStatus) {
        validator.assertCanTransition(from, to)
    }
}
