package com.hermes.domain.cash

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CashSessionStateMachineTest {

    @Test
    fun `allows open cash session to start closing`() {
        assertTrue(
            CashSessionStateMachine.canTransition(
                from = CashSessionStatus.OPEN,
                to = CashSessionStatus.CLOSING
            )
        )
    }

    @Test
    fun `allows closing cash session to close`() {
        CashSessionStateMachine.assertCanTransition(
            from = CashSessionStatus.CLOSING,
            to = CashSessionStatus.CLOSED
        )
    }

    @Test
    fun `allows closing cash session to reopen`() {
        CashSessionStateMachine.assertCanTransition(
            from = CashSessionStatus.CLOSING,
            to = CashSessionStatus.OPEN
        )
    }

    @Test
    fun `rejects open cash session going directly to closed`() {
        assertFalse(
            CashSessionStateMachine.canTransition(
                from = CashSessionStatus.OPEN,
                to = CashSessionStatus.CLOSED
            )
        )

        assertFailsWith<DomainRuleViolation> {
            CashSessionStateMachine.assertCanTransition(
                from = CashSessionStatus.OPEN,
                to = CashSessionStatus.CLOSED
            )
        }
    }

    @Test
    fun `rejects closed cash session reopening`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionStateMachine.assertCanTransition(
                from = CashSessionStatus.CLOSED,
                to = CashSessionStatus.OPEN
            )
        }
    }

    @Test
    fun `rejects canceled cash session reopening`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionStateMachine.assertCanTransition(
                from = CashSessionStatus.CANCELED,
                to = CashSessionStatus.OPEN
            )
        }
    }

    @Test
    fun `allows same state transition as idempotent`() {
        CashSessionStateMachine.assertCanTransition(
            from = CashSessionStatus.OPEN,
            to = CashSessionStatus.OPEN
        )
    }
}
