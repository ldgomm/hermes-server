package com.hermes.application.sales

import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleOperationalStatus
import java.time.Instant

interface OperationalSaleRepository {
    fun create(sale: Sale)
    fun update(sale: Sale)
    fun findById(organizationId: String, saleId: String): Sale?
    fun search(query: SaleSearchQuery): List<Sale>
}

data class SaleSearchQuery(
    val organizationId: String,
    val statuses: Set<SaleOperationalStatus> = emptySet(),
    val customerId: String? = null,
    val activityId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

interface OperationalReservationRepository {
    fun create(reservation: Reservation)
    fun update(reservation: Reservation)
    fun findById(organizationId: String, reservationId: String): Reservation?
    fun search(query: ReservationSearchQuery): List<Reservation>
}

data class ReservationSearchQuery(
    val organizationId: String,
    val statuses: Set<ReservationStatus> = emptySet(),
    val customerId: String? = null,
    val activityId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)
