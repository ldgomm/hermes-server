package com.hermes.backend.sales

import com.hermes.application.sales.AddSaleItemUseCase
import com.hermes.application.sales.CancelSaleUseCase
import com.hermes.application.sales.ChangeSaleItemStatusUseCase
import com.hermes.application.sales.ChangeSaleStatusUseCase
import com.hermes.application.sales.CloseSaleUseCase
import com.hermes.application.sales.CreateQuickSaleUseCase
import com.hermes.application.sales.CreateReservationUseCase
import com.hermes.application.sales.GetSaleUseCase
import com.hermes.application.sales.SaleItemPreparationService
import com.hermes.application.sales.SearchReservationsUseCase
import com.hermes.application.sales.SearchSalesUseCase
import com.hermes.application.sales.UuidSalesIdGenerator
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.infrastructure.mongo.catalog.MongoCatalogStore
import com.hermes.infrastructure.mongo.sales.MongoSalesAuditLogger
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.hermes.infrastructure.mongo.tax.MongoTaxAuditLogger
import com.hermes.infrastructure.mongo.tax.MongoTaxStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object SalesModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): SalesModule {
        val salesStore = MongoSalesStore(database)
        val catalogStore = MongoCatalogStore(database)
        val taxStore = MongoTaxStore(database)
        val salesAuditLogger = MongoSalesAuditLogger(database)
        val taxAuditLogger = MongoTaxAuditLogger(database)
        val idGenerator = UuidSalesIdGenerator()

        val taxSaleValidationUseCase = TaxSaleValidationUseCase(
            profileRepository = taxStore.profileRepository,
            settingsRepository = taxStore.settingsRepository,
            auditLogger = taxAuditLogger,
            clock = clock,
        )

        val saleItemPreparationService = SaleItemPreparationService(
            catalogRepository = catalogStore.organizationItemRepository,
            taxProfileRepository = taxStore.profileRepository,
            settingsRepository = taxStore.settingsRepository,
            taxSaleValidationUseCase = taxSaleValidationUseCase,
            idGenerator = idGenerator,
        )

        val createQuickSaleUseCase = CreateQuickSaleUseCase(
            saleRepository = salesStore.saleRepository,
            saleItemPreparationService = saleItemPreparationService,
            idGenerator = idGenerator,
            auditLogger = salesAuditLogger,
            clock = clock,
        )

        val addSaleItemUseCase = AddSaleItemUseCase(
            saleRepository = salesStore.saleRepository,
            saleItemPreparationService = saleItemPreparationService,
            auditLogger = salesAuditLogger,
            clock = clock,
        )

        val changeSaleStatusUseCase = ChangeSaleStatusUseCase(
            saleRepository = salesStore.saleRepository,
            auditLogger = salesAuditLogger,
            clock = clock,
        )

        return SalesModule(
            createQuickSaleUseCase = createQuickSaleUseCase,
            addSaleItemUseCase = addSaleItemUseCase,
            getSaleUseCase = GetSaleUseCase(
                saleRepository = salesStore.saleRepository,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
            searchSalesUseCase = SearchSalesUseCase(
                saleRepository = salesStore.saleRepository,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
            changeSaleStatusUseCase = changeSaleStatusUseCase,
            changeSaleItemStatusUseCase = ChangeSaleItemStatusUseCase(
                saleRepository = salesStore.saleRepository,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
            cancelSaleUseCase = CancelSaleUseCase(
                saleRepository = salesStore.saleRepository,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
            closeSaleUseCase = CloseSaleUseCase(changeSaleStatusUseCase),
            createReservationUseCase = CreateReservationUseCase(
                reservationRepository = salesStore.reservationRepository,
                createQuickSaleUseCase = createQuickSaleUseCase,
                idGenerator = idGenerator,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
            searchReservationsUseCase = SearchReservationsUseCase(
                reservationRepository = salesStore.reservationRepository,
                auditLogger = salesAuditLogger,
                clock = clock,
            ),
        )
    }
}
