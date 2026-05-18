package com.hermes.application.payments

import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.PaymentMethod
import java.time.Instant

data class RegisterPaymentCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: Instant? = null,
    val reference: String? = null,
    val notes: String? = null,
    val markRemainingAsReceivable: Boolean = false,
    val receivableDueAt: Instant? = null,
)

data class OpenCashSessionCommand(
    val organizationId: String,
    val branchId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val openingBalance: Money,
    val openedAt: Instant? = null,
    val notes: String? = null,
)

data class RegisterCashMovementCommand(
    val organizationId: String,
    val cashSessionId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val type: CashMovementType,
    val direction: CashMovementDirection,
    val amount: Money,
    val occurredAt: Instant? = null,
    val referenceId: String? = null,
    val notes: String? = null,
)

data class CloseCashSessionCommand(
    val organizationId: String,
    val cashSessionId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val countedCashAmount: Money,
    val reason: String,
    val closedAt: Instant? = null,
    val notes: String? = null,
)

data class CreateReceivableForSaleCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val dueAt: Instant? = null,
    val reason: String,
)

data class RegisterReceivableCollectionCommand(
    val organizationId: String,
    val receivableId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val amount: Money,
    val method: PaymentMethod,
    val collectedAt: Instant? = null,
    val reference: String? = null,
    val notes: String? = null,
)
