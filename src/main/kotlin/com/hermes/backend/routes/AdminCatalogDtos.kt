package com.hermes.backend.routes

import com.hermes.application.catalog.CatalogApproveRequestAsTemplateCommand
import com.hermes.application.catalog.CatalogApproveRequestAsTemplateResult
import com.hermes.application.catalog.CatalogCategoriesResult
import com.hermes.application.catalog.CatalogCategoryResult
import com.hermes.application.catalog.CatalogCreateCategoryCommand
import com.hermes.application.catalog.CatalogCreateFamilyCommand
import com.hermes.application.catalog.CatalogCreatePlatformTemplateCommand
import com.hermes.application.catalog.CatalogItemRequestResult
import com.hermes.application.catalog.CatalogItemRequestsResult
import com.hermes.application.catalog.CatalogLinkRequestToExistingTemplateCommand
import com.hermes.application.catalog.CatalogListAdminRequestsCommand
import com.hermes.application.catalog.CatalogListOrganizationRequestsCommand
import com.hermes.application.catalog.CatalogRejectRequestCommand
import com.hermes.application.catalog.CatalogRemoveLocalItemCommand
import com.hermes.application.catalog.CatalogRequestMoreInfoCommand
import com.hermes.application.catalog.CatalogRequestNewItemCommand
import com.hermes.application.catalog.CatalogSearchCategoriesCommand
import com.hermes.application.catalog.CatalogSearchFamiliesCommand
import com.hermes.application.catalog.CatalogSearchMasterTemplatesCommand
import com.hermes.application.catalog.CatalogSearchOrganizationItemsCommand
import com.hermes.application.catalog.CatalogTemplateResult
import com.hermes.application.catalog.CatalogTemplatesResult
import com.hermes.application.catalog.CatalogUpdateLocalItemCommand
import com.hermes.application.catalog.OrganizationCatalogItemResult
import com.hermes.application.catalog.OrganizationCatalogItemsResult
import com.hermes.domain.catalog.CatalogCategory
import com.hermes.domain.catalog.CatalogCategoryStatus
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogIdentifierScope
import com.hermes.domain.catalog.CatalogIdentifierSource
import com.hermes.domain.catalog.CatalogIdentifierStatus
import com.hermes.domain.catalog.CatalogIdentifierType
import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogMediaAsset
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogFamily
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlinx.serialization.Serializable

@Serializable
data class AdminCatalogMoneyRequest(
    val amount: String,
    val currency: String = "USD",
)

@Serializable
data class AdminCatalogMoneyResponse(
    val amount: String,
    val currency: String,
)

@Serializable
data class AdminCatalogIdentifierRequest(
    val type: String,
    val value: String,
    val scope: String = CatalogIdentifierScope.ORGANIZATION.name,
    val source: String = CatalogIdentifierSource.ORGANIZATION.name,
    val status: String = CatalogIdentifierStatus.ACTIVE.name,
    val isPrimary: Boolean = false,
)

@Serializable
data class AdminCatalogIdentifierResponse(
    val type: String,
    val value: String,
    val normalizedValue: String,
    val scope: String,
    val status: String,
    val source: String,
    val isPrimary: Boolean,
)

@Serializable
data class AdminCatalogMediaAssetResponse(
    val id: String,
    val ownerKind: String,
    val url: String,
    val mimeType: String,
    val status: String,
    val isPrimary: Boolean,
    val sortOrder: Int,
)

@Serializable
data class CreateAdminCatalogMasterTemplateRequest(
    val globalCatalogId: String,
    val canonicalName: String,
    val type: String = CatalogItemType.PRODUCT.name,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<AdminCatalogIdentifierRequest> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val reason: String = "Create platform catalog template",
)

@Serializable
data class AdminCatalogMasterTemplateResponse(
    val id: String,
    val globalCatalogId: String,
    val canonicalName: String,
    val normalizedName: String,
    val type: String,
    val status: String,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String>,
    val identifiers: List<AdminCatalogIdentifierResponse>,
    val attributes: Map<String, String>,
    val media: List<AdminCatalogMediaAssetResponse> = emptyList(),
)

@Serializable
data class AdminCatalogMasterTemplatesResponse(
    val templates: List<AdminCatalogMasterTemplateResponse>,
)

@Serializable
data class CreateAdminCatalogCategoryRequest(
    val parentId: String? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val businessTypeTags: Set<String> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val status: String = CatalogCategoryStatus.ACTIVE.name,
    val sortOrder: Int = 0,
    val reason: String = "Create platform catalog category",
)

