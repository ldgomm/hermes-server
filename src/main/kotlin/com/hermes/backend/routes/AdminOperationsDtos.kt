package com.hermes.backend.routes

import com.hermes.application.admin.operations.AdminCashMovementReadModel
import com.hermes.application.admin.operations.AdminCashSessionReadModel
import com.hermes.application.admin.operations.AdminCashSessionResult
import com.hermes.application.admin.operations.AdminCashSessionsResult
import com.hermes.application.admin.operations.AdminCashSummaryReport
import com.hermes.application.admin.operations.AdminCommercialDocumentReadModel
import com.hermes.application.admin.operations.AdminMoneyAmount
import com.hermes.application.admin.operations.AdminOperationalAlert
import com.hermes.application.admin.operations.AdminOperationalTodayReport
import com.hermes.application.admin.operations.AdminPaymentReadModel
import com.hermes.application.admin.operations.AdminPaymentsResult
import com.hermes.application.admin.operations.AdminReceivableReadModel
import com.hermes.application.admin.operations.AdminReceivablesResult
import com.hermes.application.admin.operations.AdminSaleDetail
import com.hermes.application.admin.operations.AdminSaleLineReadModel
import com.hermes.application.admin.operations.AdminSaleListItem
import com.hermes.application.admin.operations.AdminSaleResult
import com.hermes.application.admin.operations.AdminSalesResult
import com.hermes.application.admin.operations.AdminSalesSummaryReport
import com.hermes.application.admin.operations.AdminStatusCount
import com.hermes.application.admin.operations.AdminTaxSummaryLine
import com.hermes.application.admin.operations.AdminTaxSummaryReport
import com.hermes.application.admin.operations.AdminTopItemReportLine
import kotlinx.serialization.Serializable

@Serializable
data class AdminOperationsMoneyResponse(
    val amount: String,
    val currency: String,
)

@Serializable
data class AdminSalesResponse(val sales: List<AdminSaleListItemResponse>)

@Serializable
data class AdminSaleResponse(val sale: AdminSaleDetailResponse)

@Serializable
data class AdminCashSessionsResponse(val cashSessions: List<AdminCashSessionResponse>)

@Serializable
data class AdminCashSessionEnvelopeResponse(val cashSession: AdminCashSessionResponse?)

@Serializable
data class AdminPaymentsResponse(val payments: List<AdminPaymentResponse>)

@Serializable
data class AdminReceivablesResponse(val receivables: List<AdminReceivableResponse>)

@Serializable
data class AdminSaleListItemResponse(
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
    val subtotal: AdminOperationsMoneyResponse,
    val discountTotal: AdminOperationsMoneyResponse,
    val taxTotal: AdminOperationsMoneyResponse,
    val grandTotal: AdminOperationsMoneyResponse,
    val paidAmount: AdminOperationsMoneyResponse,
    val receivableAmount: AdminOperationsMoneyResponse,
    val dueAt: String?,
    val cashSessionId: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class AdminSaleDetailResponse(
    val summary: AdminSaleListItemResponse,
    val lines: List<AdminSaleLineResponse>,
    val payments: List<AdminPaymentResponse>,
    val documents: List<AdminCommercialDocumentResponse>,
)

@Serializable
data class AdminSaleLineResponse(
    val id: String,
    val catalogItemId: String?,
    val name: String,
    val quantity: String,
    val unitCode: String?,
    val unitPrice: AdminOperationsMoneyResponse,
    val discount: AdminOperationsMoneyResponse,
    val netTotal: AdminOperationsMoneyResponse,
    val taxTotal: AdminOperationsMoneyResponse,
    val lineTotal: AdminOperationsMoneyResponse,
    val status: String,
    val taxProfileCode: String?,
    val sriTaxCode: String?,
    val sriRateCode: String?,
)

@Serializable
data class AdminPaymentResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val saleId: String?,
    val customerId: String?,
    val cashSessionId: String?,
    val amount: AdminOperationsMoneyResponse,
    val method: String,
    val status: String,
    val paidAt: String,
    val externalReference: String?,
    val notes: String?,
)

@Serializable
data class AdminCashSessionResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val openedBy: String?,
    val openedAt: String,
    val status: String,
    val openingBalance: AdminOperationsMoneyResponse,
    val expectedCashAmount: AdminOperationsMoneyResponse,
    val countedCashAmount: AdminOperationsMoneyResponse?,
    val differenceAmount: AdminOperationsMoneyResponse?,
    val movementCount: Int,
    val closingStartedAt: String?,
    val closedAt: String?,
    val canceledAt: String?,
    val movements: List<AdminCashMovementResponse>,
)

