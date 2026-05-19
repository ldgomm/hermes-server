package com.hermes.application.payments

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Receivable
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.sale.Sale
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class RegisterPaymentUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val paymentRepository: PaymentRepository,
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val receivableRepository: ReceivableRepository,
    private val idGenerator: PaymentsIdGenerator,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val cashMovementRepository: CashMovementRepository = NoopCashMovementRepository,
    private val settlementRepository: PaymentSettlementRepository = DirectPaymentSettlementRepository(
        paymentRepository = paymentRepository,
        saleRepository = saleRepository,
        cashSessionRepository = cashSessionRepository,
        cashMovementRepository = cashMovementRepository,
        receivableRepository = receivableRepository,
    ),
) {
    fun execute(command: RegisterPaymentCommand): RegisterPaymentResult {
        val now = Instant.now(clock)
        val paidAt = command.paidAt ?: now
        val organizationId = command.organizationId.required("Organization id")
        val saleId = command.saleId.required("Sale id")
        val actorUserId = command.actorUserId.required("Actor user id")

        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.PAYMENTS_COLLECT)

        val sale = saleRepository.findById(organizationId, saleId) ?: throw DomainRuleViolation("Sale was not found.")

        if (command.amount.amount.signum() <= 0) throw DomainRuleViolation("Payment amount must be greater than zero.")
        if (command.amount.currency != sale.total.currency) throw DomainRuleViolation("Payment currency must match sale currency.")

        val remainingBeforePayment = remainingBalance(sale)
        if (remainingBeforePayment.amount.signum() == 0) throw DomainRuleViolation("Sale is already fully paid.")
        if (command.amount > remainingBeforePayment) throw DomainRuleViolation("Payment amount cannot exceed sale balance.")

        val willRemainPartiallyPaid = command.amount < remainingBeforePayment
        if (willRemainPartiallyPaid) {
            PermissionRules.assertCanPerform(
                command.actorEffectivePermissions, PermissionCatalog.PAYMENTS_PARTIAL_COLLECT
            )
            if (!command.markRemainingAsReceivable) {
                throw DomainRuleViolation("Partial payment requires marking the remaining balance as receivable.")
            }
            PermissionRules.assertCanPerform(
                command.actorEffectivePermissions, PermissionCatalog.PAYMENTS_MARK_AS_CREDIT
            )
            PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.RECEIVABLES_CREATE)
        }
        if (!willRemainPartiallyPaid && command.markRemainingAsReceivable) {
            throw DomainRuleViolation("A fully paid sale cannot be marked as receivable.")
        }
        if (command.method.affectsCashDrawer) {
            PermissionRules.assertCanPerform(
                command.actorEffectivePermissions, PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW
            )
        }

        val payment = com.hermes.domain.payment.Payment.record(
            id = idGenerator.newId("pay"),
            organizationId = organizationId,
            saleId = sale.id,
            amount = command.amount,
            method = command.method,
            paidAt = paidAt,
            reference = command.reference,
            notes = command.notes,
        )

        val updatedSale = sale.registerPayment(payment, paidAt)

        val cashUpdate = if (command.method.affectsCashDrawer) {
            val openCashSession = cashSessionRepository.findOpenByBranch(organizationId, sale.branchId)
                ?: throw DomainRuleViolation("Open cash session is required for cash payments in branch ${sale.branchId}.")
            val movement = CashMovement.create(
                id = idGenerator.newId("cmov"),
                cashSessionId = openCashSession.id,
                organizationId = organizationId,
                branchId = sale.branchId,
                type = CashMovementType.SALE_PAYMENT,
                direction = CashMovementDirection.IN,
                amount = command.amount,
                occurredAt = paidAt,
                referenceId = payment.id,
                notes = command.notes ?: "Payment for sale ${sale.id}",
            )
            CashUpdate(session = openCashSession.recordMovement(movement), movement = movement)
        } else null

        val receivable = if (willRemainPartiallyPaid) {
            if (receivableRepository.findBySaleId(organizationId, sale.id) != null) {
                throw DomainRuleViolation("Sale already has a receivable.")
            }
            Receivable.createForSale(
                id = idGenerator.newId("recv"),
                organizationId = organizationId,
                branchId = sale.branchId,
                saleId = sale.id,
                customerId = sale.customerId,
                totalDue = remainingBalance(updatedSale),
                dueAt = command.receivableDueAt,
                createdAt = paidAt,
            )
        } else null

        settlementRepository.persistPaymentSettlement(
            PaymentSettlement(
                payment = payment,
                sale = updatedSale,
                cashSession = cashUpdate?.session,
                cashMovement = cashUpdate?.movement,
                receivable = receivable,
            )
        )

        auditPayment(actorUserId, organizationId, sale, updatedSale, payment, remainingBeforePayment, now)
        cashUpdate?.let { auditCashMovement(actorUserId, organizationId, sale.id, it, now) }
        receivable?.let { auditReceivable(actorUserId, organizationId, sale.id, it, now) }

        return RegisterPaymentResult(
            payment = payment,
            sale = updatedSale,
            cashSession = cashUpdate?.session,
            cashMovement = cashUpdate?.movement,
            receivable = receivable,
        )
    }

    private fun auditPayment(
        actorUserId: String,
        organizationId: String,
        sale: Sale,
        updatedSale: Sale,
        payment: com.hermes.domain.payment.Payment,
        remainingBeforePayment: Money,
        now: Instant,
    ) {
        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.PAYMENT_REGISTERED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = payment.id,
                saleId = sale.id,
                before = mapOf(
                    "salePaymentStatus" to sale.paymentStatus.name,
                    "salePaidAmount" to sale.paidAmount.amount.toPlainString(),
                    "saleBalance" to remainingBeforePayment.amount.toPlainString(),
                ),
                after = mapOf(
                    "method" to payment.method.name,
                    "amount" to payment.amount.amount.toPlainString(),
                    "salePaymentStatus" to updatedSale.paymentStatus.name,
                    "salePaidAmount" to updatedSale.paidAmount.amount.toPlainString(),
                    "saleBalance" to remainingBalance(updatedSale).amount.toPlainString(),
                ),
                createdAt = now,
            )
        )
    }

    private fun auditCashMovement(
        actorUserId: String, organizationId: String, saleId: String, cashUpdate: CashUpdate, now: Instant
    ) {
        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.CASH_MOVEMENT_CREATED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = cashUpdate.movement.id,
                saleId = saleId,
                after = mapOf(
                    "cashSessionId" to cashUpdate.session.id,
                    "paymentId" to cashUpdate.movement.referenceId,
                    "amount" to cashUpdate.movement.amount.amount.toPlainString(),
                ),
                createdAt = now,
            )
        )
    }

    private fun auditReceivable(
        actorUserId: String, organizationId: String, saleId: String, receivable: Receivable, now: Instant
    ) {
        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.RECEIVABLE_CREATED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = receivable.id,
                saleId = saleId,
                after = mapOf(
                    "totalDue" to receivable.totalDue.amount.toPlainString(),
                    "balanceDue" to receivable.balanceDue.amount.toPlainString(),
                ),
                createdAt = now,
            )
        )
    }

    private fun remainingBalance(sale: Sale): Money =
        if (sale.paidAmount >= sale.total) Money.zero(sale.total.currency) else sale.total - sale.paidAmount

    private data class CashUpdate(val session: com.hermes.domain.cash.CashSession, val movement: CashMovement)
}

internal fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label is required.")
