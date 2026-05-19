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
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ElectronicInvoiceRideAndEmailUseCaseTest {
    @Test
    fun `generates RIDE PDF from authorized XML and moves document to delivery pending`() {
        val repository = RideInMemoryElectronicInvoiceIssueRepository()
        val storage = RideInMemoryElectronicDocumentArtifactStorage()
        val auditLogger = RideRecordingElectronicInvoiceIssueAuditLogger()
        val authorizedXml = storage.putAuthorizedXml()
        val record = authorizedRecord(authorizedXml.objectKey)
        repository.create(record)

        val useCase = GenerateElectronicInvoiceRideUseCase(
            repository = repository,
            artifactStorage = storage,
            artifactReader = storage,
            auditLogger = auditLogger,
            clock = fixedClock(),
        )

        val result = useCase.execute(
            GenerateElectronicInvoiceRideCommand(
                organizationId = ORG,
                documentId = DOC,
                actorUserId = USER,
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_INVOICE_DOWNLOAD_RIDE),
            )
        )

        assertEquals(ElectronicDocumentStatus.DELIVERY_PENDING, result.record.status)
        assertEquals(ElectronicDocumentArtifactType.RIDE_PDF, result.ridePdf.artifactType)
        assertEquals("application/pdf", result.ridePdf.contentType)
        assertTrue(String(result.ridePdf.bytes).startsWith("%PDF-1.4"))
        assertNotNull(repository.findById(ORG, DOC)!!.ridePdfObjectKey)
        assertTrue(auditLogger.events.any { it.action == ElectronicInvoiceIssueAuditAction.ELECTRONIC_RIDE_GENERATED })
    }

    @Test
    fun `emails authorized XML plus RIDE and marks document as delivered`() {
        val repository = RideInMemoryElectronicInvoiceIssueRepository()
        val storage = RideInMemoryElectronicDocumentArtifactStorage()
        val emailSender = RideRecordingElectronicInvoiceEmailSender()
        val authorizedXml = storage.putAuthorizedXml()
        repository.create(authorizedRecord(authorizedXml.objectKey))

        val generateRideUseCase = GenerateElectronicInvoiceRideUseCase(
            repository = repository,
            artifactStorage = storage,
            artifactReader = storage,
            clock = fixedClock(),
        )
        val useCase = EmailElectronicInvoiceUseCase(
            repository = repository,
            artifactReader = storage,
            generateRideUseCase = generateRideUseCase,
            emailSender = emailSender,
            clock = fixedClock(),
        )

        val result = useCase.execute(
            EmailElectronicInvoiceCommand(
                organizationId = ORG,
                documentId = DOC,
                actorUserId = USER,
                actorEffectivePermissions = setOf(
                    PermissionCatalog.DOCUMENTS_INVOICE_DOWNLOAD_RIDE,
                    PermissionCatalog.DOCUMENTS_INVOICE_DOWNLOAD_XML,
                ),
                emailTo = "cliente@example.com",
            )
        )

        assertTrue(result.delivered)
        assertEquals(ElectronicDocumentStatus.DELIVERED, result.record.status)
        assertEquals("cliente@example.com", result.record.deliveryEmailTo)
        assertEquals(1, emailSender.sent.size)
        assertEquals(2, emailSender.sent.single().attachments.size)
        assertTrue(emailSender.sent.single().attachments.any { it.contentType == "application/pdf" })
        assertTrue(emailSender.sent.single().attachments.any { it.contentType.startsWith("application/xml") })
    }

    private fun RideInMemoryElectronicDocumentArtifactStorage.putAuthorizedXml(): ElectronicDocumentArtifactFile {
        val stored = put(
            StoreElectronicDocumentArtifactCommand(
                organizationId = ORG,
                documentId = DOC,
                artifactType = ElectronicDocumentArtifactType.AUTHORIZED_XML,
                content = sampleAuthorizedXml().toByteArray(Charsets.UTF_8),
                contentType = "application/xml; charset=UTF-8",
                fileName = "${DOC}_authorized.xml",
                createdAt = NOW,
            )
        )
        return get(stored.objectKey)!!
    }

    private fun authorizedRecord(authorizedXmlObjectKey: String): ElectronicInvoiceIssueRecord {
        val series = SriSeries("001", "001")
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = LocalDate.parse("2026-05-18"),
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = SriSequential(1),
                numericCode = SriNumericCode("12345678"),
            )
        )
        return ElectronicInvoiceIssueRecord(
            id = DOC,
            organizationId = ORG,
            branchId = BRANCH,
            emissionPointId = EMISSION_POINT,
            saleId = SALE,
            environment = SriEnvironment.TEST,
            documentType = SriDocumentType.INVOICE,
            series = series,
            documentNumber = "001-001-000000001",
            accessKey = accessKey,
            authorizationNumber = accessKey.value,
            status = ElectronicDocumentStatus.AUTHORIZED,
            signedXmlObjectKey = "signed.xml",
            signedXmlSha256 = "a".repeat(64),
            authorizedXmlObjectKey = authorizedXmlObjectKey,
            authorizedXmlSha256 = "b".repeat(64),
            issuedAt = NOW,
            authorizedAt = NOW,
            createdAt = NOW,
            updatedAt = NOW,
            createdBy = USER,
            updatedBy = USER,
        )
    }

    private fun fixedClock(): Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    private fun sampleAuthorizedXml(): String = """
        <autorizacion>
          <estado>AUTORIZADO</estado>
          <numeroAutorizacion>1805202601179001234500110010010000000011234567811</numeroAutorizacion>
          <fechaAutorizacion>2026-05-18T12:00:00-05:00</fechaAutorizacion>
          <comprobante><![CDATA[
            <factura id="comprobante" version="2.1.0">
              <infoTributaria>
                <ambiente>1</ambiente>
                <tipoEmision>1</tipoEmision>
                <razonSocial>ALTOS DEL MURCO CIA LTDA</razonSocial>
                <nombreComercial>Altos del Murco</nombreComercial>
                <ruc>1790012345001</ruc>
                <claveAcceso>1805202601179001234500110010010000000011234567811</claveAcceso>
                <codDoc>01</codDoc>
                <estab>001</estab>
                <ptoEmi>001</ptoEmi>
                <secuencial>000000001</secuencial>
                <dirMatriz>Tambillo</dirMatriz>
                <contribuyenteRimpe>CONTRIBUYENTE REGIMEN RIMPE</contribuyenteRimpe>
              </infoTributaria>
              <infoFactura>
                <fechaEmision>18/05/2026</fechaEmision>
                <tipoIdentificacionComprador>07</tipoIdentificacionComprador>
                <razonSocialComprador>CONSUMIDOR FINAL</razonSocialComprador>
                <identificacionComprador>9999999999999</identificacionComprador>
                <direccionComprador>Tambillo</direccionComprador>
                <totalSinImpuestos>10.00</totalSinImpuestos>
                <totalDescuento>0.00</totalDescuento>
                <totalConImpuestos>
                  <totalImpuesto>
                    <codigo>2</codigo>
                    <codigoPorcentaje>4</codigoPorcentaje>
                    <baseImponible>10.00</baseImponible>
                    <valor>1.50</valor>
                  </totalImpuesto>
                </totalConImpuestos>
                <propina>0.00</propina>
                <importeTotal>11.50</importeTotal>
                <moneda>DOLAR</moneda>
              </infoFactura>
              <detalles>
                <detalle>
                  <codigoPrincipal>PROD-1</codigoPrincipal>
                  <descripcion>Producto de prueba</descripcion>
                  <cantidad>1.00</cantidad>
                  <precioUnitario>10.00</precioUnitario>
                  <descuento>0.00</descuento>
                  <precioTotalSinImpuesto>10.00</precioTotalSinImpuesto>
                </detalle>
              </detalles>
            </factura>
          ]]></comprobante>
        </autorizacion>
    """.trimIndent()

    private class RideInMemoryElectronicInvoiceIssueRepository : ElectronicInvoiceIssueRepository {
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
    }

    private class RideInMemoryElectronicDocumentArtifactStorage : ElectronicDocumentArtifactStorage,
        ElectronicDocumentArtifactReader {
        private data class StoredFile(
            val file: ElectronicDocumentArtifactFile,
        )

        private val files = linkedMapOf<String, StoredFile>()

        override fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact {
            val sha256 = command.content.rideSha256Hex()
            val objectKey =
                "electronic/${command.organizationId}/${command.documentId}/${command.artifactType.storageValue}/${command.fileName}"
            val file = ElectronicDocumentArtifactFile(
                objectKey = objectKey,
                artifactType = command.artifactType,
                filename = command.fileName,
                contentType = command.contentType,
                bytes = command.content,
                sha256 = sha256,
                createdAt = command.createdAt,
            )
            files[objectKey] = StoredFile(file)
            return file.toStoredArtifact()
        }

        override fun get(objectKey: String): ElectronicDocumentArtifactFile? = files[objectKey]?.file

        override fun findLatest(
            organizationId: String,
            documentId: String,
            artifactType: ElectronicDocumentArtifactType,
        ): ElectronicDocumentArtifactFile? = files.values
            .map { it.file }
            .filter { file ->
                file.objectKey.startsWith("electronic/$organizationId/$documentId/${artifactType.storageValue}/")
            }
            .maxByOrNull { it.createdAt }
    }

    private class RideRecordingElectronicInvoiceEmailSender : ElectronicInvoiceEmailSender {
        val sent = mutableListOf<ElectronicInvoiceEmail>()

        override fun send(email: ElectronicInvoiceEmail): Boolean {
            sent += email
            return true
        }
    }

    private class RideRecordingElectronicInvoiceIssueAuditLogger : ElectronicInvoiceIssueAuditLogger {
        val events = mutableListOf<ElectronicInvoiceIssueAuditEvent>()

        override fun log(event: ElectronicInvoiceIssueAuditEvent) {
            events += event
        }
    }

    private companion object {
        const val ORG = "org_1"
        const val BRANCH = "br_1"
        const val EMISSION_POINT = "ep_1"
        const val SALE = "sale_1"
        const val DOC = "edoc_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T17:00:00Z")

        fun ByteArray.rideSha256Hex(): String = MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