@Serializable
data class AdminCatalogCategoryResponse(
    val id: String,
    val parentId: String? = null,
    val code: String,
    val name: String,
    val normalizedName: String,
    val description: String? = null,
    val businessTypeTags: Set<String>,
    val activityTags: Set<String>,
    val status: String,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class AdminCatalogCategoriesResponse(
    val categories: List<AdminCatalogCategoryResponse>,
)

@Serializable
data class CreateAdminCatalogFamilyRequest(
    val globalFamilyId: String,
    val canonicalName: String,
    val categoryId: String? = null,
    val brand: String? = null,
    val type: String = CatalogItemType.PRODUCT.name,
    val aliases: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val status: String = CatalogTemplateStatus.ACTIVE.name,
    val reason: String = "Create platform catalog family",
)

@Serializable
data class AdminCatalogFamilyResponse(
    val id: String,
    val globalFamilyId: String,
    val canonicalName: String,
    val normalizedName: String,
    val categoryId: String? = null,
    val brand: String? = null,
    val type: String,
    val aliases: List<String>,
    val attributes: Map<String, String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class AdminCatalogFamiliesResponse(
    val families: List<AdminCatalogFamilyResponse>,
)

@Serializable
data class CopyAdminCatalogItemFromTemplateRequest(
    val templateId: String,
    val branchId: String? = null,
    val activityId: String,
    val localPrice: AdminCatalogMoneyRequest,
    val taxProfileCode: String,
    val reason: String,
)

@Serializable
data class UpdateAdminCatalogLocalItemRequest(
    val localName: String? = null,
    val localPrice: AdminCatalogMoneyRequest? = null,
    val taxProfileCode: String? = null,
    val identifiers: List<AdminCatalogIdentifierRequest>? = null,
    val status: String? = null,
    val reason: String,
)

@Serializable
data class AdminCatalogLocalItemActionRequest(
    val reason: String,
)

@Serializable
data class AdminCatalogLocalItemResponse(
    val id: String,
    val organizationId: String,
    val branchId: String? = null,
    val activityId: String,
    val templateId: String,
    val globalCatalogId: String,
    val localName: String,
    val searchableText: String,
    val type: String,
    val status: String,
    val localPrice: AdminCatalogMoneyResponse,
    val taxProfileId: String,
    val publicDiscoveryStatus: String,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String>,
    val identifiers: List<AdminCatalogIdentifierResponse>,
    val attributes: Map<String, String>,
    val media: List<AdminCatalogMediaAssetResponse> = emptyList(),
)

@Serializable
data class AdminCatalogLocalItemsResponse(
    val items: List<AdminCatalogLocalItemResponse>,
)

@Serializable
data class CreateAdminCatalogRequestRequest(
    val requestedName: String,
    val requestedType: String,
    val description: String? = null,
    val suggestedCategoryId: String? = null,
    val suggestedTaxProfileCode: String? = null,
    val identifiers: List<AdminCatalogIdentifierRequest> = emptyList(),
)

@Serializable
data class ReviewAdminCatalogRequestRequest(
    val action: String,
    val reason: String? = null,
    val message: String? = null,
    val globalCatalogId: String? = null,
    val canonicalName: String? = null,
    val publish: Boolean = false,
    val productFamilyId: String? = null,
    val templateId: String? = null,
    val identifiers: List<AdminCatalogIdentifierRequest>? = null,
    val attributes: Map<String, String> = emptyMap(),
)

@Serializable
data class AdminCatalogRequestResponse(
    val id: String,
    val organizationId: String,
    val requestedByUserId: String,
    val requestedName: String,
    val requestedType: String,
    val description: String? = null,
    val suggestedCategoryId: String? = null,
    val suggestedTaxProfileCode: String? = null,
    val identifiers: List<AdminCatalogIdentifierResponse>,
    val status: String,
    val reviewedByUserId: String? = null,
    val reviewedAt: String? = null,
    val reviewReason: String? = null,
    val linkedTemplateId: String? = null,
    val adminMessage: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

@Serializable
data class AdminCatalogRequestsResponse(
    val requests: List<AdminCatalogRequestResponse>,
)

@Serializable
data class AdminCatalogRequestApprovedResponse(
    val request: AdminCatalogRequestResponse,
    val template: AdminCatalogMasterTemplateResponse,
)

enum class AdminCatalogReviewAction {
    APPROVE_AS_TEMPLATE,
    REJECT,
    LINK_TO_EXISTING_TEMPLATE,
    REQUEST_MORE_INFO,
}

fun CreateAdminCatalogMasterTemplateRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogCreatePlatformTemplateCommand = CatalogCreatePlatformTemplateCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    globalCatalogId = globalCatalogId,
    canonicalName = canonicalName,
    type = type.toCatalogEnum("Template type"),
    productFamilyId = productFamilyId,
    variantAttributes = variantAttributes,
    identifiers = identifiers.map { it.toDomain() },
    attributes = attributes,
    reason = reason,
)

fun adminCatalogMasterTemplateSearchCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    query: String?,
    identifier: String?,
    type: String?,
    limit: Int,
): CatalogSearchMasterTemplatesCommand = CatalogSearchMasterTemplatesCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    query = query,
    identifier = identifier,
    type = type?.toCatalogEnum("Template type"),
    limit = limit,
)

fun CreateAdminCatalogCategoryRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogCreateCategoryCommand = CatalogCreateCategoryCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    parentId = parentId,
    code = code,
    name = name,
    description = description,
    businessTypeTags = businessTypeTags,
    activityTags = activityTags,
    status = status.toCatalogEnum("Category status"),
    sortOrder = sortOrder,
    reason = reason,
)

fun adminCatalogCategorySearchCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    parentId: String?,
    query: String?,
    statuses: String?,
    limit: Int,
): CatalogSearchCategoriesCommand = CatalogSearchCategoriesCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    parentId = parentId,
    query = query,
    statuses = statuses.toCatalogEnumSet(),
    limit = limit,
)

fun CreateAdminCatalogFamilyRequest.toCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogCreateFamilyCommand = CatalogCreateFamilyCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    globalFamilyId = globalFamilyId,
    canonicalName = canonicalName,
    categoryId = categoryId,
    brand = brand,
    type = type.toCatalogEnum("Family type"),
    aliases = aliases,
    attributes = attributes,
    status = status.toCatalogEnum("Family status"),
    reason = reason,
)

fun adminCatalogFamilySearchCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    query: String?,
    categoryId: String?,
    type: String?,
    statuses: String?,
    limit: Int,
): CatalogSearchFamiliesCommand = CatalogSearchFamiliesCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    query = query,
    categoryId = categoryId,
    type = type?.toCatalogEnum("Family type"),
    statuses = statuses.toCatalogEnumSet(),
    limit = limit,
)

fun CopyAdminCatalogItemFromTemplateRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): com.hermes.application.catalog.CatalogCopyTemplateToOrganizationCommand =
    com.hermes.application.catalog.CatalogCopyTemplateToOrganizationCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        templateId = templateId,
        branchId = branchId,
        activityId = activityId,
        localPrice = localPrice.toMoney(),
        taxProfileCode = taxProfileCode,
        reason = reason,
    )

fun adminCatalogLocalItemSearchCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    query: String?,
    identifier: String?,
    type: String?,
    statuses: String?,
    limit: Int,
): CatalogSearchOrganizationItemsCommand = CatalogSearchOrganizationItemsCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    query = query,
    identifier = identifier,
    type = type?.toCatalogEnum("Item type"),
    statuses = statuses.toCatalogEnumSet(),
    limit = limit,
)

fun UpdateAdminCatalogLocalItemRequest.toCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogUpdateLocalItemCommand = CatalogUpdateLocalItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    catalogItemId = itemId,
    localName = localName,
    localPrice = localPrice?.toMoney(),
    taxProfileCode = taxProfileCode,
    identifiers = identifiers?.map { it.toDomain() },
    status = status?.toCatalogEnum("Item status"),
    reason = reason,
)

fun AdminCatalogLocalItemActionRequest.toActivateCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogUpdateLocalItemCommand = CatalogUpdateLocalItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    catalogItemId = itemId,
    status = CatalogItemStatus.ACTIVE,
    reason = reason,
)

fun AdminCatalogLocalItemActionRequest.toDeactivateCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogUpdateLocalItemCommand = CatalogUpdateLocalItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    catalogItemId = itemId,
    status = CatalogItemStatus.PAUSED,
    reason = reason,
)

fun AdminCatalogLocalItemActionRequest.toStatusCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    status: CatalogItemStatus,
): CatalogUpdateLocalItemCommand = CatalogUpdateLocalItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    catalogItemId = itemId,
    status = status,
    reason = reason,
)

fun AdminCatalogLocalItemActionRequest.toRemoveCommand(
    organizationId: String,
    itemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRemoveLocalItemCommand = CatalogRemoveLocalItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    catalogItemId = itemId,
    reason = reason,
)

fun CreateAdminCatalogRequestRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRequestNewItemCommand = CatalogRequestNewItemCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    requestedName = requestedName,
    requestedType = requestedType.toCatalogEnum("Requested item type"),
    description = description,
    suggestedCategoryId = suggestedCategoryId,
    suggestedTaxProfileCode = suggestedTaxProfileCode,
    identifiers = identifiers.map { it.toDomain() },
)

fun adminCatalogOrganizationRequestsCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    statuses: String?,
    requestedType: String?,
    limit: Int,
): CatalogListOrganizationRequestsCommand = CatalogListOrganizationRequestsCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    statuses = statuses.toCatalogEnumSet(),
    requestedType = requestedType?.toCatalogEnum("Requested item type"),
    limit = limit,
)

fun adminCatalogPlatformRequestsCommand(
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
    organizationId: String?,
    statuses: String?,
    requestedType: String?,
    query: String?,
    limit: Int,
): CatalogListAdminRequestsCommand = CatalogListAdminRequestsCommand(
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    organizationId = organizationId,
    statuses = statuses.toCatalogEnumSet(),
    requestedType = requestedType?.toCatalogEnum("Requested item type"),
    query = query,
    limit = limit,
)

fun ReviewAdminCatalogRequestRequest.toAction(): AdminCatalogReviewAction = action.toCatalogEnum("Catalog review action")

fun ReviewAdminCatalogRequestRequest.toApproveAsTemplateCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogApproveRequestAsTemplateCommand = CatalogApproveRequestAsTemplateCommand(
    requestId = requestId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    globalCatalogId = globalCatalogId,
    canonicalName = canonicalName,
    publish = publish,
    productFamilyId = productFamilyId,
    identifiers = identifiers?.map { it.toDomain() },
    attributes = attributes,
    reason = reason.requiredAdminCatalog("Catalog request approval reason"),
)

fun ReviewAdminCatalogRequestRequest.toRejectCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRejectRequestCommand = CatalogRejectRequestCommand(
    requestId = requestId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    reason = reason.requiredAdminCatalog("Catalog request rejection reason"),
)

fun ReviewAdminCatalogRequestRequest.toLinkToExistingTemplateCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogLinkRequestToExistingTemplateCommand = CatalogLinkRequestToExistingTemplateCommand(
    requestId = requestId,
    templateId = templateId.requiredAdminCatalog("Linked template id"),
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    reason = reason.requiredAdminCatalog("Catalog request link reason"),
)

fun ReviewAdminCatalogRequestRequest.toRequestMoreInfoCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRequestMoreInfoCommand = CatalogRequestMoreInfoCommand(
    requestId = requestId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    message = message.requiredAdminCatalog("Catalog request more-info message"),
)

fun AdminCatalogIdentifierRequest.toDomain(): CatalogIdentifier = CatalogIdentifier.create(
    type = type.toCatalogEnum("Identifier type"),
    value = value,
    scope = scope.toCatalogEnum("Identifier scope"),
    source = source.toCatalogEnum("Identifier source"),
    status = status.toCatalogEnum("Identifier status"),
    isPrimary = isPrimary,
)

fun AdminCatalogMoneyRequest.toMoney(): Money = Money.of(amount = amount, currency = currency)

fun Money.toAdminCatalogResponse(): AdminCatalogMoneyResponse = AdminCatalogMoneyResponse(
    amount = amount.toPlainString(),
    currency = currency.value,
)

fun CatalogIdentifier.toAdminCatalogResponse(): AdminCatalogIdentifierResponse = AdminCatalogIdentifierResponse(
    type = type.name,
    value = value,
    normalizedValue = normalizedValue,
    scope = scope.name,
    status = status.name,
    source = source.name,
    isPrimary = isPrimary,
)

fun CatalogMediaAsset.toAdminCatalogResponse(): AdminCatalogMediaAssetResponse = AdminCatalogMediaAssetResponse(
    id = id,
    ownerKind = ownerKind.name,
    url = url,
    mimeType = mimeType,
    status = status.name,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
)

fun PlatformCatalogTemplate.toAdminCatalogResponse(): AdminCatalogMasterTemplateResponse = AdminCatalogMasterTemplateResponse(
    id = id,
    globalCatalogId = globalCatalogId,
    canonicalName = canonicalName,
    normalizedName = normalizedName,
    type = type.name,
    status = status.name,
    productFamilyId = productFamilyId,
    variantAttributes = variantAttributes,
    identifiers = identifiers.map { it.toAdminCatalogResponse() },
    attributes = attributes,
    media = media.map { it.toAdminCatalogResponse() },
)

