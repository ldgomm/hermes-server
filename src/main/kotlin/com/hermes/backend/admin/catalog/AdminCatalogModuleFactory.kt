package com.hermes.backend.admin.catalog

import com.hermes.application.admin.catalog.ChangeAdminCatalogLocalItemStatusUseCase
import com.hermes.application.admin.catalog.GetAdminCatalogRequestUseCase
import com.hermes.application.admin.catalog.SearchAdminCatalogMasterTemplatesUseCase
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
import com.hermes.application.catalog.CatalogSearchOrganizationItemsUseCase
import com.hermes.application.catalog.CatalogUpdateLocalItemUseCase
import com.hermes.application.catalog.UuidCatalogIdGenerator
import com.hermes.infrastructure.mongo.catalog.CatalogMongoBootstrap
import com.hermes.infrastructure.mongo.catalog.MongoCatalogCategoryFamilyStore
import com.hermes.infrastructure.mongo.catalog.MongoCatalogAuditLogger
import com.hermes.infrastructure.mongo.catalog.MongoCatalogStore
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminCatalogModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): AdminCatalogModule {
        CatalogMongoBootstrap.ensureIndexes(database)

        val catalogStore = MongoCatalogStore(database)
        val categoryFamilyStore = MongoCatalogCategoryFamilyStore(database)
        val taxStore = MongoTaxStore(database)
        val idGenerator = UuidCatalogIdGenerator()
        val auditLogger = MongoCatalogAuditLogger(database)

        return AdminCatalogModule(
            searchMasterTemplatesUseCase = SearchAdminCatalogMasterTemplatesUseCase(
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            getTemplateUseCase = CatalogGetTemplateUseCase(
                templateRepository = catalogStore.templateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createPlatformTemplateUseCase = CatalogCreatePlatformTemplateUseCase(
                templateRepository = catalogStore.templateRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchCategoriesUseCase = CatalogSearchCategoriesUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
            ),
            createCategoryUseCase = CatalogCreateCategoryUseCase(
                categoryRepository = categoryFamilyStore.categoryRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchFamiliesUseCase = CatalogSearchFamiliesUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
            ),
            createFamilyUseCase = CatalogCreateFamilyUseCase(
                familyRepository = categoryFamilyStore.familyRepository,
                categoryRepository = categoryFamilyStore.categoryRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            searchOrganizationItemsUseCase = CatalogSearchOrganizationItemsUseCase(
                itemRepository = catalogStore.organizationItemRepository,
            ),
            getOrganizationItemUseCase = CatalogGetOrganizationItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
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
            changeLocalItemStatusUseCase = ChangeAdminCatalogLocalItemStatusUseCase(
                itemRepository = catalogStore.organizationItemRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            removeLocalItemUseCase = CatalogRemoveLocalItemUseCase(
                itemRepository = catalogStore.organizationItemRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            requestNewItemUseCase = CatalogRequestNewItemUseCase(
                requestRepository = catalogStore.requestRepository,
                idGenerator = idGenerator,
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
            getRequestUseCase = GetAdminCatalogRequestUseCase(
                requestRepository = catalogStore.requestRepository,
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
        )
    }
}
