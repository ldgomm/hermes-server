package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogItemRequestDecision
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money

data class CatalogCreatePlatformTemplateCommand(
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val globalCatalogId: String,
    val canonicalName: String,
    val type: CatalogItemType,
    val productFamilyId: String? = null,
    val variantAttributes: Map<String, String> = emptyMap(),
    val identifiers: List<CatalogIdentifier> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val reason: String? = null,
)

data class CatalogSearchMasterTemplatesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val identifier: String? = null,
    val type: CatalogItemType? = null,
    val limit: Int = 50,
)

data class CatalogCopyTemplateToOrganizationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val templateId: String,
    val branchId: String? = null,
    val activityId: String,
    val localPrice: Money,
    val taxProfileCode: String,
    val reason: String,
)

data class CatalogSearchOrganizationItemsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val query: String? = null,
    val identifier: String? = null,
    val type: CatalogItemType? = null,
    val statuses: Set<CatalogItemStatus> = emptySet(),
    val limit: Int = 50,
)

data class CatalogUpdateLocalItemCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val catalogItemId: String,
    val localName: String? = null,
    val localPrice: Money? = null,
    val taxProfileCode: String? = null,
    val identifiers: List<CatalogIdentifier>? = null,
    val status: CatalogItemStatus? = null,
    val reason: String,
)

data class CatalogDisableLocalItemCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val catalogItemId: String,
    val reason: String,
)

data class AssignTaxProfileToCatalogItemCommand(
    val organizationId: String,
    val catalogItemId: String,
    val taxProfileCode: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

data class CatalogRequestNewItemCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val requestedName: String,
    val requestedType: CatalogItemType,
    val description: String? = null,
    val suggestedCategoryId: String? = null,
    val suggestedTaxProfileCode: String? = null,
    val identifiers: List<CatalogIdentifier> = emptyList(),
)

data class CatalogReviewRequestCommand(
    val requestId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val decision: CatalogItemRequestDecision,
    val reason: String,
)
