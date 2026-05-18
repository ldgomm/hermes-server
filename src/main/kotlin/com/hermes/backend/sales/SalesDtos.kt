package com.hermes.backend.sales

import com.hermes.application.sales.AddSaleItemCommand
import com.hermes.application.sales.CancelSaleCommand
import com.hermes.application.sales.ChangeSaleItemStatusCommand
import com.hermes.application.sales.ChangeSaleStatusCommand
import com.hermes.application.sales.CloseSaleCommand
import com.hermes.application.sales.CreateQuickSaleCommand
import com.hermes.application.sales.CreateReservationCommand
import com.hermes.application.sales.CreateSaleItemCommandLine
import com.hermes.application.sales.ReservationResult
import com.hermes.application.sales.ReservationsResult
import com.hermes.application.sales.SaleResult
import com.hermes.application.sales.SalesResult
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleItemStatus
import com.hermes.domain.sale.SaleItemTax
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.tax.PriceTaxMode
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class SalesMoneyRequest(
    val amount: String,
    val currency: String = "USD",
)

@Serializable
data class SalesQuantityRequest(
    val value: String,
    val unitCode: String = "unit",
    val allowsDecimal: Boolean = false,
)

@Serializable
data class SalesCustomerSnapshotRequest(
    val customerId: String? = null,
    val displayName: String = "Consumidor final",
    val taxId: String? = "9999999999999",
    val taxIdType: String? = "final_consumer",
    val email: String? = null,
)

@Serializable
data class CreateQuickSaleRequest(
    val branchId: String,
    val activityId: String,
    val saleNumber: String? = null,
    val customerId: String? = null,
    val customerSnapshot: SalesCustomerSnapshotRequest? = null,
    val cashSessionId: String? = null,
    val occurredAt: String? = null,
    val autoConfirm: Boolean = true,
    val items: List<CreateSaleItemLineRequest>,
)

@Serializable
data class CreateSaleItemLineRequest(
    val catalogItemId: String,
    val quantity: SalesQuantityRequest,
    val unitPrice: SalesMoneyRequest? = null,
    val discount: SalesMoneyRequest? = null,
    val priceTaxMode: String = "TAX_EXCLUSIVE",
)

@Serializable
data class ChangeSaleStatusRequest(
    val targetStatus: String,
    val reason: String,
)

@Serializable
data class ChangeSaleItemStatusRequest(
    val targetStatus: String,
    val reason: String,
)

@Serializable
data class SaleReasonRequest(
    val reason: String,
)

@Serializable
data class CreateReservationRequest(
    val branchId: String,
    val activityId: String,
    val customerId: String? = null,
    val customerSnapshot: SalesCustomerSnapshotRequest? = null,
    val resourceId: String? = null,
    val startAt: String,
    val endAt: String,
    val partySize: Int = 1,
    val notes: String? = null,
    val linkedSaleItem: CreateSaleItemLineRequest? = null,
    val cashSessionId: String? = null,
)

@Serializable
data class SalesMoneyResponse(
    val amount: String,
    val currency: String,
)

@Serializable
data class SalesQuantityResponse(
    val value: String,
    val unitCode: String,
    val allowsDecimal: Boolean,
)

@Serializable
data class CustomerSnapshotResponse(
    val customerId: String?,
    val displayName: String,
    val taxId: String?,
    val taxIdType: String?,
    val email: String?,
)

@Serializable
data class CatalogItemSnapshotResponse(
    val catalogItemId: String,
    val sourceTemplateId: String?,
    val globalCatalogId: String,
    val productFamilyId: String?,
    val name: String,
    val type: String,
    val taxProfileId: String,
    val unitCode: String,
)

@Serializable
data class SaleTaxProfileSnapshotResponse(
    val code: String,
    val taxName: String,
    val rate: String,
    val sriTaxCode: String,
    val sriRateCode: String,
    val treatment: String,
    val legalBasis: String,
    val effectiveFrom: String,
    val source: String,
)

@Serializable
data class SaleItemTaxResponse(
    val taxCode: String,
    val rateCode: String,
    val rate: String,
    val taxableBase: SalesMoneyResponse,
    val amount: SalesMoneyResponse,
)

@Serializable
data class SaleItemResponse(
    val id: String,
    val catalogItemId: String,
    val name: String,
    val unitPrice: SalesMoneyResponse,
    val quantity: SalesQuantityResponse,
    val discount: SalesMoneyResponse,
    val status: String,
    val grossTotal: SalesMoneyResponse,
    val netTotal: SalesMoneyResponse,
    val taxTotal: SalesMoneyResponse,
    val lineTotal: SalesMoneyResponse,
    val catalogSnapshot: CatalogItemSnapshotResponse,
    val taxProfileSnapshot: SaleTaxProfileSnapshotResponse,
    val taxes: List<SaleItemTaxResponse>,
)

