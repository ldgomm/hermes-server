package com.hermes.application.admin.operations

interface AdminOperationsQueryRepository {
    fun searchSales(command: SearchAdminSalesCommand): List<AdminSaleListItem>
    fun findSale(command: GetAdminSaleCommand): AdminSaleDetail?

    fun searchCashSessions(command: SearchAdminCashSessionsCommand): List<AdminCashSessionReadModel>
    fun findCurrentCashSession(command: GetCurrentAdminCashSessionCommand): AdminCashSessionReadModel?
    fun findCashSession(command: GetAdminCashSessionCommand): AdminCashSessionReadModel?

    fun searchPayments(command: SearchAdminPaymentsCommand): List<AdminPaymentReadModel>
    fun searchReceivables(command: SearchAdminReceivablesCommand): List<AdminReceivableReadModel>

    fun operationalToday(command: GetAdminOperationalTodayReportCommand): AdminOperationalTodayReport
    fun salesSummary(command: GetAdminSalesSummaryReportCommand): AdminSalesSummaryReport
    fun cashSummary(command: GetAdminCashSummaryReportCommand): AdminCashSummaryReport
    fun taxSummary(command: GetAdminTaxSummaryReportCommand): AdminTaxSummaryReport
}
