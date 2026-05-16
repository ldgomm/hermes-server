package com.hermes.domain.money

import com.hermes.domain.shared.DomainRuleViolation

@JvmInline
value class CurrencyCode(val value: String) {

    init {
        if (!Regex("^[A-Z]{3}$").matches(value)) {
            throw DomainRuleViolation("Currency code must contain exactly 3 uppercase letters.")
        }
    }

    companion object {
        val USD = CurrencyCode("USD")
    }
}
