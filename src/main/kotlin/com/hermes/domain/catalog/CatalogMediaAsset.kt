package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class CatalogMediaAsset(
    val id: String,
    val ownerKind: CatalogMediaOwnerKind,
    val url: String,
    val mimeType: String,
    val status: CatalogMediaStatus,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog media id cannot be blank.")
        if (url.isBlank()) throw DomainRuleViolation("Catalog media url cannot be blank.")
        if (mimeType.isBlank()) throw DomainRuleViolation("Catalog media mime type cannot be blank.")
        if (sortOrder < 0) throw DomainRuleViolation("Catalog media sort order cannot be negative.")
    }
}
