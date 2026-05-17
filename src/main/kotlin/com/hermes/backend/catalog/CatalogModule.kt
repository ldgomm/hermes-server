package com.hermes.backend.catalog

import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationUseCase
import com.hermes.application.catalog.CatalogCreatePlatformTemplateUseCase
import com.hermes.application.catalog.CatalogDisableLocalItemUseCase
import com.hermes.application.catalog.CatalogRequestNewItemUseCase
import com.hermes.application.catalog.CatalogReviewRequestUseCase
import com.hermes.application.catalog.CatalogSearchMasterTemplatesUseCase
import com.hermes.application.catalog.CatalogSearchOrganizationItemsUseCase
import com.hermes.application.catalog.CatalogUpdateLocalItemUseCase

data class CatalogModule(
    val createPlatformTemplateUseCase: CatalogCreatePlatformTemplateUseCase,
    val searchMasterTemplatesUseCase: CatalogSearchMasterTemplatesUseCase,
    val copyTemplateToOrganizationUseCase: CatalogCopyTemplateToOrganizationUseCase,
    val searchOrganizationItemsUseCase: CatalogSearchOrganizationItemsUseCase,
    val updateLocalItemUseCase: CatalogUpdateLocalItemUseCase,
    val disableLocalItemUseCase: CatalogDisableLocalItemUseCase,
    val assignTaxProfileToCatalogItemUseCase: AssignTaxProfileToCatalogItemUseCase,
    val requestNewItemUseCase: CatalogRequestNewItemUseCase,
    val reviewRequestUseCase: CatalogReviewRequestUseCase,
)
