package com.hermes.domain.catalog

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation

object CatalogCopyRules {
    fun copyFromTemplate(
        id: String,
        organizationId: String,
        branchId: String? = null,
        activityId: String,
        template: PlatformCatalogTemplate,
        localPrice: Money,
        taxProfileId: String,
    ): OrganizationCatalogItem {
        if (template.status != CatalogTemplateStatus.ACTIVE) {
            throw DomainRuleViolation("Only active catalog templates can be copied.")
        }
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (activityId.isBlank()) throw DomainRuleViolation("Activity id cannot be blank.")
        if (taxProfileId.isBlank()) throw DomainRuleViolation("Tax profile id cannot be blank.")
        if (localPrice.amount.signum() <= 0) throw DomainRuleViolation("Local price must be greater than zero.")

        return OrganizationCatalogItem(
            id = id,
            organizationId = organizationId,
            branchId = branchId,
            activityId = activityId,
            templateId = template.id,
            globalCatalogId = template.globalCatalogId,
            localName = template.canonicalName,
            searchableText = listOf(
                template.canonicalName,
                template.normalizedName,
                template.globalCatalogId,
                template.identifiers.joinToString(" ") { it.normalizedValue },
            ).joinToString(" ").trim().lowercase(),
            type = template.type,
            status = CatalogItemStatus.ACTIVE,
            localPrice = localPrice,
            taxProfileId = taxProfileId,
            publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
            productFamilyId = template.productFamilyId,
            variantAttributes = template.variantAttributes,
            identifiers = template.identifiers,
            attributes = template.attributes,
            media = template.media,
        )
    }
}
