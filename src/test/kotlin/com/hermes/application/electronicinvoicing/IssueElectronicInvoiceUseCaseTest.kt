package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.*
import com.hermes.infrastructure.xml.SriInvoiceXmlBuilder
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.*

class IssueElectronicInvoiceUseCaseTest {
    private val now = Instant.parse("2026-05-18T10:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `issues electronic invoice through authorized full flow`() {
        val fixture = issueFixture()

        val result = fixture.issueUseCase.execute(issueCommand())

        assertTrue(result.authorized)
        assertEquals(ElectronicDocumentStatus.AUTHORIZED, result.record.status)
        assertEquals("001-002-000000001", result.record.documentNumber)
        assertEquals(49, result.record.accessKey.value.length)
        assertNotNull(result.signedXml)
        assertEquals(result.record.accessKey.value, result.authorization?.authorizationNumber)
        assertTrue(result.artifacts.any { it.artifactType == ElectronicDocumentArtifactType.UNSIGNED_XML })
        assertTrue(result.artifacts.any { it.artifactType == ElectronicDocumentArtifactType.SIGNED_XML })
        assertTrue(result.artifacts.any { it.artifactType == ElectronicDocumentArtifactType.AUTHORIZED_XML })
        assertEquals(result.record, fixture.issueRepository.findById("org_1", result.record.id))
    }

    @Test
    fun `stops before signature and SRI when XSD validation fails`() {
        val fixture = issueFixture(xsdValidator = FakeXsdValidator(valid = false))

        val result = fixture.issueUseCase.execute(issueCommand())

        assertEquals(ElectronicDocumentStatus.XSD_INVALID, result.record.status)
        assertFalse(result.authorized)
        assertTrue(result.stoppedBeforeSri)
        assertNull(result.signedXml)
        assertNull(result.reception)
        assertNull(result.authorization)
        assertTrue(result.record.sriMessages.isNotEmpty())
    }

    @Test
    fun `stops after reception when SRI returns document`() {
        val fixture = issueFixture(
            receptionClient = FakeReceptionClient(SriReceptionStatus.RETURNED),
        )

        val result = fixture.issueUseCase.execute(issueCommand())

        assertEquals(ElectronicDocumentStatus.RETURNED_BY_SRI, result.record.status)
        assertEquals(SriReceptionStatus.RETURNED, result.reception?.status)
        assertNull(result.authorization)
        assertTrue(result.record.lastErrorClassification?.userActionRequired == true)
    }

    @Test
    fun `keeps authorization pending and retries query only`() {
        val authorizationClient = FakeAuthorizationClient(SriAuthorizationStatus.PROCESSING)
        val fixture = issueFixture(authorizationClient = authorizationClient)

        val first = fixture.issueUseCase.execute(issueCommand())
        assertEquals(ElectronicDocumentStatus.AUTHORIZATION_PENDING, first.record.status)
        assertEquals(SriAuthorizationStatus.PROCESSING, first.authorization?.status)

        authorizationClient.status = SriAuthorizationStatus.AUTHORIZED
        val retry = RetrySriAuthorizationUseCase(fixture.queryUseCase).execute(
            QuerySriAuthorizationCommand(
                record = first.record,
                actorUserId = "usr_1",
            )
        )

        assertEquals(ElectronicDocumentStatus.AUTHORIZED, retry.record.status)
        assertEquals(SriAuthorizationStatus.AUTHORIZED, retry.authorization.status)
        assertEquals(first.record.accessKey.value, retry.authorization.authorizationNumber)
    }

