package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

object CatalogVariantRules {
    fun assertTemplateIsSellableVariant(template: PlatformCatalogTemplate) {
        if (template.type !in setOf(CatalogItemType.PRODUCT, CatalogItemType.SERVICE, CatalogItemType.PACKAGE, CatalogItemType.RENTAL, CatalogItemType.FEE)) {
            throw DomainRuleViolation("Only concrete catalog templates are sellable.")
        }
        if (template.productFamilyId != null && template.variantAttributes.isEmpty()) {
            throw DomainRuleViolation("Catalog variant requires variant attributes.")
        }
    }

    fun validateUniqueVariants(templates: List<PlatformCatalogTemplate>) {
        val seen = mutableSetOf<Pair<String, Map<String, String>>>()
        templates.filter { it.productFamilyId != null }.forEach { template ->
            val key = template.productFamilyId!! to template.variantAttributes
            if (!seen.add(key)) {
                throw DomainRuleViolation("Duplicate variant attributes inside product family ${template.productFamilyId}.")
            }
        }
    }

    fun assertFamilyIsNotSoldDirectly(family: PlatformCatalogFamily, templateIdToSell: String) {
        if (family.id == templateIdToSell) {
            throw DomainRuleViolation("A product family cannot be sold directly; sell a concrete template or local item.")
        }
    }
}
