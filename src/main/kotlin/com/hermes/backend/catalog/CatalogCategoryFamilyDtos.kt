package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogCategoriesResult
import com.hermes.application.catalog.CatalogCategoryResult
import com.hermes.application.catalog.CatalogChangeTemplateStatusCommand
import com.hermes.application.catalog.CatalogCreateCategoryCommand
import com.hermes.application.catalog.CatalogCreateFamilyCommand
import com.hermes.application.catalog.CatalogFamiliesResult
import com.hermes.application.catalog.CatalogFamilyResult
import com.hermes.application.catalog.CatalogGetCategoryCommand
import com.hermes.application.catalog.CatalogGetFamilyCommand
import com.hermes.application.catalog.CatalogGetTemplateCommand
import com.hermes.application.catalog.CatalogSearchCategoriesCommand
import com.hermes.application.catalog.CatalogSearchFamiliesCommand
import com.hermes.application.catalog.CatalogUpdateCategoryCommand
import com.hermes.application.catalog.CatalogUpdateFamilyCommand
import com.hermes.application.catalog.CatalogUpdateTemplateCommand
import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.PlatformCatalogFamily
import kotlinx.serialization.Serializable

@Serializable
data class CatalogCreateCategoryRequest(
    val parentId: String? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val businessTypeTags: Set<String> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val status: String = CatalogCategoryStatus.ACTIVE.name,
    val sortOrder: Int = 0,
    val reason: String? = null,
)

@Serializable
data class CatalogUpdateCategoryRequest(
    val parentId: String? = null,
    val clearParent: Boolean = false,
    val name: String? = null,
    val description: String? = null,
    val clearDescription: Boolean = false,
    val businessTypeTags: Set<String>? = null,
    val activityTags: Set<String>? = null,
    val status: String? = null,
    val sortOrder: Int? = null,
    val reason: String,
)

@Serializable
data class CatalogCreateFamilyRequest(
    val globalFamilyId: String,
    val canonicalName: String,
    val categoryId: String? = null,
    val brand: String? = null,
    val type: String = CatalogItemType.PRODUCT.name,
    val aliases: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val status: String = CatalogTemplateStatus.ACTIVE.name,
    val reason: String? = null,
)

@Serializable
data class CatalogUpdateFamilyRequest(
    val canonicalName: String? = null,
    val categoryId: String? = null,
    val clearCategory: Boolean = false,
    val brand: String? = null,
    val clearBrand: Boolean = false,
    val type: String? = null,
    val aliases: List<String>? = null,
    val attributes: Map<String, String>? = null,
    val status: String? = null,
    val reason: String,
)

@Serializable
data class CatalogUpdateTemplateRequest(
    val canonicalName: String? = null,
    val type: String? = null,
    val productFamilyId: String? = null,
    val clearProductFamily: Boolean = false,
    val variantAttributes: Map<String, String>? = null,
    val identifiers: List<CatalogIdentifierRequest>? = null,
    val attributes: Map<String, String>? = null,
    val reason: String,
)

@Serializable
data class CatalogChangeTemplateStatusRequest(
    val status: String? = null,
    val reason: String,
)

@Serializable
data class CatalogCategoryResponse(
    val id: String,
    val parentId: String?,
    val code: String,
    val name: String,
    val normalizedName: String,
    val description: String?,
    val businessTypeTags: Set<String>,
    val activityTags: Set<String>,
    val status: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class CatalogCategoriesResponse(val categories: List<CatalogCategoryResponse>)

@Serializable
data class PlatformCatalogFamilyResponse(
    val id: String,
    val globalFamilyId: String,
    val canonicalName: String,
    val normalizedName: String,
    val categoryId: String?,
    val brand: String?,
    val type: String,
    val aliases: List<String>,
    val attributes: Map<String, String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class PlatformCatalogFamiliesResponse(val families: List<PlatformCatalogFamilyResponse>)

fun CatalogCreateCategoryRequest.toCommand(actorUserId: String, actorEffectivePermissions: Set<String>): CatalogCreateCategoryCommand =
    CatalogCreateCategoryCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        parentId = parentId,
        code = code,
        name = name,
        description = description,
        businessTypeTags = businessTypeTags,
        activityTags = activityTags,
        status = enumValueOf(status.trim().uppercase()),
        sortOrder = sortOrder,
        reason = reason,
    )

fun CatalogUpdateCategoryRequest.toCommand(categoryId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogUpdateCategoryCommand =
    CatalogUpdateCategoryCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        categoryId = categoryId,
        parentId = parentId,
        clearParent = clearParent,
        name = name,
        description = description,
        clearDescription = clearDescription,
        businessTypeTags = businessTypeTags,
        activityTags = activityTags,
        status = status?.let { enumValueOf<CatalogCategoryStatus>(it.trim().uppercase()) },
        sortOrder = sortOrder,
        reason = reason,
    )

fun CatalogCreateFamilyRequest.toCommand(actorUserId: String, actorEffectivePermissions: Set<String>): CatalogCreateFamilyCommand =
    CatalogCreateFamilyCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        globalFamilyId = globalFamilyId,
        canonicalName = canonicalName,
        categoryId = categoryId,
        brand = brand,
        type = enumValueOf(type.trim().uppercase()),
        aliases = aliases,
        attributes = attributes,
        status = enumValueOf(status.trim().uppercase()),
        reason = reason,
    )

