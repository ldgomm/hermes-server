package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogTemplate
import java.time.Instant

interface PlatformCatalogTemplateRepository {
    fun create(template: PlatformCatalogTemplate)
    fun update(template: PlatformCatalogTemplate)
    fun findById(id: String): PlatformCatalogTemplate?
    fun existsByGlobalCatalogId(globalCatalogId: String): Boolean
    fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate>
}

data class CatalogTemplateSearchQuery(
    val query: String? = null,
    val identifier: String? = null,
    val type: CatalogItemType? = null,
    val onlyActive: Boolean = true,
    val limit: Int = 50,
)

interface OrganizationCatalogItemRepository {
    fun create(item: OrganizationCatalogItem)
    fun update(item: OrganizationCatalogItem)
    fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem?
    fun existsByTemplateId(organizationId: String, templateId: String): Boolean
    fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem>
}

data class OrganizationCatalogSearchQuery(
    val organizationId: String,
    val query: String? = null,
    val identifier: String? = null,
    val type: CatalogItemType? = null,
    val statuses: Set<CatalogItemStatus> = emptySet(),
    val limit: Int = 50,
)

interface CatalogIdentifierConflictChecker {
    fun existsLocalIdentifier(
        organizationId: String,
        normalizedValue: String,
        excludeCatalogItemId: String? = null,
    ): Boolean
}

interface CatalogItemRequestRepository {
    fun create(request: CatalogItemRequest)
    fun update(request: CatalogItemRequest)
    fun findById(requestId: String): CatalogItemRequest?
    fun findPendingByOrganizationAndName(organizationId: String, requestedName: String): CatalogItemRequest?
}

interface CatalogPriceHistoryRepository {
    fun create(history: CatalogPriceHistory)
}

interface OrganizationCatalogTaxProfileRepository {
    fun assignTaxProfile(
        organizationId: String,
        catalogItemId: String,
        taxProfileId: String,
        updatedAt: Instant,
    ): CatalogTaxProfileAssignmentRecord
}

data class CatalogTaxProfileAssignmentRecord(
    val organizationId: String,
    val catalogItemId: String,
    val previousTaxProfileId: String?,
    val taxProfileId: String,
    val updatedAt: Instant,
)

fun List<CatalogIdentifier>.searchableIdentifierText(): String =
    joinToString(" ") { it.normalizedValue }
