package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.SaleItemStatus
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.tax.PriceTaxMode
import java.time.Instant

data class CreateQuickSaleCommand(
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val saleNumber: String? = null,
    val customerId: String? = null,
    val customerSnapshot: CustomerSnapshot = CustomerSnapshot.finalConsumer(),
    val cashSessionId: String? = null,
    val occurredAt: Instant,
    val autoConfirm: Boolean = true,
    val items: List<CreateSaleItemCommandLine>,
)

data class CreateSaleItemCommandLine(
    val catalogItemId: String,
    val quantity: Quantity,
    val unitPrice: Money? = null,
    val discount: Money? = null,
    val priceTaxMode: PriceTaxMode = PriceTaxMode.TAX_EXCLUSIVE,
)

data class AddSaleItemCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val occurredAt: Instant,
    val item: CreateSaleItemCommandLine,
)

data class GetSaleCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class SearchSalesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val statuses: Set<SaleOperationalStatus> = emptySet(),
    val customerId: String? = null,
    val activityId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class ChangeSaleStatusCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val targetStatus: SaleOperationalStatus,
    val reason: String,
)

data class ChangeSaleItemStatusCommand(
    val organizationId: String,
    val saleId: String,
    val saleItemId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val targetStatus: SaleItemStatus,
    val reason: String,
)

data class CancelSaleCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

data class CloseSaleCommand(
    val organizationId: String,
    val saleId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val reason: String,
)

data class CreateReservationCommand(
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val customerId: String? = null,
    val customerSnapshot: CustomerSnapshot = CustomerSnapshot.finalConsumer(),
    val resourceId: String? = null,
    val startAt: Instant,
    val endAt: Instant,
    val partySize: Int,
    val notes: String? = null,
    val linkedSaleItem: CreateSaleItemCommandLine? = null,
    val cashSessionId: String? = null,
)

data class SearchReservationsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val activityId: String? = null,
    val customerId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)
