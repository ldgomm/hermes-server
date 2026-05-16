package com.hermes.domain.percentage

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PercentageTest {

    @Test
    fun `creates percentage with four decimal places`() {
        val percentage = Percentage.of("15")

        assertEquals("15.0000", percentage.value.toPlainString())
    }

    @Test
    fun `converts percentage to fraction`() {
        val percentage = Percentage.of("15")

        assertEquals("0.15000000", percentage.asFraction().toPlainString())
    }

    @Test
    fun `applies percentage to raw amount preserving six decimal places`() {
        val percentage = Percentage.of("15")
        val result = percentage.applyToAmount(Money.of("20.87").amount)

        assertEquals("3.130500", result.toPlainString())
    }

    @Test
    fun `applies percentage to money and rounds to money scale`() {
        val percentage = Percentage.of("15")
        val result = percentage.applyToMoney(Money.of("20.87"))

        assertEquals("3.13", result.amount.toPlainString())
        assertEquals("USD", result.currency.value)
    }

    @Test
    fun `rejects negative percentage`() {
        assertFailsWith<DomainRuleViolation> {
            Percentage.of("-1")
        }
    }

    @Test
    fun `rejects percentage greater than one hundred`() {
        assertFailsWith<DomainRuleViolation> {
            Percentage.of("100.01")
        }
    }

    @Test
    fun `zero percentage returns zero amount`() {
        val result = Percentage.zero().applyToAmount(Money.of("24.00").amount)

        assertEquals("0.000000", result.toPlainString())
    }
}