fun CatalogUpdateFamilyRequest.toCommand(familyId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogUpdateFamilyCommand =
    CatalogUpdateFamilyCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        familyId = familyId,
        canonicalName = canonicalName,
        categoryId = categoryId,
        clearCategory = clearCategory,
        brand = brand,
        clearBrand = clearBrand,
        type = type?.let { enumValueOf<CatalogItemType>(it.trim().uppercase()) },
        aliases = aliases,
        attributes = attributes,
        status = status?.let { enumValueOf<CatalogTemplateStatus>(it.trim().uppercase()) },
        reason = reason,
    )

fun CatalogUpdateTemplateRequest.toCommand(templateId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogUpdateTemplateCommand =
    CatalogUpdateTemplateCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        templateId = templateId,
        canonicalName = canonicalName,
        type = type?.let { enumValueOf<CatalogItemType>(it.trim().uppercase()) },
        productFamilyId = productFamilyId,
        clearProductFamily = clearProductFamily,
        variantAttributes = variantAttributes,
        identifiers = identifiers?.map { it.toDomain() },
        attributes = attributes,
        reason = reason,
    )

fun CatalogCategory.toResponse(): CatalogCategoryResponse = CatalogCategoryResponse(
    id = id,
    parentId = parentId,
    code = code,
    name = name,
    normalizedName = normalizedName,
    description = description,
    businessTypeTags = businessTypeTags,
    activityTags = activityTags,
    status = status.name,
    sortOrder = sortOrder,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun PlatformCatalogFamily.toResponse(): PlatformCatalogFamilyResponse = PlatformCatalogFamilyResponse(
    id = id,
    globalFamilyId = globalFamilyId,
    canonicalName = canonicalName,
    normalizedName = normalizedName,
    categoryId = categoryId,
    brand = brand,
    type = type.name,
    aliases = aliases,
    attributes = attributes,
    status = status.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun CatalogCategoryResult.toResponse(): CatalogCategoryResponse = category.toResponse()
fun CatalogCategoriesResult.toResponse(): CatalogCategoriesResponse = CatalogCategoriesResponse(categories.map { it.toResponse() })
fun CatalogFamilyResult.toResponse(): PlatformCatalogFamilyResponse = family.toResponse()
fun CatalogFamiliesResult.toResponse(): PlatformCatalogFamiliesResponse = PlatformCatalogFamiliesResponse(families.map { it.toResponse() })

fun categorySearchCommand(actorUserId: String, actorEffectivePermissions: Set<String>, parentId: String?, query: String?, statuses: String?, limit: Int): CatalogSearchCategoriesCommand =
    CatalogSearchCategoriesCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        parentId = parentId,
        query = query,
        statuses = statuses.parseEnumSet<CatalogCategoryStatus>(),
        limit = limit,
    )

fun familySearchCommand(actorUserId: String, actorEffectivePermissions: Set<String>, query: String?, categoryId: String?, type: String?, statuses: String?, limit: Int): CatalogSearchFamiliesCommand =
    CatalogSearchFamiliesCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        query = query,
        categoryId = categoryId,
        type = type?.trim()?.takeIf { it.isNotBlank() }?.let { enumValueOf<CatalogItemType>(it.uppercase()) },
        statuses = statuses.parseEnumSet<CatalogTemplateStatus>(),
        limit = limit,
    )

inline fun <reified T : Enum<T>> String?.parseEnumSet(): Set<T> =
    this?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { enumValueOf<T>(it.uppercase()) }
        ?.toSet()
        .orEmpty()
