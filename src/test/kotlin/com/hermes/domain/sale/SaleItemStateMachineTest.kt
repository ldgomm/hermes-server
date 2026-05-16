package com.hermes.domain.sale

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SaleItemStateMachineTest {
    @Test
    fun `allows pending item to become in progress or ready`() {
        assertTrue(SaleItemStateMachine.canTransition(SaleItemStatus.PENDING, SaleItemStatus.IN_PROGRESS))
        assertTrue(SaleItemStateMachine.canTransition(SaleItemStatus.PENDING, SaleItemStatus.READY))
    }

    @Test
    fun `allows item to move through preparation and delivery`() {
        SaleItemStateMachine.assertCanTransition(SaleItemStatus.PENDING, SaleItemStatus.IN_PROGRESS)
        SaleItemStateMachine.assertCanTransition(SaleItemStatus.IN_PROGRESS, SaleItemStatus.READY)
        SaleItemStateMachine.assertCanTransition(SaleItemStatus.READY, SaleItemStatus.DELIVERED)
    }

    @Test
    fun `rejects delivered item returning to ready`() {
        assertFalse(SaleItemStateMachine.canTransition(SaleItemStatus.DELIVERED, SaleItemStatus.READY))
        assertFailsWith<DomainRuleViolation> {
            SaleItemStateMachine.assertCanTransition(SaleItemStatus.DELIVERED, SaleItemStatus.READY)
        }
    }

    @Test
    fun `rejects canceled item becoming delivered`() {
        assertFailsWith<DomainRuleViolation> {
            SaleItemStateMachine.assertCanTransition(SaleItemStatus.CANCELED, SaleItemStatus.DELIVERED)
        }
    }

    @Test
    fun `allows same item state as idempotent`() {
        SaleItemStateMachine.assertCanTransition(SaleItemStatus.READY, SaleItemStatus.READY)
    }
}
