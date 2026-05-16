package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CashSessionRulesTest {

    @Test
    fun `allows recording movement when cash session is open`() {
        CashSessionRules.assertCanRecordMovement(CashSessionStatus.OPEN)
    }

    @Test
    fun `rejects recording movement when cash session is closing`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertCanRecordMovement(CashSessionStatus.CLOSING)
        }
    }

    @Test
    fun `rejects recording movement when cash session is closed`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertCanRecordMovement(CashSessionStatus.CLOSED)
        }
    }

    @Test
    fun `rejects recording movement when cash session is canceled`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertCanRecordMovement(CashSessionStatus.CANCELED)
        }
    }

    @Test
    fun `allows sale payment as incoming movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.SALE_PAYMENT,
            direction = CashMovementDirection.IN,
            amount = Money.of("24.00")
        )
    }

    @Test
    fun `rejects sale payment as outgoing movement`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertValidMovement(
                type = CashMovementType.SALE_PAYMENT,
                direction = CashMovementDirection.OUT,
                amount = Money.of("24.00")
            )
        }
    }

    @Test
    fun `allows manual expense as outgoing movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.MANUAL_EXPENSE,
            direction = CashMovementDirection.OUT,
            amount = Money.of("5.00")
        )
    }

    @Test
    fun `rejects manual expense as incoming movement`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertValidMovement(
                type = CashMovementType.MANUAL_EXPENSE,
                direction = CashMovementDirection.IN,
                amount = Money.of("5.00")
            )
        }
    }

    @Test
    fun `allows withdrawal as outgoing movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.WITHDRAWAL,
            direction = CashMovementDirection.OUT,
            amount = Money.of("10.00")
        )
    }

    @Test
    fun `allows refund as outgoing movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.REFUND,
            direction = CashMovementDirection.OUT,
            amount = Money.of("3.00")
        )
    }

    @Test
    fun `allows adjustment as incoming movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.ADJUSTMENT,
            direction = CashMovementDirection.IN,
            amount = Money.of("1.00")
        )
    }

    @Test
    fun `allows adjustment as outgoing movement`() {
        CashSessionRules.assertValidMovement(
            type = CashMovementType.ADJUSTMENT,
            direction = CashMovementDirection.OUT,
            amount = Money.of("1.00")
        )
    }

    @Test
    fun `rejects adjustment as neutral movement`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertValidMovement(
                type = CashMovementType.ADJUSTMENT,
                direction = CashMovementDirection.NEUTRAL,
                amount = Money.of("1.00")
            )
        }
    }

    @Test
    fun `rejects zero cash movement amount`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertValidMovement(
                type = CashMovementType.SALE_PAYMENT,
                direction = CashMovementDirection.IN,
                amount = Money.zero()
            )
        }
    }

    @Test
    fun `rejects closing an already closed cash session`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertCanClose(CashSessionStatus.CLOSED)
        }
    }

    @Test
    fun `rejects canceling a closed cash session`() {
        assertFailsWith<DomainRuleViolation> {
            CashSessionRules.assertCanCancel(CashSessionStatus.CLOSED)
        }
    }
}
