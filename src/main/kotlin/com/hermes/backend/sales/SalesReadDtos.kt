package com.hermes.backend.sales

import com.hermes.application.sales.PendingSalesResult
import com.hermes.application.sales.SalesDaySummaryResult
import com.hermes.application.sales.SalesListItem
import com.hermes.application.sales.SalesSearchResult
import kotlinx.serialization.Serializable

@Serializable
data class SalesSearchResponse(
    val sales: List<SalesListItemResponse>,
)

@Serializable
data class PendingSalesResponse(
    val sales: List<SalesListItemResponse>,
)

@Serializable
data class SalesDaySummaryResponse(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: String,
    val to: String,
    val currency: String,
    val totalSalesCount: Int,
    val closedSalesCount: Int,
    val canceledSalesCount: Int,
    val openSalesCount: Int,
    val grossTotal: MoneyResponse,
    val discountTotal: MoneyResponse,
    val taxTotal: MoneyResponse,
    val grandTotal: MoneyResponse,
    val paidTotal: MoneyResponse,
    val receivableTotal: MoneyResponse,
    val byOperationalStatus: Map<String, Int>,
    val byPaymentStatus: Map<String, Int>,
)

@Serializable
data class SalesListItemResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleNumber: String?,
    val saleType: String,
    val customerId: String?,
    val customerDisplayName: String?,
    val operationalStatus: String,
    val paymentStatus: String,
    val itemCount: Int,
    val grandTotal: MoneyResponse,
    val paidAmount: MoneyResponse,
    val receivableAmount: MoneyResponse,
    val dueAt: String?,
    val cashSessionId: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class MoneyResponse(
    val amount: String,
    val currency: String,
)

fun SalesSearchResult.toResponse(): SalesSearchResponse =
    SalesSearchResponse(sales = sales.map { it.toResponse() })

fun PendingSalesResult.toResponse(): PendingSalesResponse =
    PendingSalesResponse(sales = sales.map { it.toResponse() })

fun SalesDaySummaryResult.toResponse(): SalesDaySummaryResponse =
    SalesDaySummaryResponse(
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        from = from.toString(),
        to = to.toString(),
        currency = currency,
        totalSalesCount = totalSalesCount,
        closedSalesCount = closedSalesCount,
        canceledSalesCount = canceledSalesCount,
        openSalesCount = openSalesCount,
        grossTotal = grossTotal.toResponse(),
        discountTotal = discountTotal.toResponse(),
        taxTotal = taxTotal.toResponse(),
        grandTotal = grandTotal.toResponse(),
        paidTotal = paidTotal.toResponse(),
        receivableTotal = receivableTotal.toResponse(),
        byOperationalStatus = byOperationalStatus.mapKeys { it.key.name.lowercase() },
        byPaymentStatus = byPaymentStatus.mapKeys { it.key.name.lowercase() },
    )

fun SalesListItem.toResponse(): SalesListItemResponse =
    SalesListItemResponse(
        id = id,
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        saleNumber = saleNumber,
        saleType = saleType.name.lowercase(),
        customerId = customerId,
        customerDisplayName = customerDisplayName,
        operationalStatus = operationalStatus.name.lowercase(),
        paymentStatus = paymentStatus.name.lowercase(),
        itemCount = itemCount,
        grandTotal = grandTotal.toResponse(),
        paidAmount = paidAmount.toResponse(),
        receivableAmount = receivableAmount.toResponse(),
        dueAt = dueAt?.toString(),
        cashSessionId = cashSessionId,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

private fun com.hermes.domain.money.Money.toResponse(): MoneyResponse =
    MoneyResponse(amount = amount.toPlainString(), currency = currency.value)
