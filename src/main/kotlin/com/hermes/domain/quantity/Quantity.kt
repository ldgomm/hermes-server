package com.hermes.domain.quantity

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class Quantity private constructor(
    val value: BigDecimal,
    val unitCode: String,
    val allowsDecimal: Boolean
) {

    init {
        if (unitCode.isBlank()) {
            throw DomainRuleViolation("Unit code cannot be blank.")
        }

        if (value <= BigDecimal.ZERO) {
            throw DomainRuleViolation("Quantity must be greater than zero.")
        }

        if (!allowsDecimal && value.stripTrailingZeros().scale() > 0) {
            throw DomainRuleViolation("This quantity does not allow decimal values.")
        }

        if (value.scale() > QUANTITY_SCALE) {
            throw DomainRuleViolation("Quantity cannot have more than $QUANTITY_SCALE decimal places.")
        }
    }

    operator fun plus(other: Quantity): Quantity {
        requireSameUnit(other)
        return of(value.add(other.value), unitCode, allowsDecimal || other.allowsDecimal)
    }

    operator fun minus(other: Quantity): Quantity {
        requireSameUnit(other)

        val result = value.subtract(other.value)

        if (result <= BigDecimal.ZERO) {
            throw DomainRuleViolation("Quantity subtraction must keep a positive result.")
        }

        return of(result, unitCode, allowsDecimal || other.allowsDecimal)
    }

    private fun requireSameUnit(other: Quantity) {
        if (unitCode != other.unitCode) {
            throw DomainRuleViolation("Cannot operate quantities with different unit codes.")
        }
    }

    companion object {
        private const val QUANTITY_SCALE = 6

        fun units(value: Int): Quantity {
            return of(BigDecimal(value), "unit", allowsDecimal = false)
        }

        fun of(value: String, unitCode: String, allowsDecimal: Boolean): Quantity {
            return of(BigDecimal(value), unitCode, allowsDecimal)
        }

        fun of(value: BigDecimal, unitCode: String, allowsDecimal: Boolean): Quantity {
            return Quantity(
                value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP),
                unitCode,
                allowsDecimal
            )
        }
    }
}