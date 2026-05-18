package com.hermes.backend.sales

import com.hermes.application.sales.ChangeReservationStatusUseCase
import com.hermes.application.sales.CheckReservationAvailabilityUseCase
import com.hermes.application.sales.RescheduleReservationUseCase

/** Separate module so Fase 8.5 does not disturb existing SalesModule wiring. */
data class ReservationSchedulingModule(
    val checkReservationAvailabilityUseCase: CheckReservationAvailabilityUseCase,
    val changeReservationStatusUseCase: ChangeReservationStatusUseCase,
    val rescheduleReservationUseCase: RescheduleReservationUseCase,
)
