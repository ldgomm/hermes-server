package com.hermes.backend.documents

import com.hermes.application.documents.*
import com.hermes.domain.document.*
import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class GenerateInternalTicketRequest(
    val emissionPointId: String? = null,
    val issuedAt: String? = null,
    val notes: String? = null,
    val allowDuplicate: Boolean = false,
)

@Serializable
data class RegisterPhysicalSaleNoteRequest(
    val physicalDocumentNumber: String,
    val emissionPointId: String? = null,
    val issuedAt: String? = null,
    val notes: String? = null,
    val allowDuplicate: Boolean = false,
)

@Serializable
data class EmailCommercialDocumentRequest(
    val emailTo: String,
    val subject: String? = null,
    val message: String? = null,
)

@Serializable
data class CommercialDocumentMoneyResponse(val amount: String, val currency: String)

@Serializable
data class CommercialDocumentQuantityResponse(val value: String, val unitCode: String, val allowsDecimal: Boolean)

@Serializable
data class CommercialDocumentTotalsResponse(
    val subtotal: CommercialDocumentMoneyResponse,
    val discount: CommercialDocumentMoneyResponse,
    val taxTotal: CommercialDocumentMoneyResponse,
    val grandTotal: CommercialDocumentMoneyResponse,
    val paidAmount: CommercialDocumentMoneyResponse,
    val currency: String,
    val paymentStatus: String,
)

@Serializable
data class CommercialDocumentTaxLineResponse(
    val taxCode: String,
    val rateCode: String,
    val rate: String,
    val taxableBase: CommercialDocumentMoneyResponse,
    val amount: CommercialDocumentMoneyResponse,
)

@Serializable
data class CommercialDocumentLineResponse(
    val saleItemId: String,
    val catalogItemId: String,
    val description: String,
    val quantity: CommercialDocumentQuantityResponse,
    val unitPrice: CommercialDocumentMoneyResponse,
    val discount: CommercialDocumentMoneyResponse,
    val netTotal: CommercialDocumentMoneyResponse,
    val taxTotal: CommercialDocumentMoneyResponse,
    val lineTotal: CommercialDocumentMoneyResponse,
    val taxProfileCode: String,
)

@Serializable
data class CommercialDocumentResponse(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String?,
    val saleId: String?,
    val customerId: String?,
    val documentType: String,
    val documentNumber: String,
    val status: String,
    val issuedAt: String,
    val totals: CommercialDocumentTotalsResponse,
    val taxes: List<CommercialDocumentTaxLineResponse>,
    val lines: List<CommercialDocumentLineResponse>,
    val pdfObjectKey: String?,
    val emailedAt: String?,
    val emailTo: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CommercialDocumentsResponse(val documents: List<CommercialDocumentResponse>)

@Serializable
data class EmailCommercialDocumentResponse(val document: CommercialDocumentResponse, val delivered: Boolean)

fun GenerateInternalTicketRequest.toCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    permissions: Set<String>,
): GenerateInternalTicketCommand = GenerateInternalTicketCommand(
    organizationId = organizationId,
    saleId = saleId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    emissionPointId = emissionPointId,
    issuedAt = issuedAt?.let(Instant::parse),
    notes = notes,
    allowDuplicate = allowDuplicate,
)

fun RegisterPhysicalSaleNoteRequest.toCommand(
    organizationId: String,
    saleId: String,
    actorUserId: String,
    permissions: Set<String>,
): RegisterPhysicalSaleNoteCommand = RegisterPhysicalSaleNoteCommand(
    organizationId = organizationId,
    saleId = saleId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    physicalDocumentNumber = physicalDocumentNumber,
    emissionPointId = emissionPointId,
    issuedAt = issuedAt?.let(Instant::parse),
    notes = notes,
    allowDuplicate = allowDuplicate,
)

fun EmailCommercialDocumentRequest.toCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
): EmailCommercialDocumentCommand = EmailCommercialDocumentCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    emailTo = emailTo,
    subject = subject,
    message = message,
)

