package com.hermes.backend.payments

import com.hermes.application.payments.CloseCashSessionUseCase
import com.hermes.application.payments.CreateReceivableForSaleUseCase
import com.hermes.application.payments.OpenCashSessionUseCase
import com.hermes.application.payments.RegisterCashMovementUseCase
import com.hermes.application.payments.RegisterPaymentUseCase
import com.hermes.application.payments.RegisterReceivableCollectionUseCase

data class PaymentsModule(
    val registerPaymentUseCase: RegisterPaymentUseCase,
    val openCashSessionUseCase: OpenCashSessionUseCase,
    val registerCashMovementUseCase: RegisterCashMovementUseCase,
    val closeCashSessionUseCase: CloseCashSessionUseCase,
    val createReceivableForSaleUseCase: CreateReceivableForSaleUseCase,
    val registerReceivableCollectionUseCase: RegisterReceivableCollectionUseCase,
)