    @Test
    fun `rejects duplicate authorized invoice for same sale`() {
        val fixture = issueFixture()
        val first = fixture.issueUseCase.execute(issueCommand())
        assertEquals(ElectronicDocumentStatus.AUTHORIZED, first.record.status)

        val error = kotlin.runCatching { fixture.issueUseCase.execute(issueCommand()) }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error.message!!.contains("authorized electronic invoice"))
    }

    private fun issueFixture(
        xsdValidator: SriXsdValidator = FakeXsdValidator(valid = true),
        receptionClient: SriReceptionClient = FakeReceptionClient(SriReceptionStatus.RECEIVED),
        authorizationClient: FakeAuthorizationClient = FakeAuthorizationClient(SriAuthorizationStatus.AUTHORIZED),
    ): IssueFixture {
        val sequenceRepository = InMemoryIssueElectronicSequenceRepository()
        EnsureElectronicSequenceUseCase(sequenceRepository).execute(
            EnsureElectronicSequenceCommand(
                id = "eseq_1",
                organizationId = "org_1",
                environment = SriEnvironment.TEST,
                documentType = SriDocumentType.INVOICE,
                series = SriSeries("001", "002"),
                now = now,
            )
        )

        val issueRepository = InMemoryElectronicInvoiceIssueRepository()
        val artifactStorage = InMemoryElectronicDocumentArtifactStorage()
        val auditLogger = RecordingElectronicInvoiceIssueAuditLogger()
        val submitUseCase = SubmitElectronicDocumentToSriUseCase(
            repository = issueRepository,
            artifactStorage = artifactStorage,
            receptionClient = receptionClient,
            auditLogger = auditLogger,
            clock = clock,
        )
        val queryUseCase = QuerySriAuthorizationUseCase(
            repository = issueRepository,
            artifactStorage = artifactStorage,
            authorizationClient = authorizationClient,
            auditLogger = auditLogger,
            clock = clock,
        )
        val issueUseCase = IssueElectronicInvoiceUseCase(
            idGenerator = ElectronicInvoiceIssueIdGenerator { prefix -> "${prefix}_1" },
            accessKeyUseCase = ReserveSriAccessKeyUseCase(sequenceRepository),
            xmlCommandFactory = MinimalInvoiceXmlCommandFactory(),
            xmlBuilder = SriInvoiceXmlBuilder(),
            xsdValidator = xsdValidator,
            signingService = FakeSigningService(),
            submitToSriUseCase = submitUseCase,
            queryAuthorizationUseCase = queryUseCase,
            repository = issueRepository,
            artifactStorage = artifactStorage,
            auditLogger = auditLogger,
            clock = clock,
        )
        return IssueFixture(issueUseCase, queryUseCase, issueRepository, artifactStorage, auditLogger)
    }

    private fun issueCommand(): IssueElectronicInvoiceCommand = IssueElectronicInvoiceCommand(
        organizationId = "org_1",
        actorUserId = "usr_1",
        saleId = "sale_1",
        branchId = "br_1",
        emissionPointId = "emi_1",
        environment = SriEnvironment.TEST,
        issuerRuc = "1790012345001",
        series = SriSeries("001", "002"),
        issuedDate = LocalDate.of(2026, 5, 18),
        numericCode = SriNumericCode("12345678"),
        issuedAt = now,
    )

    private data class IssueFixture(
        val issueUseCase: IssueElectronicInvoiceUseCase,
        val queryUseCase: QuerySriAuthorizationUseCase,
        val issueRepository: InMemoryElectronicInvoiceIssueRepository,
        val artifactStorage: InMemoryElectronicDocumentArtifactStorage,
        val auditLogger: RecordingElectronicInvoiceIssueAuditLogger,
    )
}

private class MinimalInvoiceXmlCommandFactory : ElectronicInvoiceXmlCommandFactory {
    override fun build(command: PrepareElectronicInvoiceXmlCommand): BuildSriInvoiceXmlCommand =
        SriInvoiceXmlFactories.minimalInvoice(
            issuedDate = command.issueCommand.issuedDate,
            environment = command.issueCommand.environment,
            series = command.issueCommand.series,
            sequential = command.sequentialReservation.sequential,
            accessKey = command.accessKey,
            issuerRuc = command.issueCommand.issuerRuc,
            issuerLegalName = "Hermes Test S.A.",
            issuerMatrixAddress = "Tambillo",
            buyerIdentificationType = SriIdentificationType.FINAL_CONSUMER,
            buyerIdentification = SriIdentificationType.FINAL_CONSUMER_IDENTIFICATION,
            buyerLegalName = "CONSUMIDOR FINAL",
            subtotal = BigDecimal("10.00"),
            taxRate = BigDecimal("15.00"),
            taxRateCode = "4",
            taxAmount = BigDecimal("1.50"),
            total = BigDecimal("11.50"),
            itemCode = "ITEM-1",
            itemDescription = "Producto de prueba",
            email = "cliente@example.com",
        )
}

private class FakeXsdValidator(private val valid: Boolean) : SriXsdValidator {
    override fun validate(xml: ByteArray, schemaVersionCode: String): XsdValidationResult =
        if (valid) {
            XsdValidationResult.valid(schemaVersionCode)
        } else {
            XsdValidationResult.invalid(
                schemaVersionCode,
                listOf(XsdValidationError(XsdValidationSeverity.ERROR, "Missing required field")),
            )
        }
}

private class FakeSigningService : ElectronicDocumentSigningService {
    override fun sign(command: SignElectronicDocumentCommand): SignElectronicDocumentResult {
        val signedBytes = command.unsignedXml.toString(Charsets.UTF_8)
            .replace("</factura>", "<ds:Signature>fake</ds:Signature></factura>")
            .toByteArray(Charsets.UTF_8)
        val signed = SignedXml(
            signatureId = command.signatureId ?: "sig_active",
            signedXml = signedBytes,
            signedXmlSha256 = signedBytes.sha256Hex(),
            signedAt = Instant.parse("2026-05-18T10:00:00Z"),
            certificateSerialNumber = "123456789",
            certificateFingerprintSha256 = "a".repeat(64),
            signatureAlgorithm = "RSA-SHA1",
            digestAlgorithm = "SHA1",
            xadesBesObjectIncluded = true,
        )
        return SignElectronicDocumentResult(signed, signed.signatureId, signed.signedAt)
    }
}

