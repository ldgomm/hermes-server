package com.hermes.domain.reservation

import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReservationTest {
    private val now: Instant = Instant.parse("2026-05-18T12:00:00Z")

    @Test
    fun `creates scheduled reservation`() {
        val reservation = Reservation.schedule(
            id = "res_1",
            organizationId = "org_1",
            branchId = "branch_1",
            activityId = "act_1",
            customerId = "cust_1",
            customerSnapshot = CustomerSnapshot.finalConsumer(),
            resourceId = "resource_1",
            startAt = now.plusSeconds(3600),
            endAt = now.plusSeconds(7200),
            partySize = 2,
            notes = "Mesa exterior",
            createdAt = now,
        )

        assertEquals(ReservationStatus.SCHEDULED, reservation.status)
        assertEquals(2, reservation.partySize)
    }

    @Test
    fun `rejects invalid reservation time range`() {
        assertFailsWith<DomainRuleViolation> {
            Reservation.schedule(
                id = "res_1",
                organizationId = "org_1",
                branchId = "branch_1",
                activityId = "act_1",
                customerId = null,
                customerSnapshot = CustomerSnapshot.finalConsumer(),
                resourceId = null,
                startAt = now,
                endAt = now,
                partySize = 1,
                notes = null,
                createdAt = now,
            )
        }
    }
}
