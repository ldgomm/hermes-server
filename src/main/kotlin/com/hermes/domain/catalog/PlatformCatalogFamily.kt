package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

data class PlatformCatalogFamily(
    val id: String,
    val canonicalName: String,
    val status: CatalogTemplateStatus = CatalogTemplateStatus.ACTIVE,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog family id cannot be blank.")
        if (canonicalName.isBlank()) throw DomainRuleViolation("Catalog family canonical name cannot be blank.")
    }
}
