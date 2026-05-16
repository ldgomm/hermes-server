package com.hermes.domain.reservation

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReservationStateMachineTest {
    @Test
    fun `allows draft reservation to be scheduled`() {
        assertTrue(ReservationStateMachine.canTransition(ReservationStatus.DRAFT, ReservationStatus.SCHEDULED))
    }

    @Test
    fun `allows reservation operational flow`() {
        ReservationStateMachine.assertCanTransition(ReservationStatus.SCHEDULED, ReservationStatus.CONFIRMED)
        ReservationStateMachine.assertCanTransition(ReservationStatus.CONFIRMED, ReservationStatus.IN_PROGRESS)
        ReservationStateMachine.assertCanTransition(ReservationStatus.IN_PROGRESS, ReservationStatus.COMPLETED)
    }

    @Test
    fun `allows scheduled reservation to be rescheduled`() {
        ReservationStateMachine.assertCanTransition(ReservationStatus.SCHEDULED, ReservationStatus.RESCHEDULED)
        ReservationStateMachine.assertCanTransition(ReservationStatus.RESCHEDULED, ReservationStatus.SCHEDULED)
    }

    @Test
    fun `rejects completed reservation becoming canceled`() {
        assertFalse(ReservationStateMachine.canTransition(ReservationStatus.COMPLETED, ReservationStatus.CANCELED))
        assertFailsWith<DomainRuleViolation> {
            ReservationStateMachine.assertCanTransition(ReservationStatus.COMPLETED, ReservationStatus.CANCELED)
        }
    }

    @Test
    fun `rejects no show reservation becoming confirmed`() {
        assertFailsWith<DomainRuleViolation> {
            ReservationStateMachine.assertCanTransition(ReservationStatus.NO_SHOW, ReservationStatus.CONFIRMED)
        }
    }
}