@Serializable
data class AdminCashMovementResponse(
    val id: String,
    val organizationId: String,
    val cashSessionId: String,
    val branchId: String?,
    val type: String,
    val direction: String,
    val amount: AdminOperationsMoneyResponse,
    val occurredAt: String,
    val referenceType: String?,
    val referenceId: String?,
    val notes: String?,
)

@Serializable
data class AdminReceivableResponse(
    val id: String,
    val organizationId: String,
    val branchId: String?,
    val saleId: String,
    val customerId: String?,
    val status: String,
    val totalDue: AdminOperationsMoneyResponse,
    val paidAmount: AdminOperationsMoneyResponse,
    val balanceDue: AdminOperationsMoneyResponse,
    val dueAt: String?,
    val settledAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class AdminCommercialDocumentResponse(
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
    val issuedAt: String,
    val authorizedAt: String?,
    val subtotal: AdminOperationsMoneyResponse,
    val discountTotal: AdminOperationsMoneyResponse,
    val taxTotal: AdminOperationsMoneyResponse,
    val grandTotal: AdminOperationsMoneyResponse,
)

@Serializable
data class AdminOperationalTodayReportResponse(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val businessDate: String,
    val from: String,
    val to: String,
    val sales: AdminSalesSummaryReportResponse,
    val cash: AdminCashSummaryReportResponse,
    val tax: AdminTaxSummaryReportResponse,
    val currentCashSession: AdminCashSessionResponse?,
    val pendingReceivables: AdminOperationsMoneyResponse,
    val topItems: List<AdminTopItemReportLineResponse>,
    val alerts: List<AdminOperationalAlertResponse>,
)

@Serializable
data class AdminSalesSummaryReportResponse(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: String,
    val to: String,
    val saleCount: Int,
    val closedSaleCount: Int,
    val canceledSaleCount: Int,
    val openSaleCount: Int,
    val itemCount: Int,
    val subtotal: AdminOperationsMoneyResponse,
    val discountTotal: AdminOperationsMoneyResponse,
    val taxTotal: AdminOperationsMoneyResponse,
    val grandTotal: AdminOperationsMoneyResponse,
    val paidTotal: AdminOperationsMoneyResponse,
    val receivableTotal: AdminOperationsMoneyResponse,
    val byOperationalStatus: List<AdminStatusCountResponse>,
    val byPaymentStatus: List<AdminStatusCountResponse>,
    val byDocumentStatus: List<AdminStatusCountResponse>,
    val topItems: List<AdminTopItemReportLineResponse>,
)

@Serializable
data class AdminCashSummaryReportResponse(
    val organizationId: String,
    val branchId: String?,
    val from: String,
    val to: String,
    val openSessionCount: Int,
    val closedSessionCount: Int,
    val movementCount: Int,
    val cashInTotal: AdminOperationsMoneyResponse,
    val cashOutTotal: AdminOperationsMoneyResponse,
    val netCashMovement: AdminOperationsMoneyResponse,
    val expectedOpenCashTotal: AdminOperationsMoneyResponse,
    val countedClosedCashTotal: AdminOperationsMoneyResponse,
    val differenceClosedCashTotal: AdminOperationsMoneyResponse,
    val byMovementType: List<AdminStatusCountResponse>,
)

@Serializable
data class AdminTaxSummaryReportResponse(
    val organizationId: String,
    val branchId: String?,
    val activityId: String?,
    val from: String,
    val to: String,
    val documentCount: Int,
    val authorizedDocumentCount: Int,
    val documentGrandTotal: AdminOperationsMoneyResponse,
    val taxTotal: AdminOperationsMoneyResponse,
    val byTaxRate: List<AdminTaxSummaryLineResponse>,
)

@Serializable
data class AdminTaxSummaryLineResponse(
    val taxCode: String,
    val rateCode: String,
    val rate: String,
    val taxableBase: AdminOperationsMoneyResponse,
    val taxAmount: AdminOperationsMoneyResponse,
    val documentCount: Int,
)

@Serializable
data class AdminTopItemReportLineResponse(
    val catalogItemId: String?,
    val name: String,
    val quantity: String,
    val netTotal: AdminOperationsMoneyResponse,
    val lineTotal: AdminOperationsMoneyResponse,
)

@Serializable
data class AdminStatusCountResponse(
    val status: String,
    val count: Int,
)

@Serializable
data class AdminOperationalAlertResponse(
    val code: String,
    val severity: String,
    val message: String,
    val actionHint: String?,
)

fun AdminSalesResult.toResponse(): AdminSalesResponse = AdminSalesResponse(sales.map { it.toResponse() })
fun AdminSaleResult.toResponse(): AdminSaleResponse = AdminSaleResponse(sale.toResponse())
fun AdminCashSessionsResult.toResponse(): AdminCashSessionsResponse = AdminCashSessionsResponse(cashSessions.map { it.toResponse() })
fun AdminCashSessionResult.toResponse(): AdminCashSessionEnvelopeResponse = AdminCashSessionEnvelopeResponse(cashSession?.toResponse())
fun AdminPaymentsResult.toResponse(): AdminPaymentsResponse = AdminPaymentsResponse(payments.map { it.toResponse() })
fun AdminReceivablesResult.toResponse(): AdminReceivablesResponse = AdminReceivablesResponse(receivables.map { it.toResponse() })

fun AdminSaleListItem.toResponse(): AdminSaleListItemResponse = AdminSaleListItemResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    activityId = activityId,
    saleNumber = saleNumber,
    saleType = saleType,
    customerId = customerId,
    customerDisplayName = customerDisplayName,
    operationalStatus = operationalStatus,
    paymentStatus = paymentStatus,
    documentStatus = documentStatus,
    itemCount = itemCount,
    subtotal = subtotal.toResponse(),
    discountTotal = discountTotal.toResponse(),
    taxTotal = taxTotal.toResponse(),
    grandTotal = grandTotal.toResponse(),
    paidAmount = paidAmount.toResponse(),
    receivableAmount = receivableAmount.toResponse(),
    dueAt = dueAt?.toString(),
    cashSessionId = cashSessionId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun AdminSaleDetail.toResponse(): AdminSaleDetailResponse = AdminSaleDetailResponse(
    summary = summary.toResponse(),
    lines = lines.map { it.toResponse() },
    payments = payments.map { it.toResponse() },
    documents = documents.map { it.toResponse() },
)

fun AdminSaleLineReadModel.toResponse(): AdminSaleLineResponse = AdminSaleLineResponse(
    id = id,
    catalogItemId = catalogItemId,
    name = name,
    quantity = quantity.toPlainString(),
    unitCode = unitCode,
    unitPrice = unitPrice.toResponse(),
    discount = discount.toResponse(),
    netTotal = netTotal.toResponse(),
    taxTotal = taxTotal.toResponse(),
    lineTotal = lineTotal.toResponse(),
    status = status,
    taxProfileCode = taxProfileCode,
    sriTaxCode = sriTaxCode,
    sriRateCode = sriRateCode,
)

fun AdminPaymentReadModel.toResponse(): AdminPaymentResponse = AdminPaymentResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    saleId = saleId,
    customerId = customerId,
    cashSessionId = cashSessionId,
    amount = amount.toResponse(),
    method = method,
    status = status,
    paidAt = paidAt.toString(),
    externalReference = externalReference,
    notes = notes,
)

