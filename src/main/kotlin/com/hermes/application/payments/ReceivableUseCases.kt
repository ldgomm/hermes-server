package com.hermes.application.payments

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CreateReceivableForSaleUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val receivableRepository: ReceivableRepository,
    private val idGenerator: PaymentsIdGenerator,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateReceivableForSaleCommand): ReceivableResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.PAYMENTS_MARK_AS_CREDIT)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.RECEIVABLES_CREATE)

        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Receivable reason")
        val sale = saleRepository.findById(organizationId, command.saleId.required("Sale id"))
            ?: throw DomainRuleViolation("Sale was not found.")

        if (sale.operationalStatus in setOf(SaleOperationalStatus.DRAFT, SaleOperationalStatus.CANCELED, SaleOperationalStatus.CLOSED)) {
            throw DomainRuleViolation("Cannot create receivable for sale with status ${sale.operationalStatus}.")
        }
        val remaining = if (sale.paidAmount >= sale.total) Money.zero(sale.total.currency) else sale.total - sale.paidAmount
        if (remaining.amount.signum() <= 0) throw DomainRuleViolation("Cannot create receivable for a fully paid sale.")
        if (receivableRepository.findBySaleId(organizationId, sale.id) != null) {
            throw DomainRuleViolation("Sale already has a receivable.")
        }

        val receivable = Receivable.createForSale(
            id = idGenerator.newId("recv"),
            organizationId = organizationId,
            branchId = sale.branchId,
            saleId = sale.id,
            customerId = sale.customerId,
            totalDue = remaining,
            dueAt = command.dueAt,
            createdAt = now,
        )
        receivableRepository.create(receivable)

        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.RECEIVABLE_CREATED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = receivable.id,
                saleId = sale.id,
                after = mapOf(
                    "totalDue" to receivable.totalDue.amount.toPlainString(),
                    "balanceDue" to receivable.balanceDue.amount.toPlainString(),
                ),
                reason = reason,
                createdAt = now,
            )
        )

        return ReceivableResult(receivable = receivable, sale = sale)
    }
}

class RegisterReceivableCollectionUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val receivableRepository: ReceivableRepository,
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val idGenerator: PaymentsIdGenerator,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val cashMovementRepository: CashMovementRepository = NoopCashMovementRepository,
    private val settlementRepository: PaymentSettlementRepository,
) {
    fun execute(command: RegisterReceivableCollectionCommand): RegisterReceivableCollectionResult {
        val now = command.collectedAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")

        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.RECEIVABLES_REGISTER_PAYMENT)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.PAYMENTS_COLLECT)

        val receivable = receivableRepository.findById(organizationId, command.receivableId.required("Receivable id"))
            ?: receivableRepository.findBySaleId(organizationId, command.saleId.required("Sale id"))
            ?: throw DomainRuleViolation("Receivable does not exist.")
        val sale = saleRepository.findById(organizationId, receivable.saleId)
            ?: throw DomainRuleViolation("Sale was not found.")

        val payment = Payment.record(
            id = idGenerator.newId("pay"),
            organizationId = organizationId,
            saleId = sale.id,
            amount = command.amount,
            method = command.method,
            paidAt = now,
            reference = command.reference,
            notes = command.notes,
        )
        val updatedSale = sale.registerPayment(payment, now)
        val updatedReceivable = receivable.registerCollection(command.amount, now)

        val cashUpdate = if (command.method.affectsCashDrawer) {
            PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW)
            val session = cashSessionRepository.findOpenByBranch(organizationId, sale.branchId)
                ?: throw DomainRuleViolation("Open cash session is required for cash receivable collections in branch ${sale.branchId}.")
            val movement = CashMovement.create(
                id = idGenerator.newId("cmov"),
                cashSessionId = session.id,
                organizationId = organizationId,
                branchId = sale.branchId,
                type = CashMovementType.SALE_PAYMENT,
                direction = CashMovementDirection.IN,
                amount = command.amount,
                occurredAt = now,
                referenceId = payment.id,
                notes = command.notes ?: "Receivable collection for sale ${sale.id}",
            )
            session.recordMovement(movement) to movement
        } else null

        settlementRepository.persistPaymentSettlement(
            PaymentSettlement(
                payment = payment,
                sale = updatedSale,
                cashSession = cashUpdate?.first,
                cashMovement = cashUpdate?.second,
                receivable = null,
            )
        )
        receivableRepository.update(updatedReceivable)

        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.RECEIVABLE_COLLECTION_REGISTERED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = updatedReceivable.id,
                saleId = sale.id,
                before = mapOf("balanceDue" to receivable.balanceDue.amount.toPlainString()),
                after = mapOf("balanceDue" to updatedReceivable.balanceDue.amount.toPlainString()),
                createdAt = now,
            )
        )

        return RegisterReceivableCollectionResult(
            receivable = updatedReceivable,
            payment = payment,
            sale = updatedSale,
            cashSession = cashUpdate?.first,
            cashMovement = cashUpdate?.second,
        )
    }
}