@Serializable
data class SaleTotalsResponse(
    val subtotal: SalesMoneyResponse,
    val discount: SalesMoneyResponse,
    val taxTotal: SalesMoneyResponse,
    val grandTotal: SalesMoneyResponse,
    val currency: String,
)

@Serializable
data class SaleResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleNumber: String?,
    val saleType: String,
    val workflowMode: String,
    val customerId: String?,
    val customerSnapshot: CustomerSnapshotResponse,
    val items: List<SaleItemResponse>,
    val operationalStatus: String,
    val paymentStatus: String,
    val paidAmount: SalesMoneyResponse,
    val totals: SaleTotalsResponse,
    val dueAt: String?,
    val cashSessionId: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SalesResponse(
    val sales: List<SaleResponse>,
)

@Serializable
data class ReservationResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val activityId: String,
    val saleId: String?,
    val customerId: String?,
    val customerSnapshot: CustomerSnapshotResponse,
    val resourceId: String?,
    val startAt: String,
    val endAt: String,
    val partySize: Int,
    val status: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReservationWithSaleResponse(
    val reservation: ReservationResponse,
    val linkedSale: SaleResponse?,
)

@Serializable
data class ReservationsResponse(
    val reservations: List<ReservationResponse>,
)

fun CreateQuickSaleRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CreateQuickSaleCommand =
    CreateQuickSaleCommand(
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        saleNumber = saleNumber,
        customerId = customerId,
        customerSnapshot = customerSnapshot?.toDomain() ?: CustomerSnapshot.finalConsumer(),
        cashSessionId = cashSessionId,
        occurredAt = occurredAt?.let(Instant::parse) ?: Instant.now(),
        autoConfirm = autoConfirm,
        items = items.map { it.toCommandLine() },
    )

fun CreateSaleItemLineRequest.toAddCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): AddSaleItemCommand =
    AddSaleItemCommand(
        organizationId = organizationId,
        saleId = saleId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        occurredAt = Instant.now(),
        item = toCommandLine(),
    )

fun ChangeSaleStatusRequest.toCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): ChangeSaleStatusCommand =
    ChangeSaleStatusCommand(
        organizationId = organizationId,
        saleId = saleId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        targetStatus = SaleOperationalStatus.valueOf(targetStatus.trim().uppercase()),
        reason = reason,
    )

fun ChangeSaleItemStatusRequest.toCommand(
    organizationId: String,
    saleId: String,
    saleItemId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): ChangeSaleItemStatusCommand =
    ChangeSaleItemStatusCommand(
        organizationId = organizationId,
        saleId = saleId,
        saleItemId = saleItemId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        targetStatus = SaleItemStatus.valueOf(targetStatus.trim().uppercase()),
        reason = reason,
    )

fun SaleReasonRequest.toCancelCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CancelSaleCommand =
    CancelSaleCommand(
        organizationId = organizationId,
        saleId = saleId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        reason = reason,
    )

fun SaleReasonRequest.toCloseCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CloseSaleCommand =
    CloseSaleCommand(
        organizationId = organizationId,
        saleId = saleId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        reason = reason,
    )

fun CreateReservationRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): CreateReservationCommand =
    CreateReservationCommand(
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        customerId = customerId,
        customerSnapshot = customerSnapshot?.toDomain() ?: CustomerSnapshot.finalConsumer(),
        resourceId = resourceId,
        startAt = Instant.parse(startAt),
        endAt = Instant.parse(endAt),
        partySize = partySize,
        notes = notes,
        linkedSaleItem = linkedSaleItem?.toCommandLine(),
        cashSessionId = cashSessionId,
    )

fun SaleResult.toResponse(): SaleResponse = sale.toResponse()
fun SalesResult.toResponse(): SalesResponse = SalesResponse(sales.map { it.toResponse() })
fun ReservationResult.toResponse(): ReservationWithSaleResponse =
    ReservationWithSaleResponse(reservation.toResponse(), linkedSale?.toResponse())
fun ReservationsResult.toResponse(): ReservationsResponse = ReservationsResponse(reservations.map { it.toResponse() })

