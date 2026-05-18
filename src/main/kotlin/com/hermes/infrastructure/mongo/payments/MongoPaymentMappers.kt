package com.hermes.infrastructure.mongo.payments

import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.cash.CashSession
import com.hermes.domain.cash.CashSessionStatus
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentLifecycleStatus
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.Receivable
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document

internal object MongoPaymentMappers {
    fun paymentToDocument(payment: Payment, branchId: String?, cashSessionId: String? = null, customerId: String? = null): Document =
        Document(MongoDocumentFields.ID, payment.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, payment.organizationId)
            .append("branchId", branchId)
            .append("saleId", payment.saleId)
            .append("customerId", customerId)
            .append("cashSessionId", cashSessionId)
            .append("method", payment.method.toStorage())
            .append("status", payment.status.toStorage())
            .append("amount", moneyToDocument(payment.amount))
            .append("paidAt", MongoInstantMapper.toDate(payment.paidAt))
            .append("externalReference", payment.reference)
            .append("notes", payment.notes)
            .append("allocations", listOf(Document("saleId", payment.saleId).append("amount", moneyToDocument(payment.amount))))
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(payment.paidAt))
            .append(MongoDocumentFields.CREATED_BY, null)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(payment.paidAt))
            .append(MongoDocumentFields.UPDATED_BY, null)
            .append(MongoDocumentFields.VERSION, 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun paymentFromDocument(document: Document): Payment =
        Payment.restore(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            saleId = document.requiredString("saleId"),
            amount = moneyFromDocument(document.requiredDocument("amount")),
            method = paymentMethodFromStorage(document.requiredString("method")),
            status = paymentLifecycleStatusFromStorage(document.requiredString("status")),
            paidAt = MongoInstantMapper.readRequired(document, "paidAt"),
            reference = document.optionalString("externalReference"),
            notes = document.optionalString("notes"),
        )

    fun cashSessionToDocument(session: CashSession): Document =
        Document(MongoDocumentFields.ID, session.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, session.organizationId)
            .append("branchId", session.branchId)
            .append("openedBy", session.openedBy)
            .append("openedAt", MongoInstantMapper.toDate(session.openedAt))
            .append("status", session.status.name.lowercase())
            .append("openingBalance", moneyToDocument(session.openingBalance))
            .append("expectedCashAmount", moneyToDocument(session.expectedCashAmount))
            .append("countedCashAmount", moneyToDocument(session.countedCashAmount ?: Money.zero(session.openingBalance.currency)))
            .append("differenceAmount", moneyToDocument(session.differenceAmount ?: Money.zero(session.openingBalance.currency)))
            .append("closingStartedAt", session.closingStartedAt?.let(MongoInstantMapper::toDate))
            .append("closedAt", session.closedAt?.let(MongoInstantMapper::toDate))
            .append("canceledAt", session.canceledAt?.let(MongoInstantMapper::toDate))
            .append("summary", Document("movementCount", session.movements.size).append("closingNotes", session.closingNotes))
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(session.openedAt))
            .append(MongoDocumentFields.CREATED_BY, session.openedBy)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(session.closedAt ?: session.closingStartedAt ?: session.openedAt))
            .append(MongoDocumentFields.UPDATED_BY, session.openedBy)
            .append(MongoDocumentFields.VERSION, 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun cashSessionFromDocument(document: Document, movements: List<CashMovement>): CashSession =
        CashSession.restore(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.optionalString("branchId"),
            openedBy = document.requiredString("openedBy"),
            openedAt = MongoInstantMapper.readRequired(document, "openedAt"),
            status = enumValueOf<CashSessionStatus>(document.requiredString("status").uppercase()),
            openingBalance = moneyFromDocument(document.requiredDocument("openingBalance")),
            movements = movements,
            closingStartedAt = MongoInstantMapper.readOptional(document, "closingStartedAt"),
            closedAt = MongoInstantMapper.readOptional(document, "closedAt"),
            canceledAt = MongoInstantMapper.readOptional(document, "canceledAt"),
            countedCashAmount = document.optionalDocument("countedCashAmount")?.let(::moneyFromDocument),
            differenceAmount = document.optionalDocument("differenceAmount")?.let(::moneyFromDocument),
            closingNotes = document.optionalDocument("summary")?.optionalString("closingNotes"),
        )

    fun cashMovementToDocument(movement: CashMovement): Document =
        Document(MongoDocumentFields.ID, movement.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, movement.organizationId)
            .append("cashSessionId", movement.cashSessionId)
            .append("branchId", movement.branchId)
            .append("type", movement.type.name.lowercase())
            .append("direction", movement.direction.name.lowercase())
            .append("amount", moneyToDocument(movement.amount))
            .append("occurredAt", MongoInstantMapper.toDate(movement.occurredAt))
            .append("referenceType", movement.referenceId?.let { inferReferenceType(movement) })
            .append("referenceId", movement.referenceId)
            .append("notes", movement.notes)
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(movement.occurredAt))
            .append(MongoDocumentFields.CREATED_BY, null)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(movement.occurredAt))
            .append(MongoDocumentFields.UPDATED_BY, null)
            .append(MongoDocumentFields.VERSION, 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun cashMovementFromDocument(document: Document): CashMovement =
        CashMovement.restore(
            id = document.requiredString(MongoDocumentFields.ID),
            cashSessionId = document.requiredString("cashSessionId"),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.optionalString("branchId"),
            type = cashMovementTypeFromStorage(document.requiredString("type")),
            direction = enumValueOf<CashMovementDirection>(document.requiredString("direction").uppercase()),
            amount = moneyFromDocument(document.requiredDocument("amount")),
            occurredAt = MongoInstantMapper.readRequired(document, "occurredAt"),
            referenceId = document.optionalString("referenceId"),
            notes = document.optionalString("notes"),
        )

    fun receivableToDocument(receivable: Receivable): Document =
        Document(MongoDocumentFields.ID, receivable.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, receivable.organizationId)
            .append("branchId", receivable.branchId)
            .append("saleId", receivable.saleId)
            .append("customerId", receivable.customerId)
            .append("status", receivable.status(receivable.updatedAt).toReceivableStorage())
            .append("originalAmount", moneyToDocument(receivable.totalDue))
            .append("paidAmount", moneyToDocument(receivable.paidAmount))
            .append("balanceDue", moneyToDocument(receivable.balanceDue))
            .append("dueAt", receivable.dueAt?.let(MongoInstantMapper::toDate))
            .append("settledAt", if (receivable.balanceDue.amount.signum() == 0) MongoInstantMapper.toDate(receivable.updatedAt) else null)
            .append("paymentRefs", emptyList<String>())
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(receivable.createdAt))
            .append(MongoDocumentFields.CREATED_BY, null)
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(receivable.updatedAt))
            .append(MongoDocumentFields.UPDATED_BY, null)
            .append(MongoDocumentFields.VERSION, 1)
            .append(MongoDocumentFields.SCHEMA_VERSION, 1)

    fun receivableFromDocument(document: Document): Receivable =
        Receivable.restore(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.optionalString("branchId"),
            saleId = document.requiredString("saleId"),
            customerId = document.optionalString("customerId"),
            totalDue = moneyFromDocument(document.requiredDocument("originalAmount")),
            paidAmount = moneyFromDocument(document.requiredDocument("paidAmount")),
            dueAt = MongoInstantMapper.readOptional(document, "dueAt"),
            isVoided = document.requiredString("status") == "canceled",
            isWrittenOff = document.requiredString("status") == "written_off",
            createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
            updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
        )

    fun moneyToDocument(money: Money): Document =
        Document("amount", MongoDecimalMapper.moneyToDecimal128(money.amount))
            .append("currency", money.currency.value)

    fun moneyFromDocument(document: Document): Money = Money.of(
        amount = MongoDecimalMapper.readRequired(document, "amount"),
        currency = CurrencyCode(document.requiredString("currency")),
    )

    private fun inferReferenceType(movement: CashMovement): String = when (movement.type) {
        CashMovementType.SALE_PAYMENT -> "payment"
        CashMovementType.OPENING_BALANCE -> "cash_session"
        else -> "manual"
    }

    private fun PaymentMethod.toStorage(): String = name.lowercase()
    private fun PaymentLifecycleStatus.toStorage(): String = name.lowercase()

    private fun paymentMethodFromStorage(raw: String): PaymentMethod = when (raw.trim().lowercase()) {
        "cash" -> PaymentMethod.CASH
        "bank_transfer" -> PaymentMethod.BANK_TRANSFER
        "card_manual" -> PaymentMethod.CARD_MANUAL
        "card_gateway" -> PaymentMethod.CARD_GATEWAY
        "digital_wallet" -> PaymentMethod.DIGITAL_WALLET
        else -> PaymentMethod.OTHER
    }

    private fun paymentLifecycleStatusFromStorage(raw: String): PaymentLifecycleStatus =
        enumValueOf(raw.trim().uppercase())

    private fun cashMovementTypeFromStorage(raw: String): CashMovementType =
        enumValueOf(raw.trim().uppercase())

    private fun com.hermes.domain.payment.CollectionStatus.toReceivableStorage(): String = when (this) {
        com.hermes.domain.payment.CollectionStatus.PENDING -> "open"
        com.hermes.domain.payment.CollectionStatus.PARTIALLY_COLLECTED -> "partially_collected"
        com.hermes.domain.payment.CollectionStatus.COLLECTED -> "settled"
        com.hermes.domain.payment.CollectionStatus.OVERDUE -> "overdue"
        com.hermes.domain.payment.CollectionStatus.WRITTEN_OFF -> "written_off"
        com.hermes.domain.payment.CollectionStatus.VOIDED -> "canceled"
        com.hermes.domain.payment.CollectionStatus.NOT_REQUIRED -> "settled"
    }
}

internal fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

internal fun Document.optionalString(field: String): String? = getString(field)?.takeIf { it.isNotBlank() }

internal fun Document.optionalDocument(field: String): Document? = this[field] as? Document

internal fun Document.requiredDocument(field: String): Document =
    optionalDocument(field) ?: throw IllegalArgumentException("Required document field '$field' is missing.")
