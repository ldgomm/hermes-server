package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationCommand
import com.hermes.application.catalog.CatalogCreatePlatformTemplateCommand
import com.hermes.application.catalog.CatalogItemRequestResult
import com.hermes.application.catalog.CatalogRequestNewItemCommand
import com.hermes.application.catalog.CatalogReviewRequestCommand
import com.hermes.application.catalog.CatalogTemplatesResult
import com.hermes.application.catalog.CatalogUpdateLocalItemCommand
import com.hermes.application.catalog.OrganizationCatalogItemResult
import com.hermes.application.catalog.OrganizationCatalogItemsResult
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemCommand
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogIdentifierScope
import com.hermes.domain.catalog.CatalogIdentifierSource
import com.hermes.domain.catalog.CatalogIdentifierStatus
import com.hermes.domain.catalog.CatalogIdentifierType
import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestDecision
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.money.Money
import kotlinx.serialization.Serializable

@Serializable
data class MoneyRequest(val amount: String, val currency: String = "USD")

@Serializable
data class CatalogIdentifierRequest(
    val type: String,
    val value: String,
    val scope: String = "ORGANIZATION",
    val source: String = "ORGANIZATION",
    val status: String = "ACTIVE",
    val isPrimary: Boolean = false,
)

@Serializable
data class CatalogCreatePlatformTemplateRequest(
    val globalCatalogId: String,
    val canonicalName: String,
    val type: String,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<CatalogIdentifierRequest> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val reason: String? = null,
)

@Serializable
data class CatalogCopyFromTemplateRequest(
    val templateId: String,
    val branchId: String? = null,
    val activityId: String,
    val localPrice: MoneyRequest,
    val taxProfileCode: String,
    val reason: String,
)

@Serializable
data class CatalogUpdateLocalItemRequest(
    val localName: String? = null,
    val localPrice: MoneyRequest? = null,
    val taxProfileCode: String? = null,
    val identifiers: List<CatalogIdentifierRequest>? = null,
    val status: String? = null,
    val reason: String,
)

@Serializable
data class CatalogDisableLocalItemRequest(val reason: String)

@Serializable
data class CatalogAssignTaxProfileRequest(
    val taxProfileCode: String,
    val reason: String,
)

@Serializable
data class CatalogRequestNewItemRequest(
    val requestedName: String,
    val requestedType: String,
    val description: String? = null,
    val suggestedCategoryId: String? = null,
    val suggestedTaxProfileCode: String? = null,
    val identifiers: List<CatalogIdentifierRequest> = emptyList(),
)

@Serializable
data class CatalogReviewRequestRequest(
    val decision: String,
    val reason: String,
)

@Serializable
data class MoneyResponse(val amount: String, val currency: String)

@Serializable
data class CatalogIdentifierResponse(
    val type: String,
    val value: String,
    val normalizedValue: String,
    val scope: String,
    val status: String,
    val source: String,
    val isPrimary: Boolean,
)

@Serializable
data class PlatformCatalogTemplateResponse(
    val id: String,
    val globalCatalogId: String,
    val canonicalName: String,
    val normalizedName: String,
    val type: String,
    val status: String,
    val productFamilyId: String?,
    val variantAttributes: Map<String, String>,
    val identifiers: List<CatalogIdentifierResponse>,
    val attributes: Map<String, String>,
)

@Serializable
data class PlatformCatalogTemplatesResponse(val templates: List<PlatformCatalogTemplateResponse>)

@Serializable
data class OrganizationCatalogItemResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val activityId: String,
    val templateId: String,
    val globalCatalogId: String,
    val localName: String,
    val searchableText: String,
    val type: String,
    val status: String,
    val localPrice: MoneyResponse,
    val taxProfileId: String,
    val publicDiscoveryStatus: String,
    val productFamilyId: String?,
    val variantAttributes: Map<String, String>,
    val identifiers: List<CatalogIdentifierResponse>,
    val attributes: Map<String, String>,
)

@Serializable
data class OrganizationCatalogItemsResponse(val items: List<OrganizationCatalogItemResponse>)

@Serializable
data class CatalogItemRequestResponse(
    val id: String,
    val organizationId: String,
    val requestedByUserId: String,
    val requestedName: String,
    val requestedType: String,
    val description: String?,
    val suggestedCategoryId: String?,
    val suggestedTaxProfileCode: String?,
    val identifiers: List<CatalogIdentifierResponse>,
    val status: String,
    val reviewedByUserId: String?,
    val reviewedAt: String?,
    val reviewReason: String?,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
)

