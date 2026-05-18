package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItemStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CreateQuickSaleUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val saleItemPreparationService: SaleItemPreparationService,
    private val idGenerator: SalesIdGenerator,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val persistenceGuard: SalePersistenceGuard = SalePersistenceGuard(),
) {
    fun execute(command: CreateQuickSaleCommand): SaleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)
        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val activityId = command.activityId.required("Activity id")
        if (command.items.isEmpty()) throw DomainRuleViolation("Quick sale requires at least one item.")

        val saleId = idGenerator.newId("sale")
        var sale = Sale.createDraft(
            id = saleId,
            organizationId = organizationId,
            branchId = branchId,
            activityId = activityId,
            saleType = SaleType.SALE,
            workflowMode = SaleWorkflowMode.QUICK_SALE,
            saleNumber = command.saleNumber?.trim()?.takeIf { it.isNotBlank() } ?: SaleNumberFactory.fromId(saleId),
            customerId = command.customerId,
            customerSnapshot = command.customerSnapshot,
            cashSessionId = command.cashSessionId,
            createdAt = command.occurredAt,
        )

        val builtItems = saleItemPreparationService.prepare(
            organizationId = organizationId,
            actorUserId = command.actorUserId,
            actorEffectivePermissions = command.actorEffectivePermissions,
            occurredAt = command.occurredAt,
            lines = command.items,
        )
        builtItems.forEach { item -> sale = sale.addItem(item, command.occurredAt) }
        if (command.autoConfirm) {
            PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CONFIRM)
            sale = sale.confirm(command.occurredAt)
        }

        persistenceGuard.assertReadyToPersist(sale)
        saleRepository.create(sale)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_CREATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = sale.id,
                after = mapOf(
                    "saleNumber" to sale.saleNumber,
                    "status" to sale.operationalStatus.name,
                    "itemCount" to sale.items.size.toString(),
                    "subtotal" to sale.totals.subtotal.amount.toPlainString(),
                    "taxTotal" to sale.totals.taxTotal.amount.toPlainString(),
                    "grandTotal" to sale.total.amount.toPlainString(),
                ),
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(sale)
    }
}

class AddSaleItemUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val saleItemPreparationService: SaleItemPreparationService,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val persistenceGuard: SalePersistenceGuard = SalePersistenceGuard(),
) {
    fun execute(command: AddSaleItemCommand): SaleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)
        val sale = saleRepository.findById(command.organizationId, command.saleId)
            ?: throw DomainRuleViolation("Sale does not exist.")
        if (sale.operationalStatus !in setOf(SaleOperationalStatus.DRAFT, SaleOperationalStatus.PENDING)) {
            throw DomainRuleViolation("Sale items can only be added while sale is draft or pending.")
        }
        val item = saleItemPreparationService.prepare(
            organizationId = command.organizationId,
            actorUserId = command.actorUserId,
            actorEffectivePermissions = command.actorEffectivePermissions,
            occurredAt = command.occurredAt,
            lines = listOf(command.item),
        ).single()
        val updated = sale.addItem(item, Instant.now(clock))
        persistenceGuard.assertReadyToPersist(updated)
        saleRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_ITEM_ADDED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = sale.id,
                after = mapOf(
                    "saleItemId" to item.id,
                    "catalogItemId" to item.catalogItemId,
                    "grandTotal" to updated.total.amount.toPlainString(),
                ),
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(updated)
    }
}

class GetSaleUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetSaleCommand): SaleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        val sale = saleRepository.findById(command.organizationId, command.saleId)
            ?: throw DomainRuleViolation("Sale does not exist.")
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_VIEWED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = sale.id,
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(sale)
    }
}

class SearchSalesUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: SearchSalesCommand): SalesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        val query = SaleSearchQuery(
            organizationId = command.organizationId.required("Organization id"),
            statuses = command.statuses,
            customerId = command.customerId?.trim()?.takeIf { it.isNotBlank() },
            activityId = command.activityId?.trim()?.takeIf { it.isNotBlank() },
            from = command.from,
            to = command.to,
            limit = command.limit,
        )
        val sales = saleRepository.search(query)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_LISTED,
                actorUserId = command.actorUserId,
                organizationId = query.organizationId,
                targetId = null,
                after = mapOf("resultCount" to sales.size.toString()),
                createdAt = Instant.now(clock),
            )
        )
        return SalesResult(sales)
    }
}

class ChangeSaleStatusUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val persistenceGuard: SalePersistenceGuard = SalePersistenceGuard(),
) {
    fun execute(command: ChangeSaleStatusCommand): SaleResult {
        val permission = if (command.targetStatus == SaleOperationalStatus.CLOSED) {
            PermissionCatalog.SALES_CLOSE
        } else {
            PermissionCatalog.SALES_CONFIRM
        }
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, permission)
        val reason = command.reason.required("Sale status change reason")
        val sale = saleRepository.findById(command.organizationId, command.saleId)
            ?: throw DomainRuleViolation("Sale does not exist.")
        val before = sale.operationalStatus
        val updated = if (command.targetStatus == SaleOperationalStatus.CONFIRMED) {
            sale.confirm(Instant.now(clock))
        } else {
            sale.transitionTo(command.targetStatus, Instant.now(clock))
        }
        persistenceGuard.assertReadyToPersist(updated)
        saleRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = if (command.targetStatus == SaleOperationalStatus.CLOSED) SalesAuditAction.SALE_CLOSED else SalesAuditAction.SALE_STATUS_CHANGED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = sale.id,
                before = mapOf("status" to before.name),
                after = mapOf("status" to updated.operationalStatus.name),
                reason = reason,
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(updated)
    }
}

class ChangeSaleItemStatusUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val persistenceGuard: SalePersistenceGuard = SalePersistenceGuard(),
) {
    fun execute(command: ChangeSaleItemStatusCommand): SaleResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_ITEMS_CHANGE_STATUS)
        val reason = command.reason.required("Sale item status change reason")
        val sale = saleRepository.findById(command.organizationId, command.saleId)
            ?: throw DomainRuleViolation("Sale does not exist.")
        val item = sale.items.firstOrNull { it.id == command.saleItemId }
            ?: throw DomainRuleViolation("Sale item does not exist.")
        val updated = sale.changeItemStatus(
            itemId = command.saleItemId,
            targetStatus = command.targetStatus,
            updatedAt = Instant.now(clock),
        )
        val updatedItem = updated.items.first { it.id == command.saleItemId }
        persistenceGuard.assertReadyToPersist(updated)
        saleRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_STATUS_CHANGED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = sale.id,
                before = mapOf("saleItemId" to item.id, "itemStatus" to item.status.name),
                after = mapOf("saleItemId" to updatedItem.id, "itemStatus" to updatedItem.status.name),
                reason = reason,
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(updated)
    }
}

class CancelSaleUseCase(
    private val saleRepository: OperationalSaleRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val persistenceGuard: SalePersistenceGuard = SalePersistenceGuard(),
) {
    fun execute(command: CancelSaleCommand): SaleResult {
        val sale = saleRepository.findById(command.organizationId, command.saleId)
            ?: throw DomainRuleViolation("Sale does not exist.")
        val permission = if (sale.paidAmount.isPositive()) {
            PermissionCatalog.SALES_CANCEL_AFTER_PAYMENT
        } else {
            PermissionCatalog.SALES_CANCEL
        }
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, permission)
        val reason = command.reason.required("Sale cancellation reason")
        val updated = sale.transitionTo(SaleOperationalStatus.CANCELED, Instant.now(clock))
        persistenceGuard.assertReadyToPersist(updated)
        saleRepository.update(updated)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.SALE_CANCELED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = sale.id,
                before = mapOf("status" to sale.operationalStatus.name),
                after = mapOf("status" to updated.operationalStatus.name),
                reason = reason,
                createdAt = Instant.now(clock),
            )
        )
        return SaleResult(updated)
    }
}

class CloseSaleUseCase(
    private val changeSaleStatusUseCase: ChangeSaleStatusUseCase,
) {
    fun execute(command: CloseSaleCommand): SaleResult =
        changeSaleStatusUseCase.execute(
            ChangeSaleStatusCommand(
                organizationId = command.organizationId,
                saleId = command.saleId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                targetStatus = SaleOperationalStatus.CLOSED,
                reason = command.reason,
            )
        )
}

