package com.hermes.backend.admin.catalog

import com.hermes.application.admin.catalog.ChangeAdminCatalogLocalItemStatusUseCase
import com.hermes.application.admin.catalog.GetAdminCatalogRequestUseCase
import com.hermes.application.catalog.CatalogApproveRequestAsTemplateUseCase
import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationUseCase
import com.hermes.application.catalog.CatalogCreateCategoryUseCase
import com.hermes.application.catalog.CatalogCreateFamilyUseCase
import com.hermes.application.catalog.CatalogCreatePlatformTemplateUseCase
import com.hermes.application.catalog.CatalogGetOrganizationItemUseCase
import com.hermes.application.catalog.CatalogGetTemplateUseCase
import com.hermes.application.catalog.CatalogLinkRequestToExistingTemplateUseCase
import com.hermes.application.catalog.CatalogListAdminRequestsUseCase
import com.hermes.application.catalog.CatalogListOrganizationRequestsUseCase
import com.hermes.application.catalog.CatalogRejectRequestUseCase
import com.hermes.application.catalog.CatalogRemoveLocalItemUseCase
import com.hermes.application.catalog.CatalogRequestMoreInfoUseCase
import com.hermes.application.catalog.CatalogRequestNewItemUseCase
import com.hermes.application.catalog.CatalogSearchCategoriesUseCase
import com.hermes.application.catalog.CatalogSearchFamiliesUseCase
import com.hermes.application.admin.catalog.SearchAdminCatalogMasterTemplatesUseCase
import com.hermes.application.catalog.CatalogSearchOrganizationItemsUseCase
import com.hermes.application.catalog.CatalogUpdateLocalItemUseCase

/**
 * Fase 13C — Admin Catalog API module.
 *
 * It intentionally reuses the already-tested catalog application use cases from
 * Fase 7 and only adds the missing admin detail flow for catalog requests. This
 * keeps the Admin API thin: routing + DTOs + permissions + organization scope.
 */
data class AdminCatalogModule(
    val searchMasterTemplatesUseCase: SearchAdminCatalogMasterTemplatesUseCase,
    val getTemplateUseCase: CatalogGetTemplateUseCase,
    val createPlatformTemplateUseCase: CatalogCreatePlatformTemplateUseCase,

    val searchCategoriesUseCase: CatalogSearchCategoriesUseCase,
    val createCategoryUseCase: CatalogCreateCategoryUseCase,
    val searchFamiliesUseCase: CatalogSearchFamiliesUseCase,
    val createFamilyUseCase: CatalogCreateFamilyUseCase,

    val searchOrganizationItemsUseCase: CatalogSearchOrganizationItemsUseCase,
    val getOrganizationItemUseCase: CatalogGetOrganizationItemUseCase,
    val copyTemplateToOrganizationUseCase: CatalogCopyTemplateToOrganizationUseCase,
    val updateLocalItemUseCase: CatalogUpdateLocalItemUseCase,
    val changeLocalItemStatusUseCase: ChangeAdminCatalogLocalItemStatusUseCase,
    val removeLocalItemUseCase: CatalogRemoveLocalItemUseCase,

    val requestNewItemUseCase: CatalogRequestNewItemUseCase,
    val listOrganizationRequestsUseCase: CatalogListOrganizationRequestsUseCase,
    val listAdminRequestsUseCase: CatalogListAdminRequestsUseCase,
    val getRequestUseCase: GetAdminCatalogRequestUseCase,
    val approveRequestAsTemplateUseCase: CatalogApproveRequestAsTemplateUseCase,
    val rejectRequestUseCase: CatalogRejectRequestUseCase,
    val linkRequestToExistingTemplateUseCase: CatalogLinkRequestToExistingTemplateUseCase,
    val requestMoreInfoUseCase: CatalogRequestMoreInfoUseCase,
)
