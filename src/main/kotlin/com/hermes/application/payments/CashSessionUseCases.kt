package com.hermes.application.payments

import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.cash.CashSession
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class OpenCashSessionUseCase(
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val cashMovementRepository: CashMovementRepository,
    private val idGenerator: PaymentsIdGenerator,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: OpenCashSessionCommand): CashSessionResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CASH_SESSION_OPEN)
        val now = command.openedAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val actorUserId = command.actorUserId.required("Actor user id")

        if (cashSessionRepository.findOpenByBranch(organizationId, branchId) != null) {
            throw DomainRuleViolation("Branch already has an open cash session.")
        }

        val session = CashSession.open(
            id = idGenerator.newId("cash"),
            organizationId = organizationId,
            branchId = branchId,
            openedBy = actorUserId,
            openingBalance = command.openingBalance,
            openedAt = now,
        )

        val openingMovement = if (command.openingBalance.amount.signum() > 0) {
            CashMovement.create(
                id = idGenerator.newId("cmov"),
                cashSessionId = session.id,
                organizationId = organizationId,
                branchId = branchId,
                type = CashMovementType.OPENING_BALANCE,
                direction = CashMovementDirection.NEUTRAL,
                amount = command.openingBalance,
                occurredAt = now,
                referenceId = session.id,
                notes = command.notes ?: "Opening balance",
            )
        } else null

        cashSessionRepository.create(openingMovement?.let(session::recordMovement) ?: session)
        openingMovement?.let(cashMovementRepository::create)

        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.CASH_SESSION_OPENED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = session.id,
                saleId = null,
                after = mapOf(
                    "branchId" to branchId,
                    "openingBalance" to command.openingBalance.amount.toPlainString(),
                ),
                createdAt = now,
            )
        )

        return CashSessionResult(
            cashSession = openingMovement?.let(session::recordMovement) ?: session,
            openingMovement = openingMovement,
        )
    }
}

class RegisterCashMovementUseCase(
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val cashMovementRepository: CashMovementRepository,
    private val idGenerator: PaymentsIdGenerator,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RegisterCashMovementCommand): CashMovementResult {
        val now = command.occurredAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")

        when (command.direction) {
            CashMovementDirection.IN -> PermissionRules.assertCanPerform(
                command.actorEffectivePermissions,
                PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW
            )

            CashMovementDirection.OUT -> PermissionRules.assertCanPerform(
                command.actorEffectivePermissions,
                PermissionCatalog.CASH_MOVEMENTS_REGISTER_OUTFLOW
            )

            CashMovementDirection.NEUTRAL -> PermissionRules.assertCanPerform(
                command.actorEffectivePermissions,
                PermissionCatalog.CASH_MOVEMENTS_ADJUST
            )
        }
        if (command.type == CashMovementType.ADJUSTMENT) {
            PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CASH_MOVEMENTS_ADJUST)
        }

        val session = cashSessionRepository.findById(organizationId, command.cashSessionId.required("Cash session id"))
            ?: throw DomainRuleViolation("Cash session does not exist.")

        val movement = CashMovement.create(
            id = idGenerator.newId("cmov"),
            cashSessionId = session.id,
            organizationId = organizationId,
            branchId = session.branchId,
            type = command.type,
            direction = command.direction,
            amount = command.amount,
            occurredAt = now,
            referenceId = command.referenceId,
            notes = command.notes,
        )

        val updatedSession = session.recordMovement(movement)
        cashMovementRepository.create(movement)
        cashSessionRepository.update(updatedSession)

        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.CASH_MOVEMENT_CREATED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = movement.id,
                saleId = null,
                after = mapOf(
                    "cashSessionId" to session.id,
                    "type" to movement.type.name,
                    "direction" to movement.direction.name,
                    "amount" to movement.amount.amount.toPlainString(),
                ),
                createdAt = now,
            )
        )

        return CashMovementResult(cashSession = updatedSession, cashMovement = movement)
    }
}

class CloseCashSessionUseCase(
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val auditLogger: PaymentAuditLogger = NoopPaymentAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CloseCashSessionCommand): CloseCashSessionResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CASH_SESSION_CLOSE)
        val now = command.closedAt ?: Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val reason = command.reason.required("Cash closing reason")

        val session = cashSessionRepository.findById(organizationId, command.cashSessionId.required("Cash session id"))
            ?: throw DomainRuleViolation("Cash session does not exist.")

        val closing = session.startClosing(now)
        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.CASH_SESSION_CLOSING_STARTED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = session.id,
                saleId = null,
                before = mapOf("status" to session.status.name),
                after = mapOf("status" to closing.status.name),
                reason = reason,
                createdAt = now,
            )
        )

        val closed = closing.close(
            closedAt = now,
            countedCashAmount = command.countedCashAmount,
            closingNotes = command.notes,
        )
        cashSessionRepository.update(closed)

        auditLogger.log(
            PaymentAuditEvent(
                action = PaymentAuditAction.CASH_SESSION_CLOSED,
                actorUserId = actorUserId,
                organizationId = organizationId,
                targetId = session.id,
                saleId = null,
                before = mapOf(
                    "status" to session.status.name,
                    "expectedCashAmount" to session.expectedCashAmount.amount.toPlainString(),
                ),
                after = mapOf(
                    "status" to closed.status.name,
                    "expectedCashAmount" to closed.expectedCashAmount.amount.toPlainString(),
                    "countedCashAmount" to closed.countedCashAmount?.amount?.toPlainString(),
                    "differenceAmount" to closed.differenceAmount?.amount?.toPlainString(),
                ),
                reason = reason,
                createdAt = now,
            )
        )

        return CloseCashSessionResult(closed)
    }
}