fun AdminCashSessionReadModel.toResponse(): AdminCashSessionResponse = AdminCashSessionResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    openedBy = openedBy,
    openedAt = openedAt.toString(),
    status = status,
    openingBalance = openingBalance.toResponse(),
    expectedCashAmount = expectedCashAmount.toResponse(),
    countedCashAmount = countedCashAmount?.toResponse(),
    differenceAmount = differenceAmount?.toResponse(),
    movementCount = movementCount,
    closingStartedAt = closingStartedAt?.toString(),
    closedAt = closedAt?.toString(),
    canceledAt = canceledAt?.toString(),
    movements = movements.map { it.toResponse() },
)

fun AdminCashMovementReadModel.toResponse(): AdminCashMovementResponse = AdminCashMovementResponse(
    id = id,
    organizationId = organizationId,
    cashSessionId = cashSessionId,
    branchId = branchId,
    type = type,
    direction = direction,
    amount = amount.toResponse(),
    occurredAt = occurredAt.toString(),
    referenceType = referenceType,
    referenceId = referenceId,
    notes = notes,
)

fun AdminReceivableReadModel.toResponse(): AdminReceivableResponse = AdminReceivableResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    saleId = saleId,
    customerId = customerId,
    status = status,
    totalDue = totalDue.toResponse(),
    paidAmount = paidAmount.toResponse(),
    balanceDue = balanceDue.toResponse(),
    dueAt = dueAt?.toString(),
    settledAt = settledAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun AdminCommercialDocumentReadModel.toResponse(): AdminCommercialDocumentResponse = AdminCommercialDocumentResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    emissionPointId = emissionPointId,
    saleId = saleId,
    customerId = customerId,
    documentType = documentType,
    documentNumber = documentNumber,
    accessKey = accessKey,
    authorizationNumber = authorizationNumber,
    status = status,
    issuedAt = issuedAt.toString(),
    authorizedAt = authorizedAt?.toString(),
    subtotal = subtotal.toResponse(),
    discountTotal = discountTotal.toResponse(),
    taxTotal = taxTotal.toResponse(),
    grandTotal = grandTotal.toResponse(),
)

