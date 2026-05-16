package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal

object CatalogAttributeRules {
    fun validate(definitions: List<CatalogAttributeDefinition>, values: List<CatalogAttributeValue>) {
        val definitionsByKey = definitions.associateBy { it.key }
        val valuesByKey = values.associateBy { it.key }

        definitions.filter { it.required }.forEach { required ->
            if (required.key !in valuesByKey) {
                throw DomainRuleViolation("Required catalog attribute ${required.key} is missing.")
            }
        }

        values.forEach { value ->
            val definition = definitionsByKey[value.key]
                ?: throw DomainRuleViolation("Unknown catalog attribute ${value.key}.")
            validateValue(definition, value.value)
        }
    }

    private fun validateValue(definition: CatalogAttributeDefinition, raw: String) {
        when (definition.type) {
            CatalogAttributeType.TEXT -> Unit
            CatalogAttributeType.DECIMAL -> raw.toBigDecimalOrNull()
                ?: throw DomainRuleViolation("Catalog attribute ${definition.key} must be decimal.")
            CatalogAttributeType.INTEGER -> raw.toIntOrNull()
                ?: throw DomainRuleViolation("Catalog attribute ${definition.key} must be integer.")
            CatalogAttributeType.BOOLEAN -> if (raw.lowercase() !in setOf("true", "false")) {
                throw DomainRuleViolation("Catalog attribute ${definition.key} must be boolean.")
            }
            CatalogAttributeType.ENUM -> if (raw !in definition.allowedValues) {
                throw DomainRuleViolation("Catalog attribute ${definition.key} contains an invalid enum value.")
            }
        }
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}
