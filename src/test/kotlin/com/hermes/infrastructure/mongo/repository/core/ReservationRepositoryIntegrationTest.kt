package com.hermes.infrastructure.mongo.repository.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class ReservationRepositoryIntegrationTest {
    @Test
    fun `service order repository queries by sale and status`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_service_order_repository") { database ->
            val serviceOrders = ServiceOrderRepository(database)

            serviceOrders.insert(
                RepositoryTestSupport.serviceOrder(
                    id = "svc_phase43_confirmed",
                    status = "confirmed",
                )
            )
            serviceOrders.insert(
                RepositoryTestSupport.serviceOrder(
                    id = "svc_phase43_pending",
                    saleId = "sale_phase43_002",
                    status = "pending",
                )
            )

            val bySale = serviceOrders.findBySale(
                RepositoryTestSupport.ORGANIZATION_ID,
                RepositoryTestSupport.SALE_ID,
            )

            assertNotNull(bySale)
            assertEquals("svc_phase43_confirmed", bySale?.getString("_id"))

            assertEquals(
                listOf("svc_phase43_pending"),
                serviceOrders.findByStatus(
                    RepositoryTestSupport.ORGANIZATION_ID,
                    "pending",
                ).map { it.getString("_id") }
            )
        }
    }

    @Test
    fun `reservation repository queries by sale date range and upcoming customer reservations`() {
        RepositoryTestSupport.withMigratedDatabase("phase_4_3_reservation_repository") { database ->
            val reservations = ReservationRepository(database)

            reservations.insert(
                RepositoryTestSupport.reservation(
                    id = "res_phase43_early",
                    saleId = RepositoryTestSupport.SALE_ID,
                    startAt = Instant.parse("2026-05-16T09:00:00Z"),
                )
            )
            reservations.insert(
                RepositoryTestSupport.reservation(
                    id = "res_phase43_late",
                    saleId = "sale_phase43_002",
                    startAt = Instant.parse("2026-05-16T14:00:00Z"),
                )
            )
            reservations.insert(
                RepositoryTestSupport.reservation(
                    id = "res_phase43_next_day",
                    saleId = "sale_phase43_003",
                    startAt = Instant.parse("2026-05-17T10:00:00Z"),
                )
            )

            val bySale = reservations.findBySale(
                RepositoryTestSupport.ORGANIZATION_ID,
                RepositoryTestSupport.SALE_ID,
            )

            assertNotNull(bySale)
            assertEquals("res_phase43_early", bySale?.getString("_id"))

            assertEquals(
                listOf("res_phase43_early", "res_phase43_late"),
                reservations.findByDateRange(
                    organizationId = RepositoryTestSupport.ORGANIZATION_ID,
                    from = Date.from(Instant.parse("2026-05-16T00:00:00Z")),
                    to = Date.from(Instant.parse("2026-05-16T23:59:59Z")),
                ).map { it.getString("_id") }
            )

            assertEquals(
                listOf("res_phase43_late", "res_phase43_next_day"),
                reservations.findUpcomingByCustomer(
                    organizationId = RepositoryTestSupport.ORGANIZATION_ID,
                    customerId = RepositoryTestSupport.CUSTOMER_ID,
                    from = Date.from(Instant.parse("2026-05-16T12:00:00Z")),
                ).map { it.getString("_id") }
            )
        }
    }
}
