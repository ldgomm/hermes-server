package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.GetElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesUseCase

data class ElectronicInvoicingModule(
    val getElectronicInvoiceUseCase: GetElectronicInvoiceUseCase,
    val listElectronicInvoicesUseCase: ListElectronicInvoicesUseCase,
)
