package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus

data class CatalogCreateCategoryCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val parentId: String? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val businessTypeTags: Set<String> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val status: CatalogCategoryStatus = CatalogCategoryStatus.ACTIVE,
    val sortOrder: Int = 0,
    val reason: String? = null,
)

data class CatalogUpdateCategoryCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val categoryId: String,
    val parentId: String? = null,
    val clearParent: Boolean = false,
    val name: String? = null,
    val description: String? = null,
    val clearDescription: Boolean = false,
    val businessTypeTags: Set<String>? = null,
    val activityTags: Set<String>? = null,
    val status: CatalogCategoryStatus? = null,
    val sortOrder: Int? = null,
    val reason: String,
)

data class CatalogGetCategoryCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val categoryId: String,
)

data class CatalogSearchCategoriesCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val parentId: String? = null,
    val query: String? = null,
    val statuses: Set<CatalogCategoryStatus> = emptySet(),
    val limit: Int = 100,
)

data class CatalogCreateFamilyCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val globalFamilyId: String,
    val canonicalName: String,
    val categoryId: String? = null,
    val brand: String? = null,
    val type: CatalogItemType = CatalogItemType.PRODUCT,
    val aliases: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val status: CatalogTemplateStatus = CatalogTemplateStatus.ACTIVE,
    val reason: String? = null,
)

data class CatalogUpdateFamilyCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val familyId: String,
    val canonicalName: String? = null,
    val categoryId: String? = null,
    val clearCategory: Boolean = false,
    val brand: String? = null,
    val clearBrand: Boolean = false,
    val type: CatalogItemType? = null,
    val aliases: List<String>? = null,
    val attributes: Map<String, String>? = null,
    val status: CatalogTemplateStatus? = null,
    val reason: String,
)

data class CatalogGetFamilyCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val familyId: String,
)

data class CatalogSearchFamiliesCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val categoryId: String? = null,
    val type: CatalogItemType? = null,
    val statuses: Set<CatalogTemplateStatus> = emptySet(),
    val limit: Int = 100,
)

data class CatalogGetTemplateCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val templateId: String,
)

data class CatalogUpdateTemplateCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val templateId: String,
    val canonicalName: String? = null,
    val type: CatalogItemType? = null,
    val productFamilyId: String? = null,
    val clearProductFamily: Boolean = false,
    val variantAttributes: Map<String, String>? = null,
    val identifiers: List<CatalogIdentifier>? = null,
    val attributes: Map<String, String>? = null,
    val reason: String,
)

data class CatalogChangeTemplateStatusCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val templateId: String,
    val status: CatalogTemplateStatus,
    val reason: String,
)
