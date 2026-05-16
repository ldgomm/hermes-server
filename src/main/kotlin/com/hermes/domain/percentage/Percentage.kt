package com.hermes.domain.percentage

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class Percentage private constructor(
    val value: BigDecimal
) {

    init {
        if (value < BigDecimal.ZERO) {
            throw DomainRuleViolation("Percentage cannot be negative.")
        }

        if (value > BigDecimal("100.0000")) {
            throw DomainRuleViolation("Percentage cannot be greater than 100.")
        }

        if (value.scale() != PERCENTAGE_SCALE) {
            throw DomainRuleViolation("Percentage must use exactly $PERCENTAGE_SCALE decimal places.")
        }
    }

    fun asFraction(scale: Int = FRACTION_SCALE): BigDecimal {
        return value.divide(BigDecimal("100"), scale, RoundingMode.HALF_UP)
    }

    fun applyToAmount(amount: BigDecimal): BigDecimal {
        if (amount < BigDecimal.ZERO) {
            throw DomainRuleViolation("Cannot apply percentage to a negative amount.")
        }

        return amount.multiply(asFraction()).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)
    }

    fun applyToMoney(money: Money): Money {
        return Money.of(applyToAmount(money.amount), money.currency)
    }

    companion object {
        private const val PERCENTAGE_SCALE = 4
        private const val FRACTION_SCALE = 8
        private const val AMOUNT_SCALE = 6

        fun zero(): Percentage {
            return of("0")
        }

        fun of(value: String): Percentage {
            return of(BigDecimal(value))
        }

        fun of(value: BigDecimal): Percentage {
            return Percentage(value.setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP))
        }
    }
}
