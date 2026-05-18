package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogApproveRequestAsTemplateUseCase
import com.hermes.application.catalog.CatalogChangeTemplateStatusUseCase
import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationUseCase
import com.hermes.application.catalog.CatalogCreateCategoryUseCase
import com.hermes.application.catalog.CatalogCreateFamilyUseCase
import com.hermes.application.catalog.CatalogCreatePlatformTemplateUseCase
import com.hermes.application.catalog.CatalogDisableLocalItemUseCase
import com.hermes.application.catalog.CatalogGetCategoryUseCase
import com.hermes.application.catalog.CatalogGetFamilyUseCase
import com.hermes.application.catalog.CatalogGetOrganizationItemUseCase
import com.hermes.application.catalog.CatalogGetTemplateUseCase
import com.hermes.application.catalog.CatalogLinkRequestToExistingTemplateUseCase
import com.hermes.application.catalog.CatalogListAdminRequestsUseCase
import com.hermes.application.catalog.CatalogListAuditEventsUseCase
import com.hermes.application.catalog.CatalogListOrganizationRequestsUseCase
import com.hermes.application.catalog.CatalogListPriceHistoryUseCase
import com.hermes.application.catalog.CatalogLookupOrganizationItemByCodeUseCase
import com.hermes.application.catalog.CatalogRejectRequestUseCase
import com.hermes.application.catalog.CatalogRemoveLocalItemUseCase
import com.hermes.application.catalog.CatalogRequestMoreInfoUseCase
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
import com.hermes.application.catalog.UuidCatalogIdGenerator
import com.hermes.infrastructure.mongo.catalog.CatalogMongoBootstrap
import com.hermes.infrastructure.mongo.catalog.MongoCatalogAuditLogger
import com.hermes.infrastructure.mongo.catalog.MongoCatalogCategoryFamilyStore
import com.hermes.infrastructure.mongo.catalog.MongoCatalogReadStore
import com.hermes.infrastructure.mongo.catalog.MongoCatalogStore
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object CatalogModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): CatalogModule {
        CatalogMongoBootstrap.ensureIndexes(database)

        val catalogStore = MongoCatalogStore(database)
        val categoryFamilyStore = MongoCatalogCategoryFamilyStore(database)
        val readStore = MongoCatalogReadStore(database)
        val taxStore = MongoTaxStore(database)
        val idGenerator = UuidCatalogIdGenerator()
        val auditLogger = MongoCatalogAuditLogger(database)

        return CatalogModule(
            createPlatformTemplateUseCase = CatalogCreatePlatformTemplateUseCase(
                templateRepository = catalogStore.templateRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchMasterTemplatesUseCase = CatalogSearchMasterTemplatesUseCase(
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            copyTemplateToOrganizationUseCase = CatalogCopyTemplateToOrganizationUseCase(
                templateRepository = catalogStore.templateRepository,
                itemRepository = catalogStore.organizationItemRepository,
                profileRepository = taxStore.profileRepository,
                settingsRepository = taxStore.settingsRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchOrganizationItemsUseCase = CatalogSearchOrganizationItemsUseCase(
                itemRepository = catalogStore.organizationItemRepository,
            ),
            updateLocalItemUseCase = CatalogUpdateLocalItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
                profileRepository = taxStore.profileRepository,
                settingsRepository = taxStore.settingsRepository,
                priceHistoryRepository = catalogStore.priceHistoryRepository,
                identifierConflictChecker = catalogStore.identifierConflictChecker,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            disableLocalItemUseCase = CatalogDisableLocalItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            assignTaxProfileToCatalogItemUseCase = com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase(
                catalogRepository = catalogStore.taxProfileRepository,
                profileRepository = taxStore.profileRepository,
                settingsRepository = taxStore.settingsRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            requestNewItemUseCase = CatalogRequestNewItemUseCase(
                requestRepository = catalogStore.requestRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            reviewRequestUseCase = CatalogReviewRequestUseCase(
                requestRepository = catalogStore.requestRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createCategoryUseCase = CatalogCreateCategoryUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            updateCategoryUseCase = CatalogUpdateCategoryUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            getCategoryUseCase = CatalogGetCategoryUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchCategoriesUseCase = CatalogSearchCategoriesUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
            ),
            createFamilyUseCase = CatalogCreateFamilyUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
                categoryRepository = categoryFamilyStore.categoryRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            updateFamilyUseCase = CatalogUpdateFamilyUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
                categoryRepository = categoryFamilyStore.categoryRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            getFamilyUseCase = CatalogGetFamilyUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchFamiliesUseCase = CatalogSearchFamiliesUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
            ),
            getTemplateUseCase = CatalogGetTemplateUseCase(
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            updateTemplateUseCase = CatalogUpdateTemplateUseCase(
                templateRepository = catalogStore.templateRepository,
                familyRepository = categoryFamilyStore.familyRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            changeTemplateStatusUseCase = CatalogChangeTemplateStatusUseCase(
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            getOrganizationItemUseCase = CatalogGetOrganizationItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
            ),
            lookupOrganizationItemByCodeUseCase = CatalogLookupOrganizationItemByCodeUseCase(
                itemRepository = catalogStore.organizationItemRepository,
            ),
            removeLocalItemUseCase = CatalogRemoveLocalItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            listOrganizationRequestsUseCase = CatalogListOrganizationRequestsUseCase(
                requestSearchRepository = catalogStore.requestSearchRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            listAdminRequestsUseCase = CatalogListAdminRequestsUseCase(
                requestSearchRepository = catalogStore.requestSearchRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            approveRequestAsTemplateUseCase = CatalogApproveRequestAsTemplateUseCase(
                requestRepository = catalogStore.requestRepository,
                templateRepository = catalogStore.templateRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            rejectRequestUseCase = CatalogRejectRequestUseCase(
                requestRepository = catalogStore.requestRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            linkRequestToExistingTemplateUseCase = CatalogLinkRequestToExistingTemplateUseCase(
                requestRepository = catalogStore.requestRepository,
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            requestMoreInfoUseCase = CatalogRequestMoreInfoUseCase(
                requestRepository = catalogStore.requestRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            listAuditEventsUseCase = CatalogListAuditEventsUseCase(
                auditRepository = readStore.auditQueryRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            listPriceHistoryUseCase = CatalogListPriceHistoryUseCase(
                priceHistoryRepository = readStore.priceHistoryQueryRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )
    }
}