fun Sale.toResponse(): SaleResponse =
    SaleResponse(
        id = id,
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        saleNumber = saleNumber,
        saleType = saleType.name,
        workflowMode = workflowMode.name,
        customerId = customerId,
        customerSnapshot = customerSnapshot.toResponse(),
        items = items.map { it.toResponse() },
        operationalStatus = operationalStatus.name,
        paymentStatus = paymentStatus.name,
        paidAmount = paidAmount.toResponse(),
        totals = SaleTotalsResponse(
            subtotal = totals.subtotal.toResponse(),
            discount = totals.discount.toResponse(),
            taxTotal = totals.taxTotal.toResponse(),
            grandTotal = totals.grandTotal.toResponse(),
            currency = totals.currency.value,
        ),
        dueAt = dueAt?.toString(),
        cashSessionId = cashSessionId,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

fun Reservation.toResponse(): ReservationResponse =
    ReservationResponse(
        id = id,
        organizationId = organizationId,
        branchId = branchId,
        activityId = activityId,
        saleId = saleId,
        customerId = customerId,
        customerSnapshot = customerSnapshot.toResponse(),
        resourceId = resourceId,
        startAt = startAt.toString(),
        endAt = endAt.toString(),
        partySize = partySize,
        status = status.name,
        notes = notes,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

private fun CreateSaleItemLineRequest.toCommandLine(): CreateSaleItemCommandLine =
    CreateSaleItemCommandLine(
        catalogItemId = catalogItemId,
        quantity = quantity.toDomain(),
        unitPrice = unitPrice?.toDomain(),
        discount = discount?.toDomain(),
        priceTaxMode = PriceTaxMode.valueOf(priceTaxMode.trim().uppercase()),
    )

private fun SalesCustomerSnapshotRequest.toDomain(): CustomerSnapshot =
    CustomerSnapshot(
        customerId = customerId,
        displayName = displayName,
        taxId = taxId,
        taxIdType = taxIdType,
        email = email,
    )

private fun SalesMoneyRequest.toDomain(): Money =
    Money.of(amount = BigDecimal(amount), currency = CurrencyCode(currency.trim().uppercase()))

private fun SalesQuantityRequest.toDomain(): Quantity =
    Quantity.of(value = BigDecimal(value), unitCode = unitCode.trim(), allowsDecimal = allowsDecimal)

private fun SaleItem.toResponse(): SaleItemResponse =
    SaleItemResponse(
        id = id,
        catalogItemId = catalogItemId,
        name = name,
        unitPrice = unitPrice.toResponse(),
        quantity = quantity.toResponse(),
        discount = discount.toResponse(),
        status = status.name,
        grossTotal = grossTotal.toResponse(),
        netTotal = netTotal.toResponse(),
        taxTotal = taxTotal.toResponse(),
        lineTotal = lineTotal.toResponse(),
        catalogSnapshot = catalogSnapshot.toResponse(),
        taxProfileSnapshot = taxProfileSnapshot.toResponse(),
        taxes = taxes.map { it.toResponse() },
    )

private fun CustomerSnapshot.toResponse(): CustomerSnapshotResponse =
    CustomerSnapshotResponse(customerId, displayName, taxId, taxIdType, email)

private fun CatalogItemSnapshot.toResponse(): CatalogItemSnapshotResponse =
    CatalogItemSnapshotResponse(
        catalogItemId = catalogItemId,
        sourceTemplateId = sourceTemplateId,
        globalCatalogId = globalCatalogId,
        productFamilyId = productFamilyId,
        name = name,
        type = type.name,
        taxProfileId = taxProfileId,
        unitCode = unitCode,
    )

private fun TaxProfileSnapshotForSale.toResponse(): SaleTaxProfileSnapshotResponse =
    SaleTaxProfileSnapshotResponse(
        code = code,
        taxName = taxName,
        rate = rate.value.toPlainString(),
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        treatment = treatment.name,
        legalBasis = legalBasis,
        effectiveFrom = effectiveFrom.toString(),
        source = source,
    )

private fun SaleItemTax.toResponse(): SaleItemTaxResponse =
    SaleItemTaxResponse(
        taxCode = taxCode,
        rateCode = rateCode,
        rate = rate.value.toPlainString(),
        taxableBase = taxableBase.toResponse(),
        amount = amount.toResponse(),
    )

private fun Money.toResponse(): SalesMoneyResponse =
    SalesMoneyResponse(amount = amount.toPlainString(), currency = currency.value)

private fun Quantity.toResponse(): SalesQuantityResponse =
    SalesQuantityResponse(value = value.toPlainString(), unitCode = unitCode, allowsDecimal = allowsDecimal)