private class FakeReceptionClient(private val status: SriReceptionStatus) : SriReceptionClient {
    override fun submit(command: SriReceptionCommand): SriReceptionResult = SriReceptionResult(
        environment = command.environment,
        status = status,
        accessKey = command.accessKey,
        messages = if (status == SriReceptionStatus.RETURNED) listOf(
            SriMessage.error(
                "35",
                "DOCUMENTO INVALIDO"
            )
        ) else emptyList(),
        rawRequestXml = "<soap:Envelope><validarComprobante/></soap:Envelope>",
        rawResponseXml = "<RespuestaRecepcionComprobante><estado>${status.sriValue}</estado></RespuestaRecepcionComprobante>",
        receivedAt = command.requestedAt,
    )
}

private class FakeAuthorizationClient(var status: SriAuthorizationStatus) : SriAuthorizationClient {
    override fun query(command: SriAuthorizationQueryCommand): SriAuthorizationResult = SriAuthorizationResult(
        environment = command.environment,
        status = status,
        accessKey = command.accessKey,
        authorizationNumber = if (status == SriAuthorizationStatus.AUTHORIZED) command.accessKey.value else null,
        authorizedAt = if (status == SriAuthorizationStatus.AUTHORIZED) command.requestedAt else null,
        authorizedXml = if (status == SriAuthorizationStatus.AUTHORIZED) "<autorizacion><estado>AUTORIZADO</estado></autorizacion>" else null,
        messages = if (status == SriAuthorizationStatus.NOT_AUTHORIZED) listOf(
            SriMessage.error(
                "70",
                "CLAVE DE ACCESO EN PROCESAMIENTO"
            )
        ) else emptyList(),
        rawRequestXml = "<soap:Envelope><autorizacionComprobante/></soap:Envelope>",
        rawResponseXml = "<RespuestaAutorizacionComprobante><estado>${status.sriValue}</estado></RespuestaAutorizacionComprobante>",
        queriedAt = command.requestedAt,
    )
}

//Redeclaration: class InMemoryElectronicSequenceRepository : ElectronicSequenceRepository
private class InMemoryIssueElectronicSequenceRepository : ElectronicSequenceRepository {
    private val sequences = linkedMapOf<ElectronicSequenceKey, ElectronicSequence>()

    override fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence = synchronized(this) {
        sequences.getOrPut(sequence.key) { sequence }
    }

    override fun findByKey(key: ElectronicSequenceKey): ElectronicSequence? = synchronized(this) {
        sequences[key]
    }

    override fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation =
        synchronized(this) {
            val key =
                ElectronicSequenceKey(command.organizationId, command.environment, command.documentType, command.series)
            val current = sequences[key] ?: error("Sequence does not exist: ${key.storageKey}")
            val next = current.nextSequential()
            val updated = current.markIssued(next, command.documentId, command.issuedAt)
            sequences[key] = updated
            ElectronicSequenceReservation(sequence = updated, sequential = next)
        }
}

//Redeclaration: class InMemoryElectronicInvoiceIssueRepository : ElectronicInvoiceIssueRepository
private class InMemoryElectronicInvoiceIssueRepository : ElectronicInvoiceIssueRepository {
    private val records = linkedMapOf<String, ElectronicInvoiceIssueRecord>()

    override fun create(record: ElectronicInvoiceIssueRecord) {
        records[record.id] = record
    }

    override fun update(record: ElectronicInvoiceIssueRecord) {
        records[record.id] = record
    }

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        records[documentId]?.takeIf { it.organizationId == organizationId }

    override fun existsAuthorizedInvoiceForSale(organizationId: String, saleId: String): Boolean =
        records.values.any { it.organizationId == organizationId && it.saleId == saleId && it.status == ElectronicDocumentStatus.AUTHORIZED }
}

//Redeclaration: class InMemoryElectronicDocumentArtifactStorage : ElectronicDocumentArtifactStorage, ElectronicDocumentArtifactReader
private class InMemoryElectronicDocumentArtifactStorage : ElectronicDocumentArtifactStorage {
    val artifacts = mutableListOf<StoredElectronicDocumentArtifact>()

    override fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact {
        val artifact = StoredElectronicDocumentArtifact(
            objectKey = "electronic/${command.organizationId}/${command.documentId}/${command.fileName}",
            artifactType = command.artifactType,
            sha256 = command.content.sha256Hex(),
            sizeBytes = command.content.size.toLong(),
            createdAt = command.createdAt,
        )
        artifacts += artifact
        return artifact
    }
}

private class RecordingElectronicInvoiceIssueAuditLogger : ElectronicInvoiceIssueAuditLogger {
    val events = mutableListOf<ElectronicInvoiceIssueAuditEvent>()
    override fun log(event: ElectronicInvoiceIssueAuditEvent) {
        events += event
    }
}

private val SriReceptionStatus.sriValue: String
    get() = when (this) {
        SriReceptionStatus.RECEIVED -> "RECIBIDA"
        SriReceptionStatus.RETURNED -> "DEVUELTA"
    }

private val SriAuthorizationStatus.sriValue: String
    get() = when (this) {
        SriAuthorizationStatus.AUTHORIZED -> "AUTORIZADO"
        SriAuthorizationStatus.NOT_AUTHORIZED -> "RECHAZADO"
        SriAuthorizationStatus.PROCESSING -> "PPR"
    }

private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
