package com.hermes.backend.com.hermes.domain.money

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneyTest {

    @Test
    fun `creates USD money with two decimal places`() {
        val money = Money.of("10")

        assertEquals("10.00", money.amount.toPlainString())
        assertEquals("USD", money.currency.value)
    }

    @Test
    fun `rounds money using half up`() {
        val money = Money.of("10.235")

        assertEquals("10.24", money.amount.toPlainString())
    }

    @Test
    fun `adds money with same currency`() {
        val result = Money.of("10.50") + Money.of("2.25")

        assertEquals("12.75", result.amount.toPlainString())
    }

    @Test
    fun `rejects subtraction that produces negative money`() {
        assertFailsWith<DomainRuleViolation> {
            Money.of("5.00") - Money.of("6.00")
        }
    }

    @Test
    fun `rejects operations with different currencies`() {
        assertFailsWith<DomainRuleViolation> {
            Money.of("5.00", "USD") + Money.of("5.00", "EUR")
        }
    }

    @Test
    fun `rejects negative money`() {
        assertFailsWith<DomainRuleViolation> {
            Money.of("-1.00")
        }
    }
}