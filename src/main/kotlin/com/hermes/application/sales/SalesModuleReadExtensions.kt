package com.hermes.application.sales

data class SalesReadUseCases(
    val searchSalesReadUseCase: SearchSalesReadUseCase,
    val listPendingSalesUseCase: ListPendingSalesUseCase,
    val getSalesDaySummaryUseCase: GetSalesDaySummaryUseCase,
)

object SalesReadUseCasesFactory {
    fun from(repository: SalesReadRepository): SalesReadUseCases =
        SalesReadUseCases(
            searchSalesReadUseCase = SearchSalesReadUseCase(repository),
            listPendingSalesUseCase = ListPendingSalesUseCase(repository),
            getSalesDaySummaryUseCase = GetSalesDaySummaryUseCase(repository),
        )
}
