package com.hermes.backend.catalog

import com.hermes.application.catalog.CatalogCopyTemplateToOrganizationUseCase
import com.hermes.application.catalog.CatalogCreatePlatformTemplateUseCase
import com.hermes.application.catalog.CatalogDisableLocalItemUseCase
import com.hermes.application.catalog.CatalogRequestNewItemUseCase
import com.hermes.application.catalog.CatalogReviewRequestUseCase
import com.hermes.application.catalog.CatalogSearchMasterTemplatesUseCase
import com.hermes.application.catalog.CatalogSearchOrganizationItemsUseCase
import com.hermes.application.catalog.CatalogUpdateLocalItemUseCase
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.catalog.UuidCatalogIdGenerator
import com.hermes.infrastructure.mongo.catalog.CatalogMongoBootstrap
import com.hermes.infrastructure.mongo.catalog.MongoCatalogAuditLogger
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
            assignTaxProfileToCatalogItemUseCase = AssignTaxProfileToCatalogItemUseCase(
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
        )
    }
}
