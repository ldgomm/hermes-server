package com.hermes.domain.sale

import com.hermes.domain.sale.SaleOperationalStateMachine
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SaleStateMachineTest {

    @Test
    fun `allows draft sale to become pending`() {
        assertTrue(
            SaleOperationalStateMachine.canTransition(
                from = SaleOperationalStatus.DRAFT,
                to = SaleOperationalStatus.PENDING
            )
        )
    }

    @Test
    fun `allows draft sale to become confirmed`() {
        assertTrue(
            SaleOperationalStateMachine.canTransition(
                from = SaleOperationalStatus.DRAFT,
                to = SaleOperationalStatus.CONFIRMED
            )
        )
    }

    @Test
    fun `allows confirmed sale to move through operational flow`() {
        SaleOperationalStateMachine.assertCanTransition(
            from = SaleOperationalStatus.CONFIRMED,
            to = SaleOperationalStatus.IN_PROGRESS
        )

        SaleOperationalStateMachine.assertCanTransition(
            from = SaleOperationalStatus.IN_PROGRESS,
            to = SaleOperationalStatus.READY
        )

        SaleOperationalStateMachine.assertCanTransition(
            from = SaleOperationalStatus.READY,
            to = SaleOperationalStatus.DELIVERED
        )

        SaleOperationalStateMachine.assertCanTransition(
            from = SaleOperationalStatus.DELIVERED,
            to = SaleOperationalStatus.CLOSED
        )
    }

    @Test
    fun `rejects draft sale going directly to closed`() {
        assertFalse(
            SaleOperationalStateMachine.canTransition(
                from = SaleOperationalStatus.DRAFT,
                to = SaleOperationalStatus.CLOSED
            )
        )

        assertFailsWith<DomainRuleViolation> {
            SaleOperationalStateMachine.assertCanTransition(
                from = SaleOperationalStatus.DRAFT,
                to = SaleOperationalStatus.CLOSED
            )
        }
    }

    @Test
    fun `rejects canceled sale becoming confirmed`() {
        assertFailsWith<DomainRuleViolation> {
            SaleOperationalStateMachine.assertCanTransition(
                from = SaleOperationalStatus.CANCELED,
                to = SaleOperationalStatus.CONFIRMED
            )
        }
    }

    @Test
    fun `rejects closed sale becoming canceled`() {
        assertFailsWith<DomainRuleViolation> {
            SaleOperationalStateMachine.assertCanTransition(
                from = SaleOperationalStatus.CLOSED,
                to = SaleOperationalStatus.CANCELED
            )
        }
    }

    @Test
    fun `allows same state transition as idempotent`() {
        SaleOperationalStateMachine.assertCanTransition(
            from = SaleOperationalStatus.CONFIRMED,
            to = SaleOperationalStatus.CONFIRMED
        )
    }
}