fun CatalogTemplateResult.toAdminCatalogResponse(): AdminCatalogMasterTemplateResponse = template.toAdminCatalogResponse()

fun CatalogTemplatesResult.toAdminCatalogResponse(): AdminCatalogMasterTemplatesResponse = AdminCatalogMasterTemplatesResponse(
    templates = templates.map { it.toAdminCatalogResponse() },
)

fun CatalogCategory.toAdminCatalogResponse(): AdminCatalogCategoryResponse = AdminCatalogCategoryResponse(
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

fun CatalogCategoryResult.toAdminCatalogResponse(): AdminCatalogCategoryResponse = category.toAdminCatalogResponse()

fun CatalogCategoriesResult.toAdminCatalogResponse(): AdminCatalogCategoriesResponse = AdminCatalogCategoriesResponse(
    categories = categories.map { it.toAdminCatalogResponse() },
)

fun PlatformCatalogFamily.toAdminCatalogResponse(): AdminCatalogFamilyResponse = AdminCatalogFamilyResponse(
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

fun com.hermes.application.catalog.CatalogFamilyResult.toAdminCatalogResponse(): AdminCatalogFamilyResponse =
    family.toAdminCatalogResponse()

fun com.hermes.application.catalog.CatalogFamiliesResult.toAdminCatalogResponse(): AdminCatalogFamiliesResponse =
    AdminCatalogFamiliesResponse(families = families.map { it.toAdminCatalogResponse() })

fun OrganizationCatalogItem.toAdminCatalogResponse(): AdminCatalogLocalItemResponse = AdminCatalogLocalItemResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    activityId = activityId,
    templateId = templateId,
    globalCatalogId = globalCatalogId,
    localName = localName,
    searchableText = searchableText,
    type = type.name,
    status = status.name,
    localPrice = localPrice.toAdminCatalogResponse(),
    taxProfileId = taxProfileId,
    publicDiscoveryStatus = publicDiscoveryStatus.name,
    productFamilyId = productFamilyId,
    variantAttributes = variantAttributes,
    identifiers = identifiers.map { it.toAdminCatalogResponse() },
    attributes = attributes,
    media = media.map { it.toAdminCatalogResponse() },
)

fun OrganizationCatalogItemResult.toAdminCatalogResponse(): AdminCatalogLocalItemResponse = item.toAdminCatalogResponse()

fun OrganizationCatalogItemsResult.toAdminCatalogResponse(): AdminCatalogLocalItemsResponse = AdminCatalogLocalItemsResponse(
    items = items.map { it.toAdminCatalogResponse() },
)

fun CatalogItemRequest.toAdminCatalogResponse(): AdminCatalogRequestResponse = AdminCatalogRequestResponse(
    id = id,
    organizationId = organizationId,
    requestedByUserId = requestedByUserId,
    requestedName = requestedName,
    requestedType = requestedType.name,
    description = description,
    suggestedCategoryId = suggestedCategoryId,
    suggestedTaxProfileCode = suggestedTaxProfileCode,
    identifiers = identifiers.map { it.toAdminCatalogResponse() },
    status = status.name,
    reviewedByUserId = reviewedByUserId,
    reviewedAt = reviewedAt?.toString(),
    reviewReason = reviewReason,
    linkedTemplateId = linkedTemplateId,
    adminMessage = adminMessage,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun CatalogItemRequestResult.toAdminCatalogResponse(): AdminCatalogRequestResponse = request.toAdminCatalogResponse()

fun CatalogItemRequestsResult.toAdminCatalogResponse(): AdminCatalogRequestsResponse = AdminCatalogRequestsResponse(
    requests = requests.map { it.toAdminCatalogResponse() },
)

fun CatalogApproveRequestAsTemplateResult.toAdminCatalogResponse(): AdminCatalogRequestApprovedResponse =
    AdminCatalogRequestApprovedResponse(
        request = request.toAdminCatalogResponse(),
        template = template.toAdminCatalogResponse(),
    )

private inline fun <reified T : Enum<T>> String.toCatalogEnum(label: String): T =
    runCatching { enumValueOf<T>(trim().uppercase()) }.getOrElse {
        throw DomainRuleViolation("Unsupported $label: $this.")
    }

private inline fun <reified T : Enum<T>> String?.toCatalogEnumSet(): Set<T> =
    this?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { it.toCatalogEnum<T>("enum value") }
        ?.toSet()
        .orEmpty()

private fun String?.requiredAdminCatalog(label: String): String =
    this?.trim()?.takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
