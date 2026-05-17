package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class PlatformCatalogFamily(
    val id: String,
    val canonicalName: String,
    val status: CatalogTemplateStatus = CatalogTemplateStatus.ACTIVE,
    val globalFamilyId: String = id,
    val normalizedName: String = canonicalName.trim().lowercase().replace(Regex("\\s+"), " "),
    val categoryId: String? = null,
    val brand: String? = null,
    val type: CatalogItemType = CatalogItemType.PRODUCT,
    val aliases: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog family id cannot be blank.")
        if (globalFamilyId.isBlank()) throw DomainRuleViolation("Catalog family global id cannot be blank.")
        if (canonicalName.isBlank()) throw DomainRuleViolation("Catalog family canonical name cannot be blank.")
        if (normalizedName.isBlank()) throw DomainRuleViolation("Catalog family normalized name cannot be blank.")
        categoryId?.let { if (it.isBlank()) throw DomainRuleViolation("Catalog family category id cannot be blank.") }
        brand?.let { if (it.isBlank()) throw DomainRuleViolation("Catalog family brand cannot be blank.") }
        aliases.forEach { if (it.isBlank()) throw DomainRuleViolation("Catalog family aliases cannot contain blank values.") }
    }

    fun assertActive() {
        if (status != CatalogTemplateStatus.ACTIVE) {
            throw DomainRuleViolation("Catalog family must be active.")
        }
    }
}
