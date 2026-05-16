package com.hermes.domain.cash

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CashSessionEntityTest {

    private val now = Instant.parse("2026-05-15T20:00:00Z")

    @Test
    fun `opens cash session`() {
        val session = CashSession.open(
            id = "cash_1",
            organizationId = "org_1",
            openedBy = "usr_1",
            openingBalance = Money.of("20.00"),
            openedAt = now
        )

        assertEquals(CashSessionStatus.OPEN, session.status)
        assertEquals("20.00", session.expectedCashAmount.amount.toPlainString())
    }

    @Test
    fun `records incoming cash movement`() {
        val session = CashSession.open(
            id = "cash_1",
            organizationId = "org_1",
            openedBy = "usr_1",
            openingBalance = Money.of("20.00"),
            openedAt = now
        )

        val movement = CashMovement.create(
            id = "cmov_1",
            cashSessionId = "cash_1",
            organizationId = "org_1",
            type = CashMovementType.SALE_PAYMENT,
            direction = CashMovementDirection.IN,
            amount = Money.of("24.00"),
            occurredAt = now,
            referenceId = "sale_1"
        )

        val updated = session.recordMovement(movement)

        assertEquals("44.00", updated.expectedCashAmount.amount.toPlainString())
    }

    @Test
    fun `records outgoing cash movement`() {
        val session = CashSession.open(
            id = "cash_1",
            organizationId = "org_1",
            openedBy = "usr_1",
            openingBalance = Money.of("20.00"),
            openedAt = now
        )

        val movement = CashMovement.create(
            id = "cmov_1",
            cashSessionId = "cash_1",
            organizationId = "org_1",
            type = CashMovementType.WITHDRAWAL,
            direction = CashMovementDirection.OUT,
            amount = Money.of("5.00"),
            occurredAt = now
        )

        val updated = session.recordMovement(movement)

        assertEquals("15.00", updated.expectedCashAmount.amount.toPlainString())
    }

    @Test
    fun `closes cash session through closing state`() {
        val session = CashSession.open(
            id = "cash_1",
            organizationId = "org_1",
            openedBy = "usr_1",
            openingBalance = Money.of("20.00"),
            openedAt = now
        )

        val closed = session
            .startClosing(now)
            .close(now)

        assertEquals(CashSessionStatus.CLOSED, closed.status)
    }

    @Test
    fun `rejects movement after cash session is closed`() {
        val session = CashSession.open(
            id = "cash_1",
            organizationId = "org_1",
            openedBy = "usr_1",
            openingBalance = Money.of("20.00"),
            openedAt = now
        ).startClosing(now).close(now)

        val movement = CashMovement.create(
            id = "cmov_1",
            cashSessionId = "cash_1",
            organizationId = "org_1",
            type = CashMovementType.SALE_PAYMENT,
            direction = CashMovementDirection.IN,
            amount = Money.of("24.00"),
            occurredAt = now
        )

        assertFailsWith<DomainRuleViolation> {
            session.recordMovement(movement)
        }
    }
}
