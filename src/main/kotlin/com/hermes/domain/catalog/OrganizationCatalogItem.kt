package com.hermes.domain.catalog

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

data class OrganizationCatalogItem(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val activityId: String,
    val templateId: String,
    val globalCatalogId: String,
    val localName: String,
    val searchableText: String,
    val type: CatalogItemType,
    val status: CatalogItemStatus,
    val localPrice: Money,
    val taxProfileId: String,
    val publicDiscoveryStatus: PublicDiscoveryStatus,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<CatalogIdentifier> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val media: List<CatalogMediaAsset> = emptyList(),
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Organization catalog item id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (activityId.isBlank()) throw DomainRuleViolation("Activity id cannot be blank.")
        if (templateId.isBlank()) throw DomainRuleViolation("Template id cannot be blank.")
        if (localName.isBlank()) throw DomainRuleViolation("Local catalog item name cannot be blank.")
        if (searchableText.isBlank()) throw DomainRuleViolation("Searchable text cannot be blank.")
        if (taxProfileId.isBlank()) throw DomainRuleViolation("Tax profile id cannot be blank.")
    }
}
