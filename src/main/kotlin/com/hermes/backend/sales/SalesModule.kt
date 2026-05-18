package com.hermes.backend.sales

import com.hermes.application.sales.AddSaleItemUseCase
import com.hermes.application.sales.CancelSaleUseCase
import com.hermes.application.sales.ChangeSaleItemStatusUseCase
import com.hermes.application.sales.ChangeSaleStatusUseCase
import com.hermes.application.sales.CloseSaleUseCase
import com.hermes.application.sales.CreateQuickSaleUseCase
import com.hermes.application.sales.CreateReservationUseCase
import com.hermes.application.sales.GetSaleUseCase
import com.hermes.application.sales.SearchReservationsUseCase
import com.hermes.application.sales.SearchSalesUseCase

data class SalesModule(
    val createQuickSaleUseCase: CreateQuickSaleUseCase,
    val addSaleItemUseCase: AddSaleItemUseCase,
    val getSaleUseCase: GetSaleUseCase,
    val searchSalesUseCase: SearchSalesUseCase,
    val changeSaleStatusUseCase: ChangeSaleStatusUseCase,
    val changeSaleItemStatusUseCase: ChangeSaleItemStatusUseCase,
    val cancelSaleUseCase: CancelSaleUseCase,
    val closeSaleUseCase: CloseSaleUseCase,
    val createReservationUseCase: CreateReservationUseCase,
    val searchReservationsUseCase: SearchReservationsUseCase,
)
