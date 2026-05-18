package com.hermes.application.documents

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.document.CommercialDocument
import com.hermes.domain.document.DocumentStatus
import com.hermes.domain.document.DocumentType
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.*
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.*

class CommercialDocumentUseCasesTest {
    @Test
    fun `generates internal ticket with pdf and sale snapshots`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        val result = fixture.generateInternalTicket.execute(
            GenerateInternalTicketCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                issuedAt = NOW,
            )
        )

        assertEquals(DocumentType.INTERNAL_TICKET, result.document.documentType)
        assertEquals(DocumentStatus.GENERATED, result.document.status)
        assertEquals("TCK-2026-05-18-000000001", result.document.documentNumber)
        assertEquals(Money.of("11.50"), result.document.totalsSnapshot.grandTotal)
        assertNotNull(result.document.pdfObjectKey)
        assertTrue(result.file!!.bytes.toString(Charsets.UTF_8).startsWith("%PDF"))
    }

    @Test
    fun `does not generate duplicate internal ticket unless explicitly allowed`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        val first = fixture.generateInternalTicket.execute(
            GenerateInternalTicketCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                issuedAt = NOW,
            )
        )
        val second = fixture.generateInternalTicket.execute(
            GenerateInternalTicketCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                issuedAt = NOW.plusSeconds(1),
            )
        )

        assertEquals(first.document.id, second.document.id)
        assertEquals(1, fixture.documents.search(CommercialDocumentSearchQuery(ORG)).size)
    }

    @Test
    fun `registers physical sale note with provided number`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        val result = fixture.registerPhysicalSaleNote.execute(
            RegisterPhysicalSaleNoteCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                physicalDocumentNumber = "001-001-000000123",
                issuedAt = NOW,
            )
        )

        assertEquals(DocumentType.PHYSICAL_SALE_NOTE_REGISTRY, result.document.documentType)
        assertEquals("001-001-000000123", result.document.documentNumber)
        assertEquals(DocumentStatus.GENERATED, result.document.status)
    }

    @Test
    fun `rejects internal ticket without permission`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.generateInternalTicket.execute(
                GenerateInternalTicketCommand(
                    organizationId = ORG,
                    saleId = SALE,
                    actorUserId = USER,
                    actorEffectivePermissions = emptySet(),
                    issuedAt = NOW,
                )
            )
        }
    }

    @Test
    fun `downloads generated pdf`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())
        val created = fixture.generateInternalTicket.execute(
            GenerateInternalTicketCommand(ORG, SALE, USER, permissions(), issuedAt = NOW)
        )

        val downloaded = fixture.downloadPdf.execute(
            DownloadCommercialDocumentPdfCommand(
                organizationId = ORG,
                documentId = created.document.id,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
            )
        )

        assertEquals(created.document.id, downloaded.document.id)
        assertEquals("application/pdf", downloaded.file!!.contentType)
    }

    private fun fixture(): Fixture {
        val sales = InMemorySaleRepository()
        val documents = InMemoryCommercialDocumentRepository()
        val storage = InMemoryCommercialDocumentFileStorage()
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val idGenerator = object : CommercialDocumentIdGenerator {
            var next = 1
            override fun newId(prefix: String): String = "${prefix}_${next++}"
        }
        val numberGenerator = object : CommercialDocumentNumberGenerator {
            var next = 1L
            override fun nextInternalTicketNumber(organizationId: String, branchId: String, issuedAt: Instant): String =
                "TCK-${issuedAt.atOffset(ZoneOffset.UTC).toLocalDate()}-${next++.toString().padStart(9, '0')}"
        }
        val renderer = SimpleCommercialDocumentPdfRenderer()
        return Fixture(
            sales = sales,
            documents = documents,
            generateInternalTicket = GenerateInternalTicketUseCase(
                saleRepository = sales,
                documentRepository = documents,
                numberGenerator = numberGenerator,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                clock = clock,
            ),
            registerPhysicalSaleNote = RegisterPhysicalSaleNoteUseCase(
                saleRepository = sales,
                documentRepository = documents,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                clock = clock,
            ),
            downloadPdf = DownloadCommercialDocumentPdfUseCase(
                documentRepository = documents,
                fileStorage = storage,
                clock = clock,
            ),
        )
    }

    private data class Fixture(
        val sales: InMemorySaleRepository,
        val documents: InMemoryCommercialDocumentRepository,
        val generateInternalTicket: GenerateInternalTicketUseCase,
        val registerPhysicalSaleNote: RegisterPhysicalSaleNoteUseCase,
        val downloadPdf: DownloadCommercialDocumentPdfUseCase,
    )

    private class InMemorySaleRepository : OperationalSaleRepository {
        private val data = linkedMapOf<String, Sale>()
        override fun create(sale: Sale) {
            data[sale.id] = sale
        }

        override fun update(sale: Sale) {
            data[sale.id] = sale
        }

        override fun findById(organizationId: String, saleId: String): Sale? =
            data[saleId]?.takeIf { it.organizationId == organizationId }

        override fun search(query: SaleSearchQuery): List<Sale> =
            data.values.filter { it.organizationId == query.organizationId }
    }

    private class InMemoryCommercialDocumentRepository : CommercialDocumentRepository {
        private val data = linkedMapOf<String, CommercialDocument>()
        override fun create(document: CommercialDocument) {
            data[document.id] = document
        }

        override fun update(document: CommercialDocument) {
            data[document.id] = document
        }

        override fun findById(organizationId: String, documentId: String): CommercialDocument? =
            data[documentId]?.takeIf { it.organizationId == organizationId }

        override fun findBySale(organizationId: String, saleId: String): List<CommercialDocument> =
            data.values.filter { it.organizationId == organizationId && it.saleId == saleId }

        override fun findByDocumentNumber(organizationId: String, documentNumber: String): CommercialDocument? =
            data.values.firstOrNull { it.organizationId == organizationId && it.documentNumber == documentNumber }

        override fun search(query: CommercialDocumentSearchQuery): List<CommercialDocument> =
            data.values.filter { document ->
                document.organizationId == query.organizationId &&
                        (query.saleId == null || document.saleId == query.saleId) &&
                        (query.documentType == null || document.documentType == query.documentType) &&
                        (query.statuses.isEmpty() || document.status in query.statuses)
            }
    }

    private fun confirmedSale(): Sale {
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

    private fun permissions(): Set<String> = setOf(
        PermissionCatalog.DOCUMENTS_GENERATE_INTERNAL_TICKET,
        PermissionCatalog.DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY,
        PermissionCatalog.DOCUMENTS_VIEW,
        PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF,
    )

    companion object {
        private const val ORG = "org_1"
        private const val BRANCH = "br_1"
        private const val ACTIVITY = "act_1"
        private const val SALE = "sale_1"
        private const val USER = "usr_1"
        private val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}
