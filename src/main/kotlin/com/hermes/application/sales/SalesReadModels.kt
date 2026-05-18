package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import java.time.Instant

data class SalesSearchCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val customerId: String? = null,
    val operationalStatuses: Set<SaleOperationalStatus> = emptySet(),
    val paymentStatuses: Set<SalePaymentStatus> = emptySet(),
    val saleTypes: Set<SaleType> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val query: String? = null,
    val limit: Int = 50,
)

data class PendingSalesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val now: Instant,
    val limit: Int = 100,
)

data class SalesDaySummaryCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val from: Instant,
    val to: Instant,
)

data class SalesSearchResult(
    val sales: List<SalesListItem>,
)

data class PendingSalesResult(
    val sales: List<SalesListItem>,
)

data class SalesDaySummaryResult(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: Instant,
    val to: Instant,
    val currency: String,
    val totalSalesCount: Int,
    val closedSalesCount: Int,
    val canceledSalesCount: Int,
    val openSalesCount: Int,
    val grossTotal: Money,
    val discountTotal: Money,
    val taxTotal: Money,
    val grandTotal: Money,
    val paidTotal: Money,
    val receivableTotal: Money,
    val byOperationalStatus: Map<SaleOperationalStatus, Int>,
    val byPaymentStatus: Map<SalePaymentStatus, Int>,
)

data class SalesListItem(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleNumber: String?,
    val saleType: SaleType,
    val customerId: String?,
    val customerDisplayName: String?,
    val operationalStatus: SaleOperationalStatus,
    val paymentStatus: SalePaymentStatus,
    val itemCount: Int,
    val grandTotal: Money,
    val paidAmount: Money,
    val receivableAmount: Money,
    val dueAt: Instant?,
    val cashSessionId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isPendingOperationally: Boolean
        get() = operationalStatus in setOf(
            SaleOperationalStatus.DRAFT,
            SaleOperationalStatus.PENDING,
            SaleOperationalStatus.CONFIRMED,
            SaleOperationalStatus.IN_PROGRESS,
            SaleOperationalStatus.READY,
            SaleOperationalStatus.DELIVERED,
        )

    val isReceivable: Boolean
        get() = receivableAmount.amount.signum() > 0
}
