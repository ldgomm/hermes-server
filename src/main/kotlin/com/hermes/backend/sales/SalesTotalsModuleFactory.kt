package com.hermes.backend.sales

import com.hermes.application.sales.PreviewSaleTotalsUseCase
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.infrastructure.mongo.catalog.MongoCatalogStore
import com.hermes.infrastructure.mongo.tax.MongoTaxAuditLogger
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object SalesTotalsModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): SalesTotalsModule {
        val catalogStore = MongoCatalogStore(database)
        val taxStore = MongoTaxStore(database)
        val taxAuditLogger = MongoTaxAuditLogger(database)

        val taxSaleValidationUseCase = TaxSaleValidationUseCase(
            profileRepository = taxStore.profileRepository,
            settingsRepository = taxStore.settingsRepository,
            auditLogger = taxAuditLogger,
            clock = clock,
        )

        return SalesTotalsModule(
            previewSaleTotalsUseCase = PreviewSaleTotalsUseCase(
                catalogRepository = catalogStore.organizationItemRepository,
                taxProfileRepository = taxStore.profileRepository,
                settingsRepository = taxStore.settingsRepository,
                taxSaleValidationUseCase = taxSaleValidationUseCase,
            )
        )
    }
}
