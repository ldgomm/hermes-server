package com.hermes.application.admin.operations

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

// -----------------------------------------------------------------------------
// Commands
// -----------------------------------------------------------------------------

data class SearchAdminSalesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val customerId: String? = null,
    val operationalStatuses: Set<String> = emptySet(),
    val paymentStatuses: Set<String> = emptySet(),
    val saleTypes: Set<String> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val query: String? = null,
    val limit: Int = 100,
)

data class GetAdminSaleCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val saleId: String,
)

data class SearchAdminCashSessionsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val statuses: Set<String> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class GetCurrentAdminCashSessionCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
)

data class GetAdminCashSessionCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val cashSessionId: String,
)

data class SearchAdminPaymentsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val saleId: String? = null,
    val customerId: String? = null,
    val cashSessionId: String? = null,
    val methods: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
)

data class SearchAdminReceivablesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val customerId: String? = null,
    val statuses: Set<String> = emptySet(),
    val dueFrom: Instant? = null,
    val dueTo: Instant? = null,
    val limit: Int = 100,
)

data class GetAdminOperationalTodayReportCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val businessDate: LocalDate,
    val from: Instant,
    val to: Instant,
)

data class GetAdminSalesSummaryReportCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val from: Instant,
    val to: Instant,
)

data class GetAdminCashSummaryReportCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val from: Instant,
    val to: Instant,
)

data class GetAdminTaxSummaryReportCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val branchId: String? = null,
    val activityId: String? = null,
    val from: Instant,
    val to: Instant,
)

// -----------------------------------------------------------------------------
// Read models / results
// -----------------------------------------------------------------------------

data class AdminMoneyAmount(
    val amount: BigDecimal,
    val currency: String = "USD",
) {
    fun plus(other: AdminMoneyAmount): AdminMoneyAmount {
        require(currency == other.currency) { "Cannot add different currencies: $currency and ${other.currency}." }
        return copy(amount = amount + other.amount)
    }

    fun minus(other: AdminMoneyAmount): AdminMoneyAmount {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency and ${other.currency}." }
        return copy(amount = amount - other.amount)
    }

    fun positiveOrZero(): AdminMoneyAmount = if (amount.signum() < 0) zero(currency) else this

    companion object {
        fun zero(currency: String = "USD"): AdminMoneyAmount = AdminMoneyAmount(BigDecimal.ZERO, currency)
    }
}

data class AdminSalesResult(val sales: List<AdminSaleListItem>)

data class AdminSaleResult(val sale: AdminSaleDetail)

data class AdminCashSessionsResult(val cashSessions: List<AdminCashSessionReadModel>)

data class AdminCashSessionResult(val cashSession: AdminCashSessionReadModel?)

data class AdminPaymentsResult(val payments: List<AdminPaymentReadModel>)

data class AdminReceivablesResult(val receivables: List<AdminReceivableReadModel>)

