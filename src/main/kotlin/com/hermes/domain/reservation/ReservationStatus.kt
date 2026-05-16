package com.hermes.domain.reservation

enum class ReservationStatus {
    DRAFT,
    SCHEDULED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    NO_SHOW,
    CANCELED,
    RESCHEDULED
}
