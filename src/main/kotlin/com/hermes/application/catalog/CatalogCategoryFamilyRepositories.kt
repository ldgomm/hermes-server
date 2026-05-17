package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogFamily

interface CatalogCategoryRepository {
    fun create(category: CatalogCategory)
    fun update(category: CatalogCategory)
    fun findById(id: String): CatalogCategory?
    fun findByCode(code: String): CatalogCategory?
    fun existsByCode(code: String): Boolean
    fun search(query: CatalogCategorySearchQuery): List<CatalogCategory>
}

data class CatalogCategorySearchQuery(
    val parentId: String? = null,
    val query: String? = null,
    val statuses: Set<CatalogCategoryStatus> = emptySet(),
    val limit: Int = 100,
)

interface PlatformCatalogFamilyRepository {
    fun create(family: PlatformCatalogFamily)
    fun update(family: PlatformCatalogFamily)
    fun findById(id: String): PlatformCatalogFamily?
    fun findByGlobalFamilyId(globalFamilyId: String): PlatformCatalogFamily?
    fun existsByGlobalFamilyId(globalFamilyId: String): Boolean
    fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily>
}

data class PlatformCatalogFamilySearchQuery(
    val query: String? = null,
    val categoryId: String? = null,
    val type: CatalogItemType? = null,
    val statuses: Set<CatalogTemplateStatus> = emptySet(),
    val limit: Int = 100,
)