class CreateReservationUseCase(
    private val reservationRepository: OperationalReservationRepository,
    private val createQuickSaleUseCase: CreateQuickSaleUseCase,
    private val idGenerator: SalesIdGenerator,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val schedulingGuard: ReservationSchedulingGuard = ReservationSchedulingGuard(reservationRepository),
) {
    fun execute(command: CreateReservationCommand): ReservationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)

        val organizationId = command.organizationId.required("Organization id")
        val branchId = command.branchId.required("Branch id")
        val activityId = command.activityId.required("Activity id")
        val now = Instant.now(clock)

        schedulingGuard.assertCanSchedule(
            ReservationAvailabilityCommand(
                organizationId = organizationId,
                branchId = branchId,
                activityId = activityId,
                resourceId = command.resourceId,
                startAt = command.startAt,
                endAt = command.endAt,
                partySize = command.partySize,
            ),
            now = now,
        )

        val linkedSale = command.linkedSaleItem?.let { line ->
            createQuickSaleUseCase.execute(
                CreateQuickSaleCommand(
                    organizationId = organizationId,
                    branchId = branchId,
                    activityId = activityId,
                    actorUserId = command.actorUserId,
                    actorEffectivePermissions = command.actorEffectivePermissions,
                    customerId = command.customerId,
                    customerSnapshot = command.customerSnapshot,
                    cashSessionId = command.cashSessionId,
                    occurredAt = now,
                    autoConfirm = true,
                    items = listOf(line),
                )
            ).sale
        }

        val reservation = Reservation.schedule(
            id = idGenerator.newId("res"),
            organizationId = organizationId,
            branchId = branchId,
            activityId = activityId,
            saleId = linkedSale?.id,
            customerId = command.customerId,
            customerSnapshot = command.customerSnapshot,
            resourceId = command.resourceId,
            startAt = command.startAt,
            endAt = command.endAt,
            partySize = command.partySize,
            notes = command.notes,
            createdAt = now,
        )

        reservationRepository.create(reservation)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.RESERVATION_CREATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = reservation.id,
                after = mapOf(
                    "saleId" to linkedSale?.id,
                    "resourceId" to reservation.resourceId,
                    "startAt" to reservation.startAt.toString(),
                    "endAt" to reservation.endAt.toString(),
                    "partySize" to reservation.partySize.toString(),
                    "status" to reservation.status.name,
                ),
                createdAt = now,
            )
        )

        return ReservationResult(reservation = reservation, linkedSale = linkedSale)
    }
}

class GetReservationUseCase(
    private val reservationRepository: OperationalReservationRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetReservationCommand): ReservationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)

        val organizationId = command.organizationId.required("Organization id")
        val reservationId = command.reservationId.required("Reservation id")
        val reservation = reservationRepository.findById(
            organizationId = organizationId,
            reservationId = reservationId,
        ) ?: throw DomainRuleViolation("Reservation does not exist.")

        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.RESERVATION_VIEWED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = reservation.id,
                createdAt = Instant.now(clock),
            )
        )

        return ReservationResult(reservation = reservation)
    }
}

class SearchReservationsUseCase(
    private val reservationRepository: OperationalReservationRepository,
    private val auditLogger: SalesAuditLogger = NoopSalesAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: SearchReservationsCommand): ReservationsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        val query = ReservationSearchQuery(
            organizationId = command.organizationId.required("Organization id"),
            customerId = command.customerId?.trim()?.takeIf { it.isNotBlank() },
            activityId = command.activityId?.trim()?.takeIf { it.isNotBlank() },
            from = command.from,
            to = command.to,
            limit = command.limit,
        )
        val reservations = reservationRepository.search(query)
        auditLogger.log(
            SalesAuditEvent(
                action = SalesAuditAction.RESERVATION_LISTED,
                actorUserId = command.actorUserId,
                organizationId = query.organizationId,
                targetId = null,
                after = mapOf("resultCount" to reservations.size.toString()),
                createdAt = Instant.now(clock),
            )
        )
        return ReservationsResult(reservations)
    }
}
