package com.hermes.application.documents

import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleItemTax
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommercialDocumentEmailUseCaseTest {
    @Test
    fun `emails generated document and stores recipient metadata`() {
        val documents = InMemoryCommercialDocumentRepositoryForTest()
        val storage = InMemoryCommercialDocumentFileStorage()
        val emailSender = RecordingCommercialDocumentEmailSender()
        val document = generatedDocument()
        documents.create(document)
        storage.put(
            CommercialDocumentFile(
                objectKey = document.pdfObjectKey!!,
                filename = "doc_1.pdf",
                contentType = "application/pdf",
                bytes = "%PDF test".toByteArray(),
            )
        )
        val useCase = EmailCommercialDocumentUseCase(
            documentRepository = documents,
            fileStorage = storage,
            emailSender = emailSender,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )

        val result = useCase.execute(
            EmailCommercialDocumentCommand(
                organizationId = ORG,
                documentId = "doc_1",
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF),
                emailTo = "cliente@example.com",
            )
        )

        assertTrue(result.delivered)
        assertEquals("cliente@example.com", result.document.emailTo)
        assertEquals(NOW, result.document.emailedAt)
        assertEquals(1, emailSender.sent.size)
        assertEquals("cliente@example.com", documents.findById(ORG, "doc_1")!!.emailTo)
    }

    @Test
    fun `rejects email when generated pdf file is missing`() {
        val documents = InMemoryCommercialDocumentRepositoryForTest()
        documents.create(generatedDocument())
        val useCase = EmailCommercialDocumentUseCase(
            documentRepository = documents,
            fileStorage = InMemoryCommercialDocumentFileStorage(),
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                EmailCommercialDocumentCommand(
                    organizationId = ORG,
                    documentId = "doc_1",
                    actorUserId = USER,
                    actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF),
                    emailTo = "cliente@example.com",
                )
            )
        }
    }

    private fun generatedDocument(): CommercialDocument = CommercialDocument.draftFromSale(
        id = "doc_1",
        sale = sale(),
        documentType = DocumentType.INTERNAL_TICKET,
        documentNumber = "TCK-2026-05-18-000000001",
        issuedAt = NOW,
        createdBy = USER,
    ).markGenerated(
        payloadId = null,
        pdfObjectKey = "commercial-documents/org_1/doc_1.pdf",
        updatedAt = NOW,
        updatedBy = USER,
    )

    private fun sale(): Sale {
        val tax = TaxProfileSnapshotForSale(
            code = "iva_15",
            taxName = "IVA",
            rate = Percentage.of("15.00"),
            sriTaxCode = "2",
            sriRateCode = "4",
            treatment = TaxTreatment.IVA_FULL,
            legalBasis = "SRI vigente",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "test",
        )
        val item = SaleItem.create(
            id = "si_1",
            catalogItemId = "item_1",
            name = "Producto de prueba",
            unitPrice = Money.of("10.00"),
            quantity = Quantity.units(1),
            catalogSnapshot = CatalogItemSnapshot(
                catalogItemId = "item_1",
                sourceTemplateId = null,
                globalCatalogId = "global_1",
                productFamilyId = null,
                name = "Producto de prueba",
                type = CatalogItemType.PRODUCT,
                taxProfileId = "iva_15",
                unitCode = "unit",
            ),
            taxProfileSnapshot = tax,
            taxes = listOf(
                SaleItemTax(
                    taxCode = "2",
                    rateCode = "4",
                    rate = Percentage.of("15.00"),
                    taxableBase = Money.of("10.00"),
                    amount = Money.of("1.50"),
                )
            ),
        )
        return Sale.createDraft(
            id = SALE,
            organizationId = ORG,
            branchId = BRANCH,
            activityId = ACTIVITY,
            saleType = SaleType.SALE,
            workflowMode = SaleWorkflowMode.QUICK_SALE,
            saleNumber = "SALE-1",
            customerSnapshot = CustomerSnapshot.finalConsumer(),
            createdAt = NOW,
        ).addItem(item, NOW).confirm(NOW)
    }

    private companion object {
        const val ORG = "org_1"
        const val BRANCH = "br_1"
        const val ACTIVITY = "act_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}
