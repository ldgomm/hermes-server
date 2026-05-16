package com.hermes.domain.catalog

object BarcodeSearch {
    private val searchableTypes = setOf(
        CatalogIdentifierType.BARCODE,
        CatalogIdentifierType.GTIN,
        CatalogIdentifierType.EAN_8,
        CatalogIdentifierType.EAN_13,
        CatalogIdentifierType.UPC_A,
    )

    fun findByBarcode(query: String, items: List<OrganizationCatalogItem>): OrganizationCatalogItem? {
        val normalized = query.filter(Char::isDigit)
        if (normalized.isBlank()) return null

        return items.firstOrNull { item ->
            item.status != CatalogItemStatus.ARCHIVED && item.identifiers.any { identifier ->
                identifier.type in searchableTypes &&
                    identifier.status in setOf(CatalogIdentifierStatus.ACTIVE, CatalogIdentifierStatus.VERIFIED) &&
                    identifier.normalizedValue == normalized
            }
        }
    }

    fun searchByCodeOrText(query: String, items: List<OrganizationCatalogItem>): List<OrganizationCatalogItem> {
        val normalizedCode = query.filter(Char::isDigit)
        val normalizedText = query.trim().lowercase()
        if (normalizedCode.isBlank() && normalizedText.isBlank()) return emptyList()

        return items.filter { item ->
            item.status != CatalogItemStatus.ARCHIVED && (
                item.identifiers.any { identifier ->
                    identifier.status in setOf(CatalogIdentifierStatus.ACTIVE, CatalogIdentifierStatus.VERIFIED) &&
                        (identifier.normalizedValue == normalizedCode || identifier.normalizedValue.contains(normalizedText.uppercase()))
                } || item.searchableText.contains(normalizedText)
            )
        }
    }
}
