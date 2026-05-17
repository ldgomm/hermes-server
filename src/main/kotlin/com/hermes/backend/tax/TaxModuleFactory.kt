package com.hermes.backend.tax

import com.hermes.application.tax.*
import com.hermes.infrastructure.mongo.tax.MongoTaxAuditLogger
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object TaxModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): TaxModule {
        val store = MongoTaxStore(database)
        val auditLogger = MongoTaxAuditLogger(database)
        val idGenerator = UuidTaxIdGenerator()

        return TaxModule(
            listActiveRatesUseCase = TaxListActiveRatesUseCase(
                rateRepository = store.rateRepository,
            ),
            getRateUseCase = TaxGetRateUseCase(
                rateRepository = store.rateRepository,
            ),
            createRateUseCase = TaxCreateRateUseCase(
                rateRepository = store.rateRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            updateRateUseCase = TaxUpdateRateUseCase(
                rateRepository = store.rateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),

            listActiveProfilesUseCase = TaxListActiveProfilesUseCase(
                profileRepository = store.profileRepository,
            ),
            getProfileUseCase = TaxGetProfileUseCase(
                profileRepository = store.profileRepository,
            ),
            createProfileUseCase = TaxCreateProfileUseCase(
                profileRepository = store.profileRepository,
                rateRepository = store.rateRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            updateProfileUseCase = TaxUpdateProfileUseCase(
                profileRepository = store.profileRepository,
                rateRepository = store.rateRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),

            getOrganizationSettingsUseCase = TaxGetOrganizationSettingsUseCase(
                settingsRepository = store.settingsRepository,
            ),
            updateOrganizationSettingsUseCase = TaxUpdateOrganizationSettingsUseCase(
                settingsRepository = store.settingsRepository,
                profileRepository = store.profileRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),

            calculatePreviewUseCase = TaxCalculatePreviewUseCase(
                profileRepository = store.profileRepository,
                settingsRepository = store.settingsRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )
    }
}