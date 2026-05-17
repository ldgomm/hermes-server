package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.PlatformCatalogTemplate


data class CatalogItemRequestSearchQuery(
    val organizationId: String? = null,
    val statuses: Set<CatalogItemRequestStatus> = emptySet(),
    val requestedType: CatalogItemType? = null,
    val requestedByUserId: String? = null,
    val query: String? = null,
    val limit: Int = 100,
)

interface CatalogItemRequestSearchRepository {
    fun search(query: CatalogItemRequestSearchQuery): List<CatalogItemRequest>
}

data class CatalogListOrganizationRequestsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val statuses: Set<CatalogItemRequestStatus> = emptySet(),
    val requestedType: CatalogItemType? = null,
    val limit: Int = 100,
)

data class CatalogListAdminRequestsCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val organizationId: String? = null,
    val statuses: Set<CatalogItemRequestStatus> = emptySet(),
    val requestedType: CatalogItemType? = null,
    val query: String? = null,
    val limit: Int = 100,
)

data class CatalogApproveRequestAsTemplateCommand(
    val requestId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val globalCatalogId: String? = null,
    val canonicalName: String? = null,
    val publish: Boolean = false,
    val productFamilyId: String? = null,
    val identifiers: List<CatalogIdentifier>? = null,
    val attributes: Map<String, String> = emptyMap(),
    val reason: String,
)

data class CatalogRejectRequestCommand(
    val requestId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

data class CatalogLinkRequestToExistingTemplateCommand(
    val requestId: String,
    val templateId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

data class CatalogRequestMoreInfoCommand(
    val requestId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val message: String,
)

data class CatalogItemRequestsResult(val requests: List<CatalogItemRequest>)

data class CatalogApproveRequestAsTemplateResult(
    val request: CatalogItemRequest,
    val template: PlatformCatalogTemplate,
)
