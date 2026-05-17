package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class CatalogCategory(
    val id: String,
    val parentId: String? = null,
    val code: String,
    val name: String,
    val normalizedName: String = name.trim().lowercase().replace(Regex("\\s+"), " "),
    val description: String? = null,
    val businessTypeTags: Set<String> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val status: CatalogCategoryStatus = CatalogCategoryStatus.ACTIVE,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog category id cannot be blank.")
        if (code.isBlank()) throw DomainRuleViolation("Catalog category code cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Catalog category name cannot be blank.")
        if (normalizedName.isBlank()) throw DomainRuleViolation("Catalog category normalized name cannot be blank.")
        parentId?.let {
            if (it.isBlank()) throw DomainRuleViolation("Catalog category parent id cannot be blank.")
            if (it == id) throw DomainRuleViolation("Catalog category cannot be its own parent.")
        }
        if (sortOrder < 0) throw DomainRuleViolation("Catalog category sort order cannot be negative.")
    }

    fun assertActive() {
        if (status != CatalogCategoryStatus.ACTIVE) {
            throw DomainRuleViolation("Catalog category must be active.")
        }
    }
}
