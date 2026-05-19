package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ElectronicInvoiceOperationUseCasesTest {
    @Test
    fun `retry authorization queries SRI without resubmitting XML`() {
        val repository = InMemoryOperationIssueRepository()
        val record = operationRecord(status = ElectronicDocumentStatus.AUTHORIZATION_PENDING)
        repository.create(record)
        val authorizationClient = FakeOperationSriAuthorizationClient(
            result = SriAuthorizationResult(
                environment = SriEnvironment.TEST,
                status = SriAuthorizationStatus.PROCESSING,
                accessKey = record.accessKey,
                messages = listOf(SriMessage("PPR", "Authorization is processing", type = SriMessageType.INFO)),
                rawResponseXml = "<autorizacion/>"
            )
        )
        val artifacts = InMemoryOperationArtifactStorage()
        val queryUseCase = QuerySriAuthorizationUseCase(
            repository = repository,
            artifactStorage = artifacts,
            authorizationClient = authorizationClient,
        )
        val retryUseCase = RetryElectronicInvoiceAuthorizationUseCase(repository, queryUseCase)

        val result = retryUseCase.execute(
            RetryElectronicInvoiceAuthorizationCommand(
                organizationId = "org_1",
                documentId = record.id,
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_RETRY),
            )
        )

        assertEquals(1, authorizationClient.queries)
        assertEquals(ElectronicDocumentStatus.AUTHORIZATION_PENDING, result.record.status)
        assertEquals("processing", result.record.lastSriAuthorizationStatus)
        assertTrue(artifacts.stored.any { it.artifactType == ElectronicDocumentArtifactType.SRI_AUTHORIZATION_RESPONSE })
    }

    @Test
    fun `retry authorization rejects documents that cannot be queried again`() {
        val repository = InMemoryOperationIssueRepository()
        val record = operationRecord(status = ElectronicDocumentStatus.AUTHORIZED)
        repository.create(record)
        val queryUseCase = QuerySriAuthorizationUseCase(
            repository = repository,
            artifactStorage = InMemoryOperationArtifactStorage(),
            authorizationClient = FakeOperationSriAuthorizationClient(),
        )
        val retryUseCase = RetryElectronicInvoiceAuthorizationUseCase(repository, queryUseCase)

        assertFailsWith<DomainRuleViolation> {
            retryUseCase.execute(
                RetryElectronicInvoiceAuthorizationCommand(
                    organizationId = "org_1",
                    documentId = record.id,
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_RETRY),
                )
            )
        }
    }

    @Test
    fun `errors use case returns SRI messages and requires permission`() {
        val repository = InMemoryOperationIssueRepository()
        val record = operationRecord(
            status = ElectronicDocumentStatus.NOT_AUTHORIZED,
            messages = listOf(SriMessage.error("43", "RUC no autorizado")),
        )
        repository.create(record)
        val useCase = GetElectronicInvoiceErrorsUseCase(repository)

        val result = useCase.execute(
            GetElectronicInvoiceErrorsCommand(
                organizationId = "org_1",
                documentId = record.id,
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_ERRORS),
            )
        )

        assertEquals(ElectronicDocumentStatus.NOT_AUTHORIZED, result.record.status)
        assertEquals("RUC no autorizado", result.record.sriMessages.single().message)
    }
}

private class InMemoryOperationIssueRepository : ElectronicInvoiceIssueRepository {
    private val records = linkedMapOf<String, ElectronicInvoiceIssueRecord>()

    override fun create(record: ElectronicInvoiceIssueRecord) {
        records[key(record.organizationId, record.id)] = record
    }

    override fun update(record: ElectronicInvoiceIssueRecord) {
        records[key(record.organizationId, record.id)] = record
    }

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        records[key(organizationId, documentId)]

    override fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean =
        records.values.any {
            it.organizationId == organizationId &&
                    it.saleId == saleId &&
                    it.status in setOf(
                ElectronicDocumentStatus.AUTHORIZED,
                ElectronicDocumentStatus.DELIVERY_PENDING,
                ElectronicDocumentStatus.DELIVERED,
                ElectronicDocumentStatus.DELIVERY_FAILED,
            )
        }

    private fun key(organizationId: String, documentId: String): String = "$organizationId::$documentId"
}

private class FakeOperationSriAuthorizationClient(
    private val result: SriAuthorizationResult? = null,
) : SriAuthorizationClient {
    var queries: Int = 0
        private set

    override fun query(command: SriAuthorizationQueryCommand): SriAuthorizationResult {
        queries++
        return result ?: SriAuthorizationResult(
            environment = command.environment,
            status = SriAuthorizationStatus.PROCESSING,
            accessKey = command.accessKey,
            rawResponseXml = "<autorizacion/>",
            queriedAt = command.requestedAt,
        )
    }
}

private class InMemoryOperationArtifactStorage : ElectronicDocumentArtifactStorage {
    val stored = mutableListOf<StoredElectronicDocumentArtifact>()

    override fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact {
        val artifact = StoredElectronicDocumentArtifact(
            objectKey = "${command.documentId}/${command.artifactType.storageValue}/${stored.size + 1}",
            artifactType = command.artifactType,
            sha256 = sha256(command.content),
            sizeBytes = command.content.size.toLong(),
            createdAt = command.createdAt,
        )
        stored += artifact
        return artifact
    }
}

private fun operationRecord(
    status: ElectronicDocumentStatus,
    messages: List<SriMessage> = emptyList(),
): ElectronicInvoiceIssueRecord {
    val series = SriSeries("001", "002")
    val accessKey = SriAccessKeyGenerator.generate(
        SriAccessKeyGenerationCommand(
            issuedDate = LocalDate.of(2026, 5, 18),
            documentType = SriDocumentType.INVOICE,
            ruc = "1790012345001",
            environment = SriEnvironment.TEST,
            series = series,
            sequential = SriSequential(1),
            numericCode = SriNumericCode("12345678"),
        )
    )
    val now = Instant.parse("2026-05-18T10:00:00Z")

    return ElectronicInvoiceIssueRecord(
        id = "doc_1",
        organizationId = "org_1",
        branchId = "br_1",
        emissionPointId = "emi_1",
        saleId = "sale_1",
        environment = SriEnvironment.TEST,
        documentType = SriDocumentType.INVOICE,
        series = series,
        documentNumber = "001-002-000000001",
        accessKey = accessKey,
        authorizationNumber = accessKey.value,
        status = status,
        signedXmlObjectKey = "doc_1/signed.xml",
        signedXmlSha256 = "a".repeat(64),
        lastSriAuthorizationStatus = if (status == ElectronicDocumentStatus.AUTHORIZATION_PENDING) "processing" else null,
        sriMessages = messages,
        issuedAt = now,
        authorizedAt = if (status == ElectronicDocumentStatus.AUTHORIZED) now else null,
        createdAt = now,
        updatedAt = now,
        createdBy = "usr_1",
        updatedBy = "usr_1",
    )
}

private fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}
