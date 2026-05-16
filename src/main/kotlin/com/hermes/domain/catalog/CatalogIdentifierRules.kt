package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class CatalogItemIdentity(
    val itemId: String,
    val organizationId: String? = null,
    val identifiers: List<CatalogIdentifier>,
)

object CatalogIdentifierRules {
    fun validate(identifier: CatalogIdentifier) {
        when (identifier.type) {
            CatalogIdentifierType.EAN_13 -> requireDigits(identifier, 13)
            CatalogIdentifierType.EAN_8 -> requireDigits(identifier, 8)
            CatalogIdentifierType.UPC_A -> requireDigits(identifier, 12)
            CatalogIdentifierType.BARCODE,
            CatalogIdentifierType.GTIN -> {
                if (!identifier.normalizedValue.all(Char::isDigit)) {
                    throw DomainRuleViolation("Barcode identifiers must contain only digits after normalization.")
                }
            }
            else -> Unit
        }

        if (identifier.status in setOf(CatalogIdentifierStatus.DEPRECATED, CatalogIdentifierStatus.REJECTED) && identifier.isPrimary) {
            throw DomainRuleViolation("Deprecated or rejected identifiers cannot be primary.")
        }
    }

    fun validateNoConflictingGlobalVerifiedIdentifiers(items: List<CatalogItemIdentity>) {
        val seen = mutableMapOf<Pair<CatalogIdentifierType, String>, String>()
        items.forEach { item ->
            item.identifiers
                .filter { it.scope == CatalogIdentifierScope.GLOBAL && it.status == CatalogIdentifierStatus.VERIFIED }
                .forEach { identifier ->
                    val key = identifier.type to identifier.normalizedValue
                    val existing = seen[key]
                    if (existing != null && existing != item.itemId) {
                        throw DomainRuleViolation("Verified global identifier ${identifier.normalizedValue} is assigned to multiple items.")
                    }
                    seen[key] = item.itemId
                }
        }
    }

    fun validateLocalSkuUniqueness(items: List<CatalogItemIdentity>) {
        val seen = mutableMapOf<Pair<String, String>, String>()
        items.forEach { item ->
            val organizationId = item.organizationId ?: return@forEach
            item.identifiers
                .filter { it.type == CatalogIdentifierType.SKU_LOCAL && it.status in setOf(CatalogIdentifierStatus.ACTIVE, CatalogIdentifierStatus.VERIFIED) }
                .forEach { identifier ->
                    val key = organizationId to identifier.normalizedValue
                    val existing = seen[key]
                    if (existing != null && existing != item.itemId) {
                        throw DomainRuleViolation("Local SKU ${identifier.normalizedValue} must be unique inside the organization.")
                    }
                    seen[key] = item.itemId
                }
        }
    }

    private fun requireDigits(identifier: CatalogIdentifier, count: Int) {
        if (!Regex("^\\d{$count}$").matches(identifier.normalizedValue)) {
            throw DomainRuleViolation("${identifier.type} must contain exactly $count digits.")
        }
    }
}
