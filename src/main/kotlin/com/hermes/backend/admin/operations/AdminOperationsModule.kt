package com.hermes.backend.admin.operations

import com.hermes.application.admin.operations.*

data class AdminOperationsModule(
    val searchSalesUseCase: SearchAdminSalesUseCase,
    val getSaleUseCase: GetAdminSaleUseCase,
    val searchCashSessionsUseCase: SearchAdminCashSessionsUseCase,
    val getCurrentCashSessionUseCase: GetCurrentAdminCashSessionUseCase,
    val getCashSessionUseCase: GetAdminCashSessionUseCase,
    val searchPaymentsUseCase: SearchAdminPaymentsUseCase,
    val searchReceivablesUseCase: SearchAdminReceivablesUseCase,
    val operationalTodayReportUseCase: GetAdminOperationalTodayReportUseCase,
    val salesSummaryReportUseCase: GetAdminSalesSummaryReportUseCase,
    val cashSummaryReportUseCase: GetAdminCashSummaryReportUseCase,
    val taxSummaryReportUseCase: GetAdminTaxSummaryReportUseCase,
)
