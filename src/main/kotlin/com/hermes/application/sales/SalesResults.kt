package com.hermes.application.sales

import com.hermes.domain.reservation.Reservation
import com.hermes.domain.sale.Sale

data class SaleResult(
    val sale: Sale,
)

data class SalesResult(
    val sales: List<Sale>,
)

data class ReservationResult(
    val reservation: Reservation,
    val linkedSale: Sale? = null,
)

data class ReservationsResult(
    val reservations: List<Reservation>,
)
