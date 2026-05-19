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
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate

class ElectronicInvoiceDeliveryUseCasesTest {
    @Test
    fun `downloads authorized XML without exposing storage key through response models`() {
        val repository = InMemory12DIssueRepository()
        val storage = InMemory12DArtifactStorage()
        val record = deliveryRecord(ElectronicDocumentStatus.AUTHORIZED)
        repository.create(record)
        storage.seed(record.authorizedXmlObjectKey!!, ElectronicDocumentArtifactType.AUTHORIZED_XML, "authorized.xml", "<autorizacion/>".toByteArray())

        val useCase = DownloadElectronicInvoiceArtifactUseCase(repository, storage)
        val result = useCase.execute(
            DownloadElectronicInvoiceArtifactCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_XML),
                artifactKind = ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML,
            )
        )

        assertEquals(ElectronicDocumentArtifactType.AUTHORIZED_XML, result.artifact.artifactType)
        assertEquals("authorized.xml", result.artifact.filename)
    }

    @Test
    fun `download artifact rejects missing permission`() {
        val repository = InMemory12DIssueRepository()
        val storage = InMemory12DArtifactStorage()
        val record = deliveryRecord(ElectronicDocumentStatus.AUTHORIZED)
        repository.create(record)

        val useCase = DownloadElectronicInvoiceArtifactUseCase(repository, storage)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                DownloadElectronicInvoiceArtifactCommand(
                    organizationId = record.organizationId,
                    documentId = record.id,
                    actorUserId = "usr_1",
                    actorEffectivePermissions = emptySet(),
                    artifactKind = ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML,
                )
            )
        }
    }

    @Test
    fun `email permission can generate RIDE and deliver invoice`() {
        val repository = InMemory12DIssueRepository()
        val storage = InMemory12DArtifactStorage()
        val record = deliveryRecord(ElectronicDocumentStatus.AUTHORIZED)
        repository.create(record)
        storage.seed(record.authorizedXmlObjectKey!!, ElectronicDocumentArtifactType.AUTHORIZED_XML, "authorized.xml", "<autorizacion/>".toByteArray())

        val rideUseCase = GenerateElectronicInvoiceRideUseCase(
            repository = repository,
            artifactStorage = storage,
            artifactReader = storage,
            rideRenderer = Fake12DRideRenderer(),
        )
        val emailSender = Fake12DEmailSender(delivered = true)
        val emailUseCase = EmailElectronicInvoiceUseCase(
            repository = repository,
            artifactReader = storage,
            generateRideUseCase = rideUseCase,
            emailSender = emailSender,
        )

        val result = emailUseCase.execute(
            EmailElectronicInvoiceCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_EMAIL),
                emailTo = "cliente@example.com",
            )
        )

        assertTrue(result.delivered)
        assertEquals(ElectronicDocumentStatus.DELIVERED, result.record.status)
        assertEquals(1, emailSender.sent)
        assertEquals(2, emailSender.lastEmail!!.attachments.size)
    }

    @Test
    fun `timeline requires audit permission and returns events`() {
        val repository = InMemory12DIssueRepository()
        val record = deliveryRecord(ElectronicDocumentStatus.AUTHORIZED)
        repository.create(record)
        val timelineRepository = InMemory12DTimelineRepository(
            listOf(
                ElectronicInvoiceTimelineEvent(
                    id = "audit_1",
                    organizationId = record.organizationId,
                    documentId = record.id,
                    action = "SRI_AUTHORIZED",
                    actorUserId = "usr_1",
                    saleId = record.saleId,
                    accessKey = record.accessKey.value,
                    status = record.status.name,
                    message = "Authorized",
                    occurredAt = record.authorizedAt!!,
                )
            )
        )
        val useCase = GetElectronicInvoiceTimelineUseCase(repository, timelineRepository)

        val result = useCase.execute(
            GetElectronicInvoiceTimelineCommand(
                organizationId = record.organizationId,
                documentId = record.id,
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_AUDIT),
            )
        )

        assertEquals("SRI_AUTHORIZED", result.events.single().action)
    }
}

