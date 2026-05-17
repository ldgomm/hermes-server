package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogApproveRequestAsTemplateCommand
import com.hermes.application.catalog.CatalogApproveRequestAsTemplateResult
import com.hermes.application.catalog.CatalogItemRequestsResult
import com.hermes.application.catalog.CatalogLinkRequestToExistingTemplateCommand
import com.hermes.application.catalog.CatalogRejectRequestCommand
import com.hermes.application.catalog.CatalogRequestMoreInfoCommand
import com.hermes.domain.catalog.CatalogItemRequest
import kotlinx.serialization.Serializable

@Serializable
data class CatalogApproveRequestAsTemplateRequest(
    val globalCatalogId: String? = null,
    val canonicalName: String? = null,
    val publish: Boolean = false,
    val productFamilyId: String? = null,
    val identifiers: List<CatalogIdentifierRequest>? = null,
    val attributes: Map<String, String> = emptyMap(),
    val reason: String,
)

@Serializable
data class CatalogRejectRequestRequest(val reason: String)

@Serializable
data class CatalogLinkRequestToExistingTemplateRequest(
    val templateId: String,
    val reason: String,
)

@Serializable
data class CatalogRequestMoreInfoRequest(val message: String)

@Serializable
data class CatalogItemRequestsResponse(val requests: List<CatalogItemRequestResponse>)

@Serializable
data class CatalogApproveRequestAsTemplateResponse(
    val request: CatalogItemRequestResponse,
    val template: PlatformCatalogTemplateResponse,
)

fun CatalogApproveRequestAsTemplateRequest.toCommand(
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
    reason = reason,
)

fun CatalogRejectRequestRequest.toCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRejectRequestCommand = CatalogRejectRequestCommand(
    requestId = requestId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    reason = reason,
)

fun CatalogLinkRequestToExistingTemplateRequest.toCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogLinkRequestToExistingTemplateCommand = CatalogLinkRequestToExistingTemplateCommand(
    requestId = requestId,
    templateId = templateId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    reason = reason,
)

fun CatalogRequestMoreInfoRequest.toCommand(
    requestId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CatalogRequestMoreInfoCommand = CatalogRequestMoreInfoCommand(
    requestId = requestId,
    actorUserId = actorUserId,
    actorEffectivePermissions = actorEffectivePermissions,
    message = message,
)

fun CatalogItemRequestsResult.toResponse(): CatalogItemRequestsResponse = CatalogItemRequestsResponse(
    requests = requests.map { it.toAdvancedResponse() },
)

fun CatalogApproveRequestAsTemplateResult.toResponse(): CatalogApproveRequestAsTemplateResponse =
    CatalogApproveRequestAsTemplateResponse(
        request = request.toAdvancedResponse(),
        template = template.toResponse(),
    )

fun CatalogItemRequest.toAdvancedResponse(): CatalogItemRequestResponse = CatalogItemRequestResponse(
    id = id,
    organizationId = organizationId,
    requestedByUserId = requestedByUserId,
    requestedName = requestedName,
    requestedType = requestedType.name,
    description = description,
    suggestedCategoryId = suggestedCategoryId,
    suggestedTaxProfileCode = suggestedTaxProfileCode,
    identifiers = identifiers.map { it.toResponse() },
    status = status.name,
    reviewedByUserId = reviewedByUserId,
    reviewedAt = reviewedAt?.toString(),
    reviewReason = buildList {
        reviewReason?.let { add(it) }
        linkedTemplateId?.let { add("linkedTemplateId=$it") }
        adminMessage?.let { add("adminMessage=$it") }
    }.takeIf { it.isNotEmpty() }?.joinToString(" | "),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    version = version,
)

fun com.hermes.application.catalog.CatalogItemRequestResult.toAdvancedResponse(): CatalogItemRequestResponse =
    request.toAdvancedResponse()