fun searchCommercialDocumentsCommand(
    organizationId: String,
    actorUserId: String,
    permissions: Set<String>,
    saleId: String?,
    documentType: String?,
    statuses: String?,
    from: String?,
    to: String?,
    limit: Int,
): SearchCommercialDocumentsCommand = SearchCommercialDocumentsCommand(
    organizationId = organizationId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
    saleId = saleId,
    documentType = documentType?.takeIf { it.isNotBlank() }?.let(DocumentType::fromStorage),
    statuses = statuses?.split(',')?.mapNotNull { raw ->
        raw.trim().takeIf { it.isNotBlank() }?.let { enumValueOf<DocumentStatus>(it.uppercase()) }
    }?.toSet().orEmpty(),
    from = from?.let(Instant::parse),
    to = to?.let(Instant::parse),
    limit = limit,
)

fun downloadCommercialDocumentPdfCommand(
    organizationId: String,
    documentId: String,
    actorUserId: String,
    permissions: Set<String>,
): DownloadCommercialDocumentPdfCommand = DownloadCommercialDocumentPdfCommand(
    organizationId = organizationId,
    documentId = documentId,
    actorUserId = actorUserId,
    actorEffectivePermissions = permissions,
)

fun CommercialDocumentResult.toResponse(): CommercialDocumentResponse = document.toResponse()
fun CommercialDocumentsResult.toResponse(): CommercialDocumentsResponse =
    CommercialDocumentsResponse(documents.map { it.toResponse() })

fun CommercialDocument.toResponse(): CommercialDocumentResponse = CommercialDocumentResponse(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    emissionPointId = emissionPointId,
    saleId = saleId,
    customerId = customerId,
    documentType = documentType.storageValue,
    documentNumber = documentNumber,
    status = status.name,
    issuedAt = issuedAt.toString(),
    totals = CommercialDocumentTotalsResponse(
        subtotal = totalsSnapshot.subtotal.toResponse(),
        discount = totalsSnapshot.discount.toResponse(),
        taxTotal = totalsSnapshot.taxTotal.toResponse(),
        grandTotal = totalsSnapshot.grandTotal.toResponse(),
        paidAmount = totalsSnapshot.paidAmount.toResponse(),
        currency = totalsSnapshot.currency.value,
        paymentStatus = totalsSnapshot.paymentStatus,
    ),
    taxes = taxSnapshot.taxes.map { it.toResponse() },
    lines = lineSnapshots.map { it.toResponse() },
    pdfObjectKey = pdfObjectKey,
    emailedAt = emailedAt?.toString(),
    emailTo = emailTo,
    notes = notes,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

private fun CommercialDocumentTaxLineSnapshot.toResponse(): CommercialDocumentTaxLineResponse =
    CommercialDocumentTaxLineResponse(
        taxCode = taxCode,
        rateCode = rateCode,
        rate = rate.value.toPlainString(),
        taxableBase = taxableBase.toResponse(),
        amount = amount.toResponse(),
    )

private fun CommercialDocumentLineSnapshot.toResponse(): CommercialDocumentLineResponse =
    CommercialDocumentLineResponse(
        saleItemId = saleItemId,
        catalogItemId = catalogItemId,
        description = description,
        quantity = quantity.toResponse(),
        unitPrice = unitPrice.toResponse(),
        discount = discount.toResponse(),
        netTotal = netTotal.toResponse(),
        taxTotal = taxTotal.toResponse(),
        lineTotal = lineTotal.toResponse(),
        taxProfileCode = taxProfileSnapshot.code,
    )

private fun Quantity.toResponse(): CommercialDocumentQuantityResponse = CommercialDocumentQuantityResponse(
    value = value.toPlainString(),
    unitCode = unitCode,
    allowsDecimal = allowsDecimal,
)

private fun Money.toResponse(): CommercialDocumentMoneyResponse = CommercialDocumentMoneyResponse(
    amount = amount.toPlainString(),
    currency = currency.value,
)
