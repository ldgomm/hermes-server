package com.hermes.backend.com.hermes.domain.quantity

import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuantityTest {

    @Test
    fun `creates unit quantity`() {
        val quantity = Quantity.units(2)

        assertEquals("2.000000", quantity.value.toPlainString())
        assertEquals("unit", quantity.unitCode)
        assertEquals(false, quantity.allowsDecimal)
    }

    @Test
    fun `creates decimal quantity when allowed`() {
        val quantity = Quantity.of("1.5", "kg", allowsDecimal = true)

        assertEquals("1.500000", quantity.value.toPlainString())
        assertEquals("kg", quantity.unitCode)
    }

    @Test
    fun `rejects decimal quantity when not allowed`() {
        assertFailsWith<DomainRuleViolation> {
            Quantity.of("1.5", "unit", allowsDecimal = false)
        }
    }

    @Test
    fun `adds quantities with same unit`() {
        val result = Quantity.of("1.5", "kg", true) + Quantity.of("2.25", "kg", true)

        assertEquals("3.750000", result.value.toPlainString())
    }

    @Test
    fun `rejects operation with different units`() {
        assertFailsWith<DomainRuleViolation> {
            Quantity.of("1", "kg", true) + Quantity.of("1", "unit", false)
        }
    }

    @Test
    fun `rejects zero quantity`() {
        assertFailsWith<DomainRuleViolation> {
            Quantity.of("0", "unit", false)
        }
    }
}