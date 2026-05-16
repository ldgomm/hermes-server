package com.hermes.domain.sale

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.LocalDate

data class CustomerSnapshot(
    val customerId: String?,
    val displayName: String,
    val taxId: String?,
    val taxIdType: String?,
    val email: String? = null,
) {
    init {
        if (displayName.isBlank()) throw DomainRuleViolation("Customer snapshot display name cannot be blank.")
    }

    companion object {
        fun finalConsumer(): CustomerSnapshot =
            CustomerSnapshot(
                customerId = null,
                displayName = "Consumidor final",
                taxId = "9999999999999",
                taxIdType = "final_consumer",
            )
    }
}

data class CatalogItemSnapshot(
    val catalogItemId: String,
    val sourceTemplateId: String?,
    val globalCatalogId: String,
    val productFamilyId: String?,
    val name: String,
    val type: CatalogItemType,
    val taxProfileId: String,
    val unitCode: String,
) {
    init {
        if (catalogItemId.isBlank()) throw DomainRuleViolation("Catalog item snapshot id cannot be blank.")
        if (globalCatalogId.isBlank()) throw DomainRuleViolation("Catalog item snapshot global catalog id cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Catalog item snapshot name cannot be blank.")
        if (taxProfileId.isBlank()) throw DomainRuleViolation("Catalog item snapshot tax profile id cannot be blank.")
        if (unitCode.isBlank()) throw DomainRuleViolation("Catalog item snapshot unit code cannot be blank.")
    }
}

data class TaxProfileSnapshotForSale(
    val code: String,
    val taxName: String,
    val rate: Percentage,
    val sriTaxCode: String,
    val sriRateCode: String,
    val treatment: TaxTreatment,
    val legalBasis: String,
    val effectiveFrom: LocalDate,
    val source: String,
) {
    init {
        if (code.isBlank()) throw DomainRuleViolation("Tax profile snapshot code cannot be blank.")
        if (taxName.isBlank()) throw DomainRuleViolation("Tax profile snapshot tax name cannot be blank.")
        if (sriTaxCode.isBlank()) throw DomainRuleViolation("Tax profile snapshot SRI tax code cannot be blank.")
        if (sriRateCode.isBlank()) throw DomainRuleViolation("Tax profile snapshot SRI rate code cannot be blank.")
        if (legalBasis.isBlank()) throw DomainRuleViolation("Tax profile snapshot legal basis cannot be blank.")
        if (source.isBlank()) throw DomainRuleViolation("Tax profile snapshot source cannot be blank.")
    }
}
