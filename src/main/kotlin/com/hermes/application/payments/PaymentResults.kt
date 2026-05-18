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

data class CashSessionResult(
    val cashSession: CashSession,
    val openingMovement: CashMovement? = null,
)

data class CashMovementResult(
    val cashSession: CashSession,
    val cashMovement: CashMovement,
)

data class CloseCashSessionResult(
    val cashSession: CashSession,
)

data class ReceivableResult(
    val receivable: Receivable,
    val sale: Sale,
)

data class RegisterReceivableCollectionResult(
    val receivable: Receivable,
    val payment: Payment,
    val sale: Sale,
    val cashSession: CashSession?,
    val cashMovement: CashMovement?,
)
