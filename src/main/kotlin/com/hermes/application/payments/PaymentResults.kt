package com.hermes.application.payments

import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashSession
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable
import com.hermes.domain.sale.Sale

data class RegisterPaymentResult(
    val payment: Payment,
    val sale: Sale,
    val cashSession: CashSession?,
    val cashMovement: CashMovement?,
    val receivable: Receivable?,
)
