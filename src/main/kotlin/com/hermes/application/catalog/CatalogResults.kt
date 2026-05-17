package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogTemplate

data class CatalogTemplateResult(val template: PlatformCatalogTemplate)
data class CatalogTemplatesResult(val templates: List<PlatformCatalogTemplate>)
data class OrganizationCatalogItemResult(val item: OrganizationCatalogItem)
data class OrganizationCatalogItemsResult(val items: List<OrganizationCatalogItem>)
data class CatalogItemRequestResult(val request: CatalogItemRequest)
data class AssignTaxProfileToCatalogItemResult(val assignment: CatalogTaxProfileAssignmentRecord)
