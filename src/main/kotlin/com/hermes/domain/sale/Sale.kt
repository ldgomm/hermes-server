package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.payment.*
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

@ConsistentCopyVisibility
data class Sale private constructor(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleNumber: String?,
    val saleType: SaleType,
    val workflowMode: SaleWorkflowMode,
    val customerId: String?,
    val customerSnapshot: CustomerSnapshot,
    val items: List<SaleItem>,
    val payments: List<Payment>,
    val operationalStatus: SaleOperationalStatus,
    val dueAt: Instant?,
    val cashSessionId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Sale id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Sale organization id cannot be blank.")
        if (branchId.isBlank()) throw DomainRuleViolation("Sale branch id cannot be blank.")
        if (activityId.isBlank()) throw DomainRuleViolation("Sale activity id cannot be blank.")

        payments.forEach { payment ->
            if (payment.saleId != id) throw DomainRuleViolation("Payment does not belong to this sale.")
            if (payment.organizationId != organizationId) throw DomainRuleViolation("Payment does not belong to this organization.")
        }

        items.forEach { item ->
            if (item.catalogSnapshot.catalogItemId != item.catalogItemId) {
                throw DomainRuleViolation("Sale item snapshot mismatch.")
            }
        }
    }

    val activeItems: List<SaleItem>
        get() = items.filterNot { it.status == SaleItemStatus.CANCELED }

    val totals: SaleTotals
        get() = SaleTotals.fromItems(activeItems)

    val total: Money
        get() = totals.grandTotal

    val paidAmount: Money
        get() = payments
            .filter { it.isEffective }
            .fold(Money.zero(total.currency)) { current, payment -> current + payment.amount }

    val paymentStatus: SalePaymentStatus
        get() = SalePaymentStatusResolver.resolve(
            totalDue = total,
            paidAmount = paidAmount,
            isVoided = operationalStatus == SaleOperationalStatus.CANCELED,
        )

    fun receivableStatus(now: Instant): ReceivableStatus {
        return ReceivableStatusResolver.resolve(
            totalDue = total,
            paidAmount = paidAmount,
            dueAt = dueAt,
            now = now,
            isCanceled = operationalStatus == SaleOperationalStatus.CANCELED,
        )
    }

    fun addItem(item: SaleItem, updatedAt: Instant): Sale {
        assertCanMutateItems()
        if (items.any { it.id == item.id }) {
            throw DomainRuleViolation("Sale item id already exists in this sale.")
        }
        return copy(items = items + item, updatedAt = updatedAt)
    }

    fun removeItem(itemId: String, updatedAt: Instant): Sale {
        assertCanMutateItems()
        val item = items.firstOrNull { it.id == itemId }
            ?: throw DomainRuleViolation("Sale item does not exist.")
        return copy(
            items = items.map { if (it.id == itemId) item.cancel() else it },
            updatedAt = updatedAt,
        )
    }

    fun confirm(updatedAt: Instant): Sale {
        if (activeItems.isEmpty()) {
            throw DomainRuleViolation("Cannot confirm a sale without active items.")
        }

        SaleOperationalStateMachine.assertCanTransition(
            from = operationalStatus,
            to = SaleOperationalStatus.CONFIRMED,
        )

        return copy(
            operationalStatus = SaleOperationalStatus.CONFIRMED,
            updatedAt = updatedAt,
        )
    }

    fun transitionTo(target: SaleOperationalStatus, updatedAt: Instant): Sale {
        SaleOperationalStateMachine.assertCanTransition(
            from = operationalStatus,
            to = target,
        )

        if (target == SaleOperationalStatus.CLOSED &&
            paymentStatus !in setOf(SalePaymentStatus.PAID, SalePaymentStatus.OVERPAID)
        ) {
            throw DomainRuleViolation("Cannot close an unpaid sale.")
        }

        return copy(operationalStatus = target, updatedAt = updatedAt)
    }

    fun registerPayment(payment: Payment, updatedAt: Instant): Sale {
        if (operationalStatus in setOf(
                SaleOperationalStatus.DRAFT,
                SaleOperationalStatus.CANCELED,
                SaleOperationalStatus.CLOSED,
            )
        ) {
            throw DomainRuleViolation("Cannot register payment for sale with status $operationalStatus.")
        }

        if (payment.saleId != id) throw DomainRuleViolation("Cannot register payment for another sale.")
        if (payment.organizationId != organizationId) throw DomainRuleViolation("Cannot register payment for another organization.")
        if (payment.amount.currency != total.currency) throw DomainRuleViolation("Payment currency must match sale currency.")

        return copy(payments = payments + payment, updatedAt = updatedAt)
    }

    private fun assertCanMutateItems() {
        if (operationalStatus !in setOf(SaleOperationalStatus.DRAFT, SaleOperationalStatus.PENDING)) {
            throw DomainRuleViolation("Sale items can only be changed while sale is draft or pending.")
        }
    }

    companion object {
        fun createDraft(
            id: String,
            organizationId: String,
            branchId: String,
            activityId: String,
            saleType: SaleType = SaleType.SALE,
            workflowMode: SaleWorkflowMode = SaleWorkflowMode.QUICK_SALE,
            saleNumber: String? = null,
            customerId: String? = null,
            customerSnapshot: CustomerSnapshot = CustomerSnapshot.finalConsumer(),
            dueAt: Instant? = null,
            cashSessionId: String? = null,
            createdAt: Instant,
        ): Sale {
            return Sale(
                id = id,
                organizationId = organizationId,
                branchId = branchId,
                activityId = activityId,
                saleNumber = saleNumber?.trim()?.takeIf { it.isNotBlank() },
                saleType = saleType,
                workflowMode = workflowMode,
                customerId = customerId?.trim()?.takeIf { it.isNotBlank() },
                customerSnapshot = customerSnapshot,
                items = emptyList(),
                payments = emptyList(),
                operationalStatus = SaleOperationalStatus.DRAFT,
                dueAt = dueAt,
                cashSessionId = cashSessionId?.trim()?.takeIf { it.isNotBlank() },
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }
}
