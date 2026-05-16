package com.hermes.domain.catalog

import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnitConversionRulesTest {
    @Test
    fun `converts purchase box to units`() {
        val converted = UnitConversionRules.convert(
            quantity = Quantity.units(2),
            rule = UnitConversionRule("unit", "box", BigDecimal("0.083333")),
            targetAllowsDecimal = true,
        )

        assertEquals("box", converted.unitCode)
        assertEquals("0.166666", converted.value.toPlainString())
    }

    @Test
    fun `rejects inactive conversion`() {
        assertFailsWith<DomainRuleViolation> {
            UnitConversionRules.convert(
                quantity = Quantity.units(1),
                rule = UnitConversionRule("unit", "box", BigDecimal("1.00"), active = false),
            )
        }
    }

    @Test
    fun `rejects conversion for incompatible unit`() {
        assertFailsWith<DomainRuleViolation> {
            UnitConversionRules.convert(
                quantity = Quantity.of("1.00", "kg", allowsDecimal = true),
                rule = UnitConversionRule("unit", "box", BigDecimal("1.00")),
            )
        }
    }

    @Test
    fun `rejects duplicated conversion rules`() {
        assertFailsWith<DomainRuleViolation> {
            UnitConversionRules.validateNoDuplicateConversions(
                listOf(
                    UnitConversionRule("box", "unit", BigDecimal("12")),
                    UnitConversionRule("box", "unit", BigDecimal("24")),
                ),
            )
        }
    }
}
