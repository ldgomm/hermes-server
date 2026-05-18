package com.hermes.domain.document

import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

@Suppress("LongParameterList")
data class CommercialDocument(
    val id: String,
    val organizationId: String,
    val branchId: String,
    val emissionPointId: String?,
    val saleId: String?,
    val customerId: String?,
    val documentType: DocumentType,
    val documentNumber: String,
    val accessKey: String?,
    val authorizationNumber: String?,
    val status: DocumentStatus,
    val issuedAt: Instant,
    val authorizedAt: Instant?,
    val totalsSnapshot: CommercialDocumentTotalsSnapshot,
    val taxSnapshot: CommercialDocumentTaxSnapshot,
    val lineSnapshots: List<CommercialDocumentLineSnapshot>,
    val payloadId: String?,
    val pdfObjectKey: String?,
    val emailedAt: Instant?,
    val emailTo: String?,
    val notes: String?,
    val createdAt: Instant,
    val createdBy: String?,
    val updatedAt: Instant,
    val updatedBy: String?,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Commercial document id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Commercial document organization id cannot be blank.")
        if (branchId.isBlank()) throw DomainRuleViolation("Commercial document branch id cannot be blank.")
        if (documentNumber.isBlank()) throw DomainRuleViolation("Commercial document number cannot be blank.")
        if (!documentType.isPhase10OperationalDocument) {
            throw DomainRuleViolation("Document type $documentType belongs to a later electronic-document phase.")
        }
        if (lineSnapshots.isEmpty()) throw DomainRuleViolation("Commercial document requires at least one line snapshot.")
        if (accessKey != null || authorizationNumber != null || authorizedAt != null) {
            throw DomainRuleViolation("Phase 10 operational documents cannot store SRI access or authorization data.")
        }
        if (status !in setOf(DocumentStatus.DRAFT, DocumentStatus.GENERATED, DocumentStatus.ERROR)) {
            throw DomainRuleViolation("Phase 10 operational documents can only be draft, generated or error.")
        }
        if (updatedAt.isBefore(createdAt)) throw DomainRuleViolation("Commercial document updatedAt cannot be before createdAt.")
        if (version < 1) throw DomainRuleViolation("Commercial document version must be positive.")
    }

    fun markGenerated(
        payloadId: String?,
        pdfObjectKey: String?,
        updatedAt: Instant,
        updatedBy: String?,
    ): CommercialDocument {
        DocumentStatusRules.assertCanGenerate(status)
        return copy(
            status = DocumentStatus.GENERATED,
            payloadId = payloadId?.trim()?.takeIf { it.isNotBlank() },
            pdfObjectKey = pdfObjectKey?.trim()?.takeIf { it.isNotBlank() },
            updatedAt = updatedAt,
            updatedBy = updatedBy,
            version = version + 1,
        )
    }

    fun attachPdf(
        pdfObjectKey: String,
        updatedAt: Instant,
        updatedBy: String?,
    ): CommercialDocument {
        if (status == DocumentStatus.ERROR) throw DomainRuleViolation("Cannot attach PDF to errored commercial document.")
        return copy(
            pdfObjectKey = pdfObjectKey.trim().takeIf { it.isNotBlank() }
                ?: throw DomainRuleViolation("PDF object key cannot be blank."),
            updatedAt = updatedAt,
            updatedBy = updatedBy,
            version = version + 1,
        )
    }

    fun markEmailSent(emailTo: String, sentAt: Instant, updatedBy: String?): CommercialDocument {
        if (status != DocumentStatus.GENERATED) {
            throw DomainRuleViolation("Only generated commercial documents can be emailed.")
        }
        if (pdfObjectKey.isNullOrBlank()) throw DomainRuleViolation("Commercial document must have a PDF before email delivery.")
        return copy(
            emailedAt = sentAt,
            emailTo = emailTo.trim().takeIf { it.isNotBlank() }
                ?: throw DomainRuleViolation("Document email recipient cannot be blank."),
            updatedAt = sentAt,
            updatedBy = updatedBy,
            version = version + 1,
        )
    }

    companion object {
        @Suppress("LongParameterList")
        fun draftFromSale(
            id: String,
            sale: Sale,
            documentType: DocumentType,
            documentNumber: String,
            emissionPointId: String? = null,
            issuedAt: Instant,
            createdBy: String,
            notes: String? = null,
        ): CommercialDocument {
            if (!documentType.isPhase10OperationalDocument) {
                throw DomainRuleViolation("Only internal ticket and physical sale note registry can be generated in Phase 10.")
            }
            if (sale.operationalStatus == SaleOperationalStatus.CANCELED) {
                throw DomainRuleViolation("Cannot generate a commercial document for a canceled sale.")
            }
            if (sale.activeItems.isEmpty()) {
                throw DomainRuleViolation("Cannot generate a commercial document for a sale without active items.")
            }

            val totals = sale.totals
            val taxLines = sale.activeItems.flatMap { item ->
                item.taxes.map { tax ->
                    CommercialDocumentTaxLineSnapshot(
                        taxCode = tax.taxCode,
                        rateCode = tax.rateCode,
                        rate = tax.rate,
                        taxableBase = tax.taxableBase,
                        amount = tax.amount,
                    )
                }
            }

            return CommercialDocument(
                id = id,
                organizationId = sale.organizationId,
                branchId = sale.branchId,
                emissionPointId = emissionPointId?.trim()?.takeIf { it.isNotBlank() },
                saleId = sale.id,
                customerId = sale.customerId,
                documentType = documentType,
                documentNumber = documentNumber.trim(),
                accessKey = null,
                authorizationNumber = null,
                status = DocumentStatus.DRAFT,
                issuedAt = issuedAt,
                authorizedAt = null,
                totalsSnapshot = CommercialDocumentTotalsSnapshot(
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    taxTotal = totals.taxTotal,
                    grandTotal = totals.grandTotal,
                    paidAmount = sale.paidAmount,
                    currency = totals.currency,
                    paymentStatus = sale.paymentStatus.name,
                ),
                taxSnapshot = CommercialDocumentTaxSnapshot(
                    taxTotal = totals.taxTotal,
                    taxes = taxLines,
                ),
                lineSnapshots = sale.activeItems.map { item ->
                    CommercialDocumentLineSnapshot(
                        saleItemId = item.id,
                        catalogItemId = item.catalogItemId,
                        description = item.name,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        discount = item.discount,
                        netTotal = item.netTotal,
                        taxTotal = item.taxTotal,
                        lineTotal = item.lineTotal,
                        taxProfileSnapshot = item.taxProfileSnapshot,
                    )
                },
                payloadId = null,
                pdfObjectKey = null,
                emailedAt = null,
                emailTo = null,
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                createdAt = issuedAt,
                createdBy = createdBy,
                updatedAt = issuedAt,
                updatedBy = createdBy,
                version = 1,
            )
        }
    }
}
