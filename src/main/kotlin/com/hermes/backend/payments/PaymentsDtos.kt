package com.hermes.backend.payments

import com.hermes.application.payments.CloseCashSessionCommand
import com.hermes.application.payments.CloseCashSessionResult
import com.hermes.application.payments.CreateReceivableForSaleCommand
import com.hermes.application.payments.OpenCashSessionCommand
import com.hermes.application.payments.RegisterCashMovementCommand
import com.hermes.application.payments.RegisterPaymentCommand
import com.hermes.application.payments.RegisterPaymentResult
import com.hermes.application.payments.RegisterReceivableCollectionCommand
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.cash.CashSession
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.Receivable
import com.hermes.domain.sale.Sale
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class PaymentMoneyRequest(val amount: String, val currency: String = "USD")

@Serializable
data class PaymentMoneyResponse(val amount: String, val currency: String)

@Serializable
data class OpenCashSessionRequest(
    val branchId: String,
    val openingBalance: PaymentMoneyRequest,
    val openedAt: String? = null,
    val notes: String? = null,
)

@Serializable
data class RegisterCashMovementRequest(
    val type: String,
    val direction: String,
    val amount: PaymentMoneyRequest,
    val occurredAt: String? = null,
    val referenceId: String? = null,
    val notes: String? = null,
)

@Serializable
data class CloseCashSessionRequest(
    val countedCashAmount: PaymentMoneyRequest,
    val reason: String,
    val closedAt: String? = null,
    val notes: String? = null,
)

@Serializable
data class RegisterPaymentRequest(
    val amount: PaymentMoneyRequest,
    val method: String,
    val paidAt: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val markRemainingAsReceivable: Boolean = false,
    val receivableDueAt: String? = null,
)

@Serializable
data class CreateReceivableForSaleRequest(
    val dueAt: String? = null,
    val reason: String,
)

@Serializable
data class RegisterReceivableCollectionRequest(
    val saleId: String,
    val amount: PaymentMoneyRequest,
    val method: String,
    val collectedAt: String? = null,
    val reference: String? = null,
    val notes: String? = null,
)

@Serializable
data class PaymentResponse(
    val id: String,
    val organizationId: String,
    val saleId: String,
    val amount: PaymentMoneyResponse,
    val method: String,
    val status: String,
    val paidAt: String,
    val reference: String?,
    val notes: String?,
)

@Serializable
data class CashMovementResponse(
    val id: String,
    val cashSessionId: String,
    val organizationId: String,
    val branchId: String?,
    val type: String,
    val direction: String,
    val amount: PaymentMoneyResponse,
    val occurredAt: String,
    val referenceId: String?,
    val notes: String?,
)

@Serializable
data class CashSessionResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val openedBy: String,
    val openedAt: String,
    val status: String,
    val openingBalance: PaymentMoneyResponse,
    val expectedCashAmount: PaymentMoneyResponse,
    val countedCashAmount: PaymentMoneyResponse?,
    val differenceAmount: PaymentMoneyResponse?,
    val movementCount: Int,
    val closingStartedAt: String?,
    val closedAt: String?,
    val canceledAt: String?,
)

@Serializable
data class ReceivableResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val saleId: String,
    val customerId: String?,
    val totalDue: PaymentMoneyResponse,
    val paidAmount: PaymentMoneyResponse,
    val balanceDue: PaymentMoneyResponse,
    val dueAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RegisterPaymentResponse(
    val payment: PaymentResponse,
    val saleId: String,
    val salePaymentStatus: String,
    val salePaidAmount: PaymentMoneyResponse,
    val cashSession: CashSessionResponse?,
    val cashMovement: CashMovementResponse?,
    val receivable: ReceivableResponse?,
)

fun OpenCashSessionRequest.toCommand(organizationId: String, actorUserId: String, permissions: Set<String>): OpenCashSessionCommand =
    OpenCashSessionCommand(
        organizationId = organizationId,
        branchId = branchId,
        actorUserId = actorUserId,
        actorEffectivePermissions = permissions,
        openingBalance = openingBalance.toDomain(),
        openedAt = openedAt?.let(Instant::parse),
        notes = notes,
    )

fun RegisterCashMovementRequest.toCommand(
    organizationId: String,
    cashSessionId: String,
    actorUserId: String,
    permissions: Set<String>,
): RegisterCashMovementCommand = RegisterCashMovementCommand(
    organizationId = organizationId,
    cashSessionId = cashSessionId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    type = CashMovementType.valueOf(type.trim().uppercase()),
    direction = CashMovementDirection.valueOf(direction.trim().uppercase()),
    amount = amount.toDomain(),
    occurredAt = occurredAt?.let(Instant::parse),
    referenceId = referenceId,
    notes = notes,
)

