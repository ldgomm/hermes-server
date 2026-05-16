package com.hermes.domain.money

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class Money private constructor(
    val amount: BigDecimal,
    val currency: CurrencyCode
) : Comparable<Money> {

    init {
        if (amount.scale() > MONEY_SCALE) {
            throw DomainRuleViolation("Money amount cannot have more than $MONEY_SCALE decimal places.")
        }

        if (amount < BigDecimal.ZERO) {
            throw DomainRuleViolation("Money amount cannot be negative.")
        }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount.add(other.amount), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)

        val result = amount.subtract(other.amount)

        if (result < BigDecimal.ZERO) {
            throw DomainRuleViolation("Money subtraction cannot produce a negative amount.")
        }

        return of(result, currency)
    }

    fun multiply(multiplier: BigDecimal): Money {
        if (multiplier < BigDecimal.ZERO) {
            throw DomainRuleViolation("Money multiplier cannot be negative.")
        }

        return of(amount.multiply(multiplier), currency)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    private fun requireSameCurrency(other: Money) {
        if (currency != other.currency) {
            throw DomainRuleViolation("Cannot operate money with different currencies.")
        }
    }

    companion object {
        private const val MONEY_SCALE = 2

        fun zero(currency: CurrencyCode = CurrencyCode.USD): Money {
            return Money(BigDecimal.ZERO.setScale(MONEY_SCALE), currency)
        }

        fun of(amount: String, currency: String = "USD"): Money {
            return of(BigDecimal(amount), CurrencyCode(currency))
        }

        fun of(amount: BigDecimal, currency: CurrencyCode = CurrencyCode.USD): Money {
            return Money(
                amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                currency
            )
        }
    }
}