package com.hermes.backend.admin.tax

import com.hermes.application.admin.tax.GetAdminTaxReadinessUseCase
import com.hermes.application.admin.tax.SearchAdminTaxProfilesUseCase
import com.hermes.application.admin.tax.SearchAdminTaxRatesUseCase
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.tax.*
import com.hermes.infrastructure.mongo.catalog.CatalogMongoBootstrap
import com.hermes.infrastructure.mongo.catalog.MongoCatalogAuditLogger
import com.hermes.infrastructure.mongo.catalog.MongoCatalogStore
import com.hermes.infrastructure.mongo.tax.MongoAdminTaxProfileQueryRepository
import com.hermes.infrastructure.mongo.tax.MongoAdminTaxRateQueryRepository
import com.hermes.infrastructure.mongo.tax.MongoTaxAuditLogger
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminTaxModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): AdminTaxModule {
        CatalogMongoBootstrap.ensureIndexes(database)

        val taxStore = MongoTaxStore(database)
        val catalogStore = MongoCatalogStore(database)
        val taxAuditLogger = MongoTaxAuditLogger(database)
        val catalogAuditLogger = MongoCatalogAuditLogger(database)
        val idGenerator = UuidTaxIdGenerator()

        return AdminTaxModule(
            searchRatesUseCase = SearchAdminTaxRatesUseCase(
                repository = MongoAdminTaxRateQueryRepository(database),
            ),
            getRateUseCase = TaxGetRateUseCase(
                rateRepository = taxStore.rateRepository,
            ),
            createRateUseCase = TaxCreateRateUseCase(
                rateRepository = taxStore.rateRepository,
                idGenerator = idGenerator,
                auditLogger = taxAuditLogger,
                clock = clock,
            ),
            updateRateUseCase = TaxUpdateRateUseCase(
                rateRepository = taxStore.rateRepository,
                auditLogger = taxAuditLogger,
                clock = clock,
            ),

            searchProfilesUseCase = SearchAdminTaxProfilesUseCase(
                repository = MongoAdminTaxProfileQueryRepository(database),
            ),
            getProfileUseCase = TaxGetProfileUseCase(
                profileRepository = taxStore.profileRepository,
            ),
            createProfileUseCase = TaxCreateProfileUseCase(
                profileRepository = taxStore.profileRepository,
                rateRepository = taxStore.rateRepository,
                idGenerator = idGenerator,
                auditLogger = taxAuditLogger,
                clock = clock,
            ),
            updateProfileUseCase = TaxUpdateProfileUseCase(
                profileRepository = taxStore.profileRepository,
                rateRepository = taxStore.rateRepository,
                auditLogger = taxAuditLogger,
                clock = clock,
            ),
            assignTaxProfileToCatalogItemUseCase = AssignTaxProfileToCatalogItemUseCase(
                catalogRepository = catalogStore.taxProfileRepository,
                profileRepository = taxStore.profileRepository,
                settingsRepository = taxStore.settingsRepository,
                auditLogger = catalogAuditLogger,
                clock = clock,
            ),
            readinessUseCase = GetAdminTaxReadinessUseCase(
                settingsRepository = taxStore.settingsRepository,
                profileRepository = taxStore.profileRepository,
                clock = clock,
            ),
        )
    }
}
