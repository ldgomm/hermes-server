package com.hermes.application.sales

interface SalesReadRepository {
    fun search(command: SalesSearchCommand): List<SalesListItem>
    fun findPending(command: PendingSalesCommand): List<SalesListItem>
    fun summarizeDay(command: SalesDaySummaryCommand): SalesDaySummaryResult
}
