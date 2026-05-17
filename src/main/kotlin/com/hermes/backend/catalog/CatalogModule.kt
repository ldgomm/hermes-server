package com.hermes.backend.catalog

import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.catalog.CatalogChangeTemplateStatusUseCase
import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationUseCase
import com.hermes.application.catalog.CatalogCreateCategoryUseCase
import com.hermes.application.catalog.CatalogCreateFamilyUseCase
import com.hermes.application.catalog.CatalogCreatePlatformTemplateUseCase
import com.hermes.application.catalog.CatalogDisableLocalItemUseCase
import com.hermes.application.catalog.CatalogGetCategoryUseCase
import com.hermes.application.catalog.CatalogGetOrganizationItemUseCase
import com.hermes.application.catalog.CatalogGetFamilyUseCase
import com.hermes.application.catalog.CatalogGetTemplateUseCase
import com.hermes.application.catalog.CatalogLookupOrganizationItemByCodeUseCase
import com.hermes.application.catalog.CatalogRemoveLocalItemUseCase
import com.hermes.application.catalog.CatalogRequestNewItemUseCase
import com.hermes.application.catalog.CatalogReviewRequestUseCase
import com.hermes.application.catalog.CatalogSearchCategoriesUseCase
import com.hermes.application.catalog.CatalogSearchFamiliesUseCase
import com.hermes.application.catalog.CatalogSearchMasterTemplatesUseCase
import com.hermes.application.catalog.CatalogSearchOrganizationItemsUseCase
import com.hermes.application.catalog.CatalogUpdateCategoryUseCase
import com.hermes.application.catalog.CatalogUpdateFamilyUseCase
import com.hermes.application.catalog.CatalogUpdateLocalItemUseCase
import com.hermes.application.catalog.CatalogUpdateTemplateUseCase

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
    val createCategoryUseCase: CatalogCreateCategoryUseCase,
    val updateCategoryUseCase: CatalogUpdateCategoryUseCase,
    val getCategoryUseCase: CatalogGetCategoryUseCase,
    val searchCategoriesUseCase: CatalogSearchCategoriesUseCase,
    val createFamilyUseCase: CatalogCreateFamilyUseCase,
    val updateFamilyUseCase: CatalogUpdateFamilyUseCase,
    val getFamilyUseCase: CatalogGetFamilyUseCase,
    val searchFamiliesUseCase: CatalogSearchFamiliesUseCase,
    val getTemplateUseCase: CatalogGetTemplateUseCase,
    val updateTemplateUseCase: CatalogUpdateTemplateUseCase,
    val changeTemplateStatusUseCase: CatalogChangeTemplateStatusUseCase,
    val getOrganizationItemUseCase: CatalogGetOrganizationItemUseCase,
    val lookupOrganizationItemByCodeUseCase: CatalogLookupOrganizationItemByCodeUseCase,
    val removeLocalItemUseCase: CatalogRemoveLocalItemUseCase,
)