data class AdminSaleListItem(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val saleNumber: String?,
    val saleType: String,
    val customerId: String?,
    val customerDisplayName: String?,
    val operationalStatus: String,
    val paymentStatus: String,
    val documentStatus: String?,
    val itemCount: Int,
    val subtotal: AdminMoneyAmount,
    val discountTotal: AdminMoneyAmount,
    val taxTotal: AdminMoneyAmount,
    val grandTotal: AdminMoneyAmount,
    val paidAmount: AdminMoneyAmount,
    val receivableAmount: AdminMoneyAmount,
    val dueAt: Instant?,
    val cashSessionId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AdminSaleDetail(
    val summary: AdminSaleListItem,
    val lines: List<AdminSaleLineReadModel>,
    val payments: List<AdminPaymentReadModel>,
    val documents: List<AdminCommercialDocumentReadModel>,
)

data class AdminSaleLineReadModel(
    val id: String,
    val catalogItemId: String?,
    val name: String,
    val quantity: BigDecimal,
    val unitCode: String?,
    val unitPrice: AdminMoneyAmount,
    val discount: AdminMoneyAmount,
    val netTotal: AdminMoneyAmount,
    val taxTotal: AdminMoneyAmount,
    val lineTotal: AdminMoneyAmount,
    val status: String,
    val taxProfileCode: String?,
    val sriTaxCode: String?,
    val sriRateCode: String?,
)

data class AdminPaymentReadModel(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val saleId: String?,
    val customerId: String?,
    val cashSessionId: String?,
    val amount: AdminMoneyAmount,
    val method: String,
    val status: String,
    val paidAt: Instant,
    val externalReference: String?,
    val notes: String?,
)

data class AdminCashSessionReadModel(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val openedBy: String?,
    val openedAt: Instant,
    val status: String,
    val openingBalance: AdminMoneyAmount,
    val expectedCashAmount: AdminMoneyAmount,
    val countedCashAmount: AdminMoneyAmount?,
    val differenceAmount: AdminMoneyAmount?,
    val movementCount: Int,
    val closingStartedAt: Instant?,
    val closedAt: Instant?,
    val canceledAt: Instant?,
    val movements: List<AdminCashMovementReadModel> = emptyList(),
)

data class AdminCashMovementReadModel(
    val id: String,
    val organizationId: String,
    val cashSessionId: String,
    val branchId: String?,
    val type: String,
    val direction: String,
    val amount: AdminMoneyAmount,
    val occurredAt: Instant,
    val referenceType: String?,
    val referenceId: String?,
    val notes: String?,
)

data class AdminReceivableReadModel(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val saleId: String,
    val customerId: String?,
    val status: String,
    val totalDue: AdminMoneyAmount,
    val paidAmount: AdminMoneyAmount,
    val balanceDue: AdminMoneyAmount,
    val dueAt: Instant?,
    val settledAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class AdminCommercialDocumentReadModel(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val emissionPointId: String?,
    val saleId: String?,
    val customerId: String?,
    val documentType: String,
    val documentNumber: String,
    val accessKey: String?,
    val authorizationNumber: String?,
    val status: String,
    val issuedAt: Instant,
    val authorizedAt: Instant?,
    val subtotal: AdminMoneyAmount,
    val discountTotal: AdminMoneyAmount,
    val taxTotal: AdminMoneyAmount,
    val grandTotal: AdminMoneyAmount,
)

data class AdminOperationalTodayReport(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val businessDate: LocalDate,
    val from: Instant,
    val to: Instant,
    val sales: AdminSalesSummaryReport,
    val cash: AdminCashSummaryReport,
    val tax: AdminTaxSummaryReport,
    val currentCashSession: AdminCashSessionReadModel?,
    val pendingReceivables: AdminMoneyAmount,
    val topItems: List<AdminTopItemReportLine>,
    val alerts: List<AdminOperationalAlert>,
)

data class AdminSalesSummaryReport(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: Instant,
    val to: Instant,
    val saleCount: Int,
    val closedSaleCount: Int,
    val canceledSaleCount: Int,
    val openSaleCount: Int,
    val itemCount: Int,
    val subtotal: AdminMoneyAmount,
    val discountTotal: AdminMoneyAmount,
    val taxTotal: AdminMoneyAmount,
    val grandTotal: AdminMoneyAmount,
    val paidTotal: AdminMoneyAmount,
    val receivableTotal: AdminMoneyAmount,
    val byOperationalStatus: List<AdminStatusCount>,
    val byPaymentStatus: List<AdminStatusCount>,
    val byDocumentStatus: List<AdminStatusCount>,
    val topItems: List<AdminTopItemReportLine>,
)

data class AdminCashSummaryReport(
    val organizationId: String,
    val branchId: String?,
    val from: Instant,
    val to: Instant,
    val openSessionCount: Int,
    val closedSessionCount: Int,
    val movementCount: Int,
    val cashInTotal: AdminMoneyAmount,
    val cashOutTotal: AdminMoneyAmount,
    val netCashMovement: AdminMoneyAmount,
    val expectedOpenCashTotal: AdminMoneyAmount,
    val countedClosedCashTotal: AdminMoneyAmount,
    val differenceClosedCashTotal: AdminMoneyAmount,
    val byMovementType: List<AdminStatusCount>,
)

data class AdminTaxSummaryReport(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: Instant,
    val to: Instant,
    val documentCount: Int,
    val authorizedDocumentCount: Int,
    val documentGrandTotal: AdminMoneyAmount,
    val taxTotal: AdminMoneyAmount,
    val byTaxRate: List<AdminTaxSummaryLine>,
)

data class AdminTaxSummaryLine(
    val taxCode: String,
    val rateCode: String,
    val rate: BigDecimal,
    val taxableBase: AdminMoneyAmount,
    val taxAmount: AdminMoneyAmount,
    val documentCount: Int,
)

data class AdminTopItemReportLine(
    val catalogItemId: String?,
    val name: String,
    val quantity: BigDecimal,
    val netTotal: AdminMoneyAmount,
    val lineTotal: AdminMoneyAmount,
)

data class AdminStatusCount(
    val status: String,
    val count: Int,
)

data class AdminOperationalAlert(
    val code: String,
    val severity: String,
    val message: String,
    val actionHint: String? = null,
)
