package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class PlatformCatalogTemplate(
    val id: String,
    val globalCatalogId: String,
    val canonicalName: String,
    val normalizedName: String,
    val type: CatalogItemType,
    val status: CatalogTemplateStatus,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<CatalogIdentifier> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val media: List<CatalogMediaAsset> = emptyList(),
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog template id cannot be blank.")
        if (globalCatalogId.isBlank()) throw DomainRuleViolation("Global catalog id cannot be blank.")
        if (canonicalName.isBlank()) throw DomainRuleViolation("Canonical name cannot be blank.")
        if (normalizedName.isBlank()) throw DomainRuleViolation("Normalized name cannot be blank.")
    }
}