private class InMemory12DIssueRepository : ElectronicInvoiceIssueRepository, ElectronicInvoiceIssueQueryRepository {
    private val records = linkedMapOf<String, ElectronicInvoiceIssueRecord>()

    override fun create(record: ElectronicInvoiceIssueRecord) {
        records[key(record.organizationId, record.id)] = record
    }

    override fun update(record: ElectronicInvoiceIssueRecord) {
        records[key(record.organizationId, record.id)] = record
    }

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        records[key(organizationId, documentId)]

    override fun search(query: ElectronicInvoiceIssueSearchQuery): List<ElectronicInvoiceIssueRecord> =
        records.values.filter { it.organizationId == query.organizationId }.take(query.limit)

    override fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean = false

    private fun key(organizationId: String, documentId: String): String = "$organizationId::$documentId"
}

private class InMemory12DArtifactStorage : ElectronicDocumentArtifactStorage, ElectronicDocumentArtifactReader {
    private val files = linkedMapOf<String, ElectronicDocumentArtifactFile>()

    fun seed(objectKey: String, type: ElectronicDocumentArtifactType, filename: String, bytes: ByteArray) {
        files[objectKey] = ElectronicDocumentArtifactFile(
            objectKey = objectKey,
            artifactType = type,
            filename = filename,
            contentType = if (type == ElectronicDocumentArtifactType.RIDE_PDF) "application/pdf" else "application/xml",
            bytes = bytes,
            sha256 = sha256(bytes),
            createdAt = Instant.parse("2026-05-18T10:00:00Z"),
        )
    }

    override fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact {
        val objectKey = "${command.documentId}/${command.artifactType.storageValue}/${files.size + 1}_${command.fileName}"
        seed(objectKey, command.artifactType, command.fileName, command.content)
        return files.getValue(objectKey).toStoredArtifact()
    }

    override fun get(objectKey: String): ElectronicDocumentArtifactFile? = files[objectKey]

    override fun findLatest(
        organizationId: String,
        documentId: String,
        artifactType: ElectronicDocumentArtifactType,
    ): ElectronicDocumentArtifactFile? = files.values.lastOrNull { file ->
        file.objectKey.startsWith(documentId) && file.artifactType == artifactType
    }
}

private class Fake12DRideRenderer : ElectronicInvoiceRideRenderer {
    override fun render(command: ElectronicInvoiceRideRenderCommand): ElectronicInvoiceGeneratedFile =
        ElectronicInvoiceGeneratedFile(
            filename = "${command.record.documentNumber}.pdf",
            contentType = "application/pdf",
            bytes = "%PDF-1.4 fake ride".toByteArray(),
        )
}

private class Fake12DEmailSender(private val delivered: Boolean) : ElectronicInvoiceEmailSender {
    var sent: Int = 0
        private set
    var lastEmail: ElectronicInvoiceEmail? = null
        private set

    override fun send(email: ElectronicInvoiceEmail): Boolean {
        sent++
        lastEmail = email
        return delivered
    }
}

private class InMemory12DTimelineRepository(
    private val events: List<ElectronicInvoiceTimelineEvent>,
) : ElectronicInvoiceTimelineRepository {
    override fun list(query: ElectronicInvoiceTimelineQuery): List<ElectronicInvoiceTimelineEvent> =
        events.filter { it.organizationId == query.organizationId && it.documentId == query.documentId }.take(query.limit)
}

private fun deliveryRecord(status: ElectronicDocumentStatus): ElectronicInvoiceIssueRecord {
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
        authorizedXmlObjectKey = "doc_1/authorized.xml",
        authorizedXmlSha256 = "b".repeat(64),
        lastSriAuthorizationStatus = if (status == ElectronicDocumentStatus.AUTHORIZED) "authorized" else null,
        issuedAt = now,
        authorizedAt = if (status == ElectronicDocumentStatus.AUTHORIZED) now else null,
        createdAt = now,
        updatedAt = now,
        createdBy = "usr_1",
        updatedBy = "usr_1",
    )
}

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
