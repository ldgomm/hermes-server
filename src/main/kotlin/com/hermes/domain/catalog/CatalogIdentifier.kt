package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class CatalogIdentifier(
    val type: CatalogIdentifierType,
    val value: String,
    val normalizedValue: String,
    val scope: CatalogIdentifierScope,
    val status: CatalogIdentifierStatus,
    val source: CatalogIdentifierSource,
    val isPrimary: Boolean = false,
) {
    init {
        if (value.isBlank()) throw DomainRuleViolation("Catalog identifier value cannot be blank.")
        if (normalizedValue.isBlank()) throw DomainRuleViolation("Catalog identifier normalized value cannot be blank.")
    }

    companion object {
        fun create(
            type: CatalogIdentifierType,
            value: String,
            scope: CatalogIdentifierScope,
            source: CatalogIdentifierSource,
            status: CatalogIdentifierStatus = CatalogIdentifierStatus.ACTIVE,
            isPrimary: Boolean = false,
        ): CatalogIdentifier {
            return CatalogIdentifier(
                type = type,
                value = value.trim(),
                normalizedValue = normalize(type, value),
                scope = scope,
                source = source,
                status = status,
                isPrimary = isPrimary,
            )
        }

        fun normalize(type: CatalogIdentifierType, raw: String): String {
            val trimmed = raw.trim()
            return when (type) {
                CatalogIdentifierType.BARCODE,
                CatalogIdentifierType.GTIN,
                CatalogIdentifierType.EAN_8,
                CatalogIdentifierType.EAN_13,
                CatalogIdentifierType.UPC_A,
                CatalogIdentifierType.ISBN -> trimmed.filter(Char::isDigit)
                else -> trimmed.replace(" ", "").replace("-", "").uppercase()
            }
        }
    }
}