fun CloseCashSessionRequest.toCommand(
    organizationId: String,
    cashSessionId: String,
    actorUserId: String,
    permissions: Set<String>,
): CloseCashSessionCommand = CloseCashSessionCommand(
    organizationId = organizationId,
    cashSessionId = cashSessionId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    countedCashAmount = countedCashAmount.toDomain(),
    reason = reason,
    closedAt = closedAt?.let(Instant::parse),
    notes = notes,
)

fun RegisterPaymentRequest.toCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    permissions: Set<String>,
): RegisterPaymentCommand = RegisterPaymentCommand(
    organizationId = organizationId,
    saleId = saleId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    amount = amount.toDomain(),
    method = PaymentMethod.valueOf(method.trim().uppercase()),
    paidAt = paidAt?.let(Instant::parse),
    reference = reference,
    notes = notes,
    markRemainingAsReceivable = markRemainingAsReceivable,
    receivableDueAt = receivableDueAt?.let(Instant::parse),
)

fun CreateReceivableForSaleRequest.toCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    permissions: Set<String>,
): CreateReceivableForSaleCommand = CreateReceivableForSaleCommand(
    organizationId = organizationId,
    saleId = saleId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    dueAt = dueAt?.let(Instant::parse),
    reason = reason,
)

fun RegisterReceivableCollectionRequest.toCommand(
    organizationId: String,
    receivableId: String,
    actorUserId: String,
    permissions: Set<String>,
): RegisterReceivableCollectionCommand = RegisterReceivableCollectionCommand(
    organizationId = organizationId,
    receivableId = receivableId,
    saleId = saleId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    amount = amount.toDomain(),
    method = PaymentMethod.valueOf(method.trim().uppercase()),
    collectedAt = collectedAt?.let(Instant::parse),
    reference = reference,
    notes = notes,
)

fun RegisterPaymentResult.toResponse(): RegisterPaymentResponse = RegisterPaymentResponse(
    payment = payment.toResponse(),
    saleId = sale.id,
    salePaymentStatus = sale.paymentStatus.name,
    salePaidAmount = sale.paidAmount.toResponse(),
    cashSession = cashSession?.toResponse(),
    cashMovement = cashMovement?.toResponse(),
    receivable = receivable?.toResponse(),
)

fun CloseCashSessionResult.toResponse(): CashSessionResponse = cashSession.toResponse()
fun CashSession.toResponse(): CashSessionResponse = CashSessionResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    openedBy = openedBy,
    openedAt = openedAt.toString(),
    status = status.name,
    openingBalance = openingBalance.toResponse(),
    expectedCashAmount = expectedCashAmount.toResponse(),
    countedCashAmount = countedCashAmount?.toResponse(),
    differenceAmount = differenceAmount?.toResponse(),
    movementCount = movements.size,
    closingStartedAt = closingStartedAt?.toString(),
    closedAt = closedAt?.toString(),
    canceledAt = canceledAt?.toString(),
)

fun CashMovement.toResponse(): CashMovementResponse = CashMovementResponse(
    id = id,
    cashSessionId = cashSessionId,
    organizationId = organizationId,
    branchId = branchId,
    type = type.name,
    direction = direction.name,
    amount = amount.toResponse(),
    occurredAt = occurredAt.toString(),
    referenceId = referenceId,
    notes = notes,
)

fun Payment.toResponse(): PaymentResponse = PaymentResponse(
    id = id,
    organizationId = organizationId,
    saleId = saleId,
    amount = amount.toResponse(),
    method = method.name,
    status = status.name,
    paidAt = paidAt.toString(),
    reference = reference,
    notes = notes,
)

fun Receivable.toResponse(): ReceivableResponse = ReceivableResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    saleId = saleId,
    customerId = customerId,
    totalDue = totalDue.toResponse(),
    paidAmount = paidAmount.toResponse(),
    balanceDue = balanceDue.toResponse(),
    dueAt = dueAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

private fun PaymentMoneyRequest.toDomain(): Money = Money.of(BigDecimal(amount), CurrencyCode(currency.trim().uppercase()))
private fun Money.toResponse(): PaymentMoneyResponse = PaymentMoneyResponse(amount.toPlainString(), currency.value)