fun CatalogCreatePlatformTemplateRequest.toCommand(actorUserId: String, actorEffectivePermissions: Set<String>): CatalogCreatePlatformTemplateCommand =
    CatalogCreatePlatformTemplateCommand(
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        globalCatalogId = globalCatalogId,
        canonicalName = canonicalName,
        type = enumValueOf(type.trim().uppercase()),
        productFamilyId = productFamilyId,
        variantAttributes = variantAttributes,
        identifiers = identifiers.map { it.toDomain() },
        attributes = attributes,
        reason = reason,
    )

fun CatalogCopyFromTemplateRequest.toCommand(organizationId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogCopyTemplateToOrganizationCommand =
    CatalogCopyTemplateToOrganizationCommand(
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

fun CatalogUpdateLocalItemRequest.toCommand(organizationId: String, catalogItemId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogUpdateLocalItemCommand =
    CatalogUpdateLocalItemCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        catalogItemId = catalogItemId,
        localName = localName,
        localPrice = localPrice?.toMoney(),
        taxProfileCode = taxProfileCode,
        identifiers = identifiers?.map { it.toDomain() },
        status = status?.let { enumValueOf<CatalogItemStatus>(it.trim().uppercase()) },
        reason = reason,
    )

fun CatalogRequestNewItemRequest.toCommand(organizationId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogRequestNewItemCommand =
    CatalogRequestNewItemCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        requestedName = requestedName,
        requestedType = enumValueOf(requestedType.trim().uppercase()),
        description = description,
        suggestedCategoryId = suggestedCategoryId,
        suggestedTaxProfileCode = suggestedTaxProfileCode,
        identifiers = identifiers.map { it.toDomain() },
    )

fun CatalogReviewRequestRequest.toCommand(requestId: String, actorUserId: String, actorEffectivePermissions: Set<String>): CatalogReviewRequestCommand =
    CatalogReviewRequestCommand(
        requestId = requestId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        decision = enumValueOf<CatalogItemRequestDecision>(decision.trim().uppercase()),
        reason = reason,
    )

fun CatalogIdentifierRequest.toDomain(): CatalogIdentifier =
    CatalogIdentifier.create(
        type = enumValueOf<CatalogIdentifierType>(type.trim().uppercase()),
        value = value,
        scope = enumValueOf<CatalogIdentifierScope>(scope.trim().uppercase()),
        source = enumValueOf<CatalogIdentifierSource>(source.trim().uppercase()),
        status = enumValueOf<CatalogIdentifierStatus>(status.trim().uppercase()),
        isPrimary = isPrimary,
    )

fun MoneyRequest.toMoney(): Money = Money.of(amount = amount, currency = currency)
fun Money.toResponse(): MoneyResponse = MoneyResponse(amount = amount.toPlainString(), currency = currency.value)

fun CatalogIdentifier.toResponse(): CatalogIdentifierResponse = CatalogIdentifierResponse(type.name, value, normalizedValue, scope.name, status.name, source.name, isPrimary)
fun PlatformCatalogTemplate.toResponse(): PlatformCatalogTemplateResponse = PlatformCatalogTemplateResponse(id, globalCatalogId, canonicalName, normalizedName, type.name, status.name, productFamilyId, variantAttributes, identifiers.map { it.toResponse() }, attributes)
fun CatalogTemplatesResult.toResponse(): PlatformCatalogTemplatesResponse = PlatformCatalogTemplatesResponse(templates.map { it.toResponse() })
fun OrganizationCatalogItem.toResponse(): OrganizationCatalogItemResponse = OrganizationCatalogItemResponse(id, organizationId, branchId, activityId, templateId, globalCatalogId, localName, searchableText, type.name, status.name, localPrice.toResponse(), taxProfileId, publicDiscoveryStatus.name, productFamilyId, variantAttributes, identifiers.map { it.toResponse() }, attributes)
fun OrganizationCatalogItemResult.toResponse(): OrganizationCatalogItemResponse = item.toResponse()
fun OrganizationCatalogItemsResult.toResponse(): OrganizationCatalogItemsResponse = OrganizationCatalogItemsResponse(items.map { it.toResponse() })
fun CatalogItemRequest.toResponse(): CatalogItemRequestResponse = CatalogItemRequestResponse(id, organizationId, requestedByUserId, requestedName, requestedType.name, description, suggestedCategoryId, suggestedTaxProfileCode, identifiers.map { it.toResponse() }, status.name, reviewedByUserId, reviewedAt?.toString(), reviewReason, createdAt.toString(), updatedAt.toString(), version)
fun CatalogItemRequestResult.toResponse(): CatalogItemRequestResponse = request.toResponse()
