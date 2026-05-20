package com.hermes.backend.admin.operations

import com.hermes.application.admin.operations.*
import com.hermes.infrastructure.mongo.admin.operations.MongoAdminOperationsQueryRepository
import com.mongodb.client.MongoDatabase

object AdminOperationsModuleFactory {
    fun fromMongo(database: MongoDatabase): AdminOperationsModule {
        val queryRepository = MongoAdminOperationsQueryRepository(database)
        return AdminOperationsModule(
            searchSalesUseCase = SearchAdminSalesUseCase(queryRepository),
            getSaleUseCase = GetAdminSaleUseCase(queryRepository),
            searchCashSessionsUseCase = SearchAdminCashSessionsUseCase(queryRepository),
            getCurrentCashSessionUseCase = GetCurrentAdminCashSessionUseCase(queryRepository),
            getCashSessionUseCase = GetAdminCashSessionUseCase(queryRepository),
            searchPaymentsUseCase = SearchAdminPaymentsUseCase(queryRepository),
            searchReceivablesUseCase = SearchAdminReceivablesUseCase(queryRepository),
            operationalTodayReportUseCase = GetAdminOperationalTodayReportUseCase(queryRepository),
            salesSummaryReportUseCase = GetAdminSalesSummaryReportUseCase(queryRepository),
            cashSummaryReportUseCase = GetAdminCashSummaryReportUseCase(queryRepository),
            taxSummaryReportUseCase = GetAdminTaxSummaryReportUseCase(queryRepository),
        )
    }
}
