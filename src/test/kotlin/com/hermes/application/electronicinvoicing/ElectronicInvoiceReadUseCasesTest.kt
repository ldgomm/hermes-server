package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerationCommand
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerator
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.time.Instant
import java.time.LocalDate

class ElectronicInvoiceReadUseCasesTest {
    @Test
    fun `gets electronic invoice detail with view permission`() {
        val repository = ReadUseCaseElectronicInvoiceQueryRepository()
        val record = issueRecord(id = "edoc_1", organizationId = ORG, saleId = SALE, sequential = 1)
        repository.records += record

        val result = GetElectronicInvoiceUseCase(repository).execute(
            GetElectronicInvoiceCommand(
                organizationId = ORG,
                documentId = "edoc_1",
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW),
            )
        )

        assertEquals(record, result.record)
    }

    @Test
    fun `lists electronic invoices scoped by organization and filters`() {
        val repository = ReadUseCaseElectronicInvoiceQueryRepository()
        repository.records += issueRecord(id = "edoc_1", organizationId = ORG, saleId = SALE, sequential = 1)
        repository.records += issueRecord(id = "edoc_2", organizationId = ORG, saleId = SALE, sequential = 2)
            .copy(status = ElectronicDocumentStatus.AUTHORIZED, authorizedAt = NOW.plusSeconds(2))
        repository.records += issueRecord(id = "edoc_other_org", organizationId = "org_other", saleId = SALE, sequential = 3)
            .copy(status = ElectronicDocumentStatus.AUTHORIZED, authorizedAt = NOW.plusSeconds(3))

        val result = ListElectronicInvoicesUseCase(repository).execute(
            ListElectronicInvoicesCommand(
                organizationId = ORG,
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_LIST),
                saleId = SALE,
                statuses = setOf(ElectronicDocumentStatus.AUTHORIZED),
                environment = SriEnvironment.TEST,
                limit = 50,
            )
        )

        assertEquals(listOf("edoc_2"), result.records.map { it.id })
    }

    @Test
    fun `rejects missing list permission`() {
        val repository = ReadUseCaseElectronicInvoiceQueryRepository()
        val error = assertFailsWith<DomainRuleViolation> {
            ListElectronicInvoicesUseCase(repository).execute(
                ListElectronicInvoicesCommand(
                    organizationId = ORG,
                    actorUserId = USER,
                    actorEffectivePermissions = emptySet(),
                )
            )
        }

        assertTrue(error.message!!.contains(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_LIST))
    }

    private companion object {
        const val ORG = "org_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T10:00:00Z")
    }
}

private class ReadUseCaseElectronicInvoiceQueryRepository : ElectronicInvoiceIssueQueryRepository {
    val records: MutableList<ElectronicInvoiceIssueRecord> = mutableListOf()

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        records.firstOrNull { it.organizationId == organizationId && it.id == documentId }

    override fun search(query: ElectronicInvoiceIssueSearchQuery): List<ElectronicInvoiceIssueRecord> = records
        .asSequence()
        .filter { it.organizationId == query.organizationId }
        .filter { query.saleId == null || it.saleId == query.saleId }
        .filter { query.statuses.isEmpty() || it.status in query.statuses }
        .filter { query.environment == null || it.environment == query.environment }
        .filter { query.from == null || !it.issuedAt.isBefore(query.from) }
        .filter { query.to == null || !it.issuedAt.isAfter(query.to) }
        .sortedByDescending { it.issuedAt }
        .take(query.limit)
        .toList()
}

private fun issueRecord(
    id: String,
    organizationId: String,
    saleId: String,
    sequential: Int,
    issuedAt: Instant = Instant.parse("2026-05-18T10:00:00Z").plusSeconds(sequential.toLong()),
): ElectronicInvoiceIssueRecord {
    val series = SriSeries("001", "002")
    val accessKey = SriAccessKeyGenerator.generate(
        SriAccessKeyGenerationCommand(
            issuedDate = LocalDate.of(2026, 5, 18),
            documentType = SriDocumentType.INVOICE,
            ruc = "1790012345001",
            environment = SriEnvironment.TEST,
            series = series,
            sequential = SriSequential(sequential),
            numericCode = SriNumericCode("12345678"),
        )
    )

    return ElectronicInvoiceIssueRecord.accessKeyGenerated(
        id = id,
        organizationId = organizationId,
        branchId = "br_1",
        emissionPointId = "emi_1",
        saleId = saleId,
        environment = SriEnvironment.TEST,
        documentType = SriDocumentType.INVOICE,
        series = series,
        documentNumber = "001-002-${sequential.toString().padStart(9, '0')}",
        accessKey = accessKey,
        authorizationNumber = accessKey.value,
        issuedAt = issuedAt,
        actorUserId = "usr_1",
    )
}
