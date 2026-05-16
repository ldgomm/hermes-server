package com.hermes.domain.catalog

import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal

data class UnitConversionRule(
    val fromUnitCode: String,
    val toUnitCode: String,
    val factor: BigDecimal,
    val active: Boolean = true,
) {
    init {
        if (fromUnitCode.isBlank()) throw DomainRuleViolation("Conversion from unit cannot be blank.")
        if (toUnitCode.isBlank()) throw DomainRuleViolation("Conversion to unit cannot be blank.")
        if (fromUnitCode == toUnitCode) throw DomainRuleViolation("Conversion units must be different.")
        if (factor <= BigDecimal.ZERO) throw DomainRuleViolation("Conversion factor must be greater than zero.")
    }
}

object UnitConversionRules {
    fun convert(quantity: Quantity, rule: UnitConversionRule, targetAllowsDecimal: Boolean = true): Quantity {
        if (!rule.active) throw DomainRuleViolation("Inactive unit conversion cannot be used.")
        if (quantity.unitCode != rule.fromUnitCode) {
            throw DomainRuleViolation("Quantity unit ${quantity.unitCode} does not match conversion from unit ${rule.fromUnitCode}.")
        }
        return Quantity.of(quantity.value.multiply(rule.factor), rule.toUnitCode, targetAllowsDecimal)
    }

    fun validateNoDuplicateConversions(rules: List<UnitConversionRule>) {
        val duplicated = rules.groupBy { it.fromUnitCode to it.toUnitCode }.filterValues { it.size > 1 }
        if (duplicated.isNotEmpty()) {
            throw DomainRuleViolation("Duplicate unit conversion rules are not allowed.")
        }
    }
}
