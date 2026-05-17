package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.PlatformCatalogFamily

data class CatalogCategoryResult(val category: CatalogCategory)
data class CatalogCategoriesResult(val categories: List<CatalogCategory>)
data class CatalogFamilyResult(val family: PlatformCatalogFamily)
data class CatalogFamiliesResult(val families: List<PlatformCatalogFamily>)