fun AdminOperationalTodayReport.toResponse(): AdminOperationalTodayReportResponse = AdminOperationalTodayReportResponse(
    organizationId = organizationId,
    branchId = branchId,
    activityId = activityId,
    businessDate = businessDate.toString(),
    from = from.toString(),
    to = to.toString(),
    sales = sales.toResponse(),
    cash = cash.toResponse(),
    tax = tax.toResponse(),
    currentCashSession = currentCashSession?.toResponse(),
    pendingReceivables = pendingReceivables.toResponse(),
    topItems = topItems.map { it.toResponse() },
    alerts = alerts.map { it.toResponse() },
)

fun AdminSalesSummaryReport.toResponse(): AdminSalesSummaryReportResponse = AdminSalesSummaryReportResponse(
    organizationId = organizationId,
    branchId = branchId,
    activityId = activityId,
    from = from.toString(),
    to = to.toString(),
    saleCount = saleCount,
    closedSaleCount = closedSaleCount,
    canceledSaleCount = canceledSaleCount,
    openSaleCount = openSaleCount,
    itemCount = itemCount,
    subtotal = subtotal.toResponse(),
    discountTotal = discountTotal.toResponse(),
    taxTotal = taxTotal.toResponse(),
    grandTotal = grandTotal.toResponse(),
    paidTotal = paidTotal.toResponse(),
    receivableTotal = receivableTotal.toResponse(),
    byOperationalStatus = byOperationalStatus.map { it.toResponse() },
    byPaymentStatus = byPaymentStatus.map { it.toResponse() },
    byDocumentStatus = byDocumentStatus.map { it.toResponse() },
    topItems = topItems.map { it.toResponse() },
)

fun AdminCashSummaryReport.toResponse(): AdminCashSummaryReportResponse = AdminCashSummaryReportResponse(
    organizationId = organizationId,
    branchId = branchId,
    from = from.toString(),
    to = to.toString(),
    openSessionCount = openSessionCount,
    closedSessionCount = closedSessionCount,
    movementCount = movementCount,
    cashInTotal = cashInTotal.toResponse(),
    cashOutTotal = cashOutTotal.toResponse(),
    netCashMovement = netCashMovement.toResponse(),
    expectedOpenCashTotal = expectedOpenCashTotal.toResponse(),
    countedClosedCashTotal = countedClosedCashTotal.toResponse(),
    differenceClosedCashTotal = differenceClosedCashTotal.toResponse(),
    byMovementType = byMovementType.map { it.toResponse() },
)

fun AdminTaxSummaryReport.toResponse(): AdminTaxSummaryReportResponse = AdminTaxSummaryReportResponse(
    organizationId = organizationId,
    branchId = branchId,
    activityId = activityId,
    from = from.toString(),
    to = to.toString(),
    documentCount = documentCount,
    authorizedDocumentCount = authorizedDocumentCount,
    documentGrandTotal = documentGrandTotal.toResponse(),
    taxTotal = taxTotal.toResponse(),
    byTaxRate = byTaxRate.map { it.toResponse() },
)

private fun AdminTaxSummaryLine.toResponse(): AdminTaxSummaryLineResponse = AdminTaxSummaryLineResponse(
    taxCode = taxCode,
    rateCode = rateCode,
    rate = rate.toPlainString(),
    taxableBase = taxableBase.toResponse(),
    taxAmount = taxAmount.toResponse(),
    documentCount = documentCount,
)

private fun AdminTopItemReportLine.toResponse(): AdminTopItemReportLineResponse = AdminTopItemReportLineResponse(
    catalogItemId = catalogItemId,
    name = name,
    quantity = quantity.toPlainString(),
    netTotal = netTotal.toResponse(),
    lineTotal = lineTotal.toResponse(),
)

private fun AdminStatusCount.toResponse(): AdminStatusCountResponse = AdminStatusCountResponse(status = status, count = count)

private fun AdminOperationalAlert.toResponse(): AdminOperationalAlertResponse = AdminOperationalAlertResponse(
    code = code,
    severity = severity,
    message = message,
    actionHint = actionHint,
)

private fun AdminMoneyAmount.toResponse(): AdminOperationsMoneyResponse = AdminOperationsMoneyResponse(
    amount = amount.toPlainString(),
    currency = currency,
)
