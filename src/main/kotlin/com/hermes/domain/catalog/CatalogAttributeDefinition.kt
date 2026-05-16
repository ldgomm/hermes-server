package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class CatalogAttributeDefinition(
    val key: String,
    val label: String,
    val type: CatalogAttributeType,
    val required: Boolean = false,
    val filterable: Boolean = true,
    val allowedValues: Set<String> = emptySet(),
) {
    init {
        if (key.isBlank()) throw DomainRuleViolation("Catalog attribute key cannot be blank.")
        if (label.isBlank()) throw DomainRuleViolation("Catalog attribute label cannot be blank.")
        if (type == CatalogAttributeType.ENUM && allowedValues.isEmpty()) {
            throw DomainRuleViolation("Enum catalog attributes require allowed values.")
        }
    }
}

data class CatalogAttributeValue(
    val key: String,
    val value: String,
) {
    init {
        if (key.isBlank()) throw DomainRuleViolation("Catalog attribute value key cannot be blank.")
        if (value.isBlank()) throw DomainRuleViolation("Catalog attribute value cannot be blank.")
    }
}
