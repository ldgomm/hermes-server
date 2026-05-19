package com.hermes.application.signature

import com.hermes.application.electronicinvoicing.ElectronicSignatureSecretReader
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.signature.ElectronicSignature
import com.hermes.domain.signature.SignatureCertificateMetadata
import com.hermes.domain.signature.SignatureCertificateType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectronicSignatureAdminUseCasesTest {
    @Test
    fun `uploads validates activates and revokes electronic signature`() {
        val repository = InMemoryAdminSignatureRepository()
        val secrets = InMemorySignatureSecrets()
        val inspector = StaticSignatureCertificateInspector()
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val idGenerator = ElectronicSignatureIdGenerator { "sig_1" }

        val upload = UploadElectronicSignatureUseCase(repository, secrets, inspector, idGenerator, clock).execute(
            UploadElectronicSignatureCommand(
                organizationId = ORG,
                actorUserId = USER,
                actorEffectivePermissions = PERMISSIONS,
                fileName = "firma.p12",
                content = byteArrayOf(1, 2, 3),
                password = "secret".toCharArray(),
            )
        )

        assertEquals("sig_1", upload.signature.id)
        assertEquals("UPLOADED", upload.signature.status.name)

        val validated = ValidateElectronicSignatureUseCase(repository, secrets, inspector, clock).execute(
            ValidateElectronicSignatureCommand(ORG, USER, PERMISSIONS, "sig_1")
        )
        assertEquals("VALID", validated.signature.status.name)

        val activated = ActivateElectronicSignatureUseCase(repository, clock).execute(
            ActivateElectronicSignatureCommand(ORG, USER, PERMISSIONS, "sig_1")
        )
        assertTrue(activated.validation.usable)

        val revoked = RevokeElectronicSignatureUseCase(repository, clock).execute(
            RevokeElectronicSignatureCommand(ORG, USER, PERMISSIONS, "sig_1")
        )
        assertEquals("REVOKED", revoked.signature.status.name)
    }

    private companion object {
        const val ORG = "org_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
        val PERMISSIONS = setOf(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
    }
}

private class InMemoryAdminSignatureRepository : ElectronicSignatureRepository {
    private val values = mutableMapOf<String, ElectronicSignature>()
    override fun create(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun update(signature: ElectronicSignature) {
        values[signature.id] = signature
    }

    override fun findById(id: String): ElectronicSignature? = values[id]
    override fun findActiveByOrganizationId(organizationId: String): ElectronicSignature? =
        values.values.firstOrNull { it.organizationId == organizationId && it.status.name == "VALID" }

    override fun findByOrganizationId(organizationId: String): List<ElectronicSignature> =
        values.values.filter { it.organizationId == organizationId }
}

private class InMemorySignatureSecrets : SignatureSecretStore, ElectronicSignatureSecretReader {
    private val files = mutableMapOf<String, ByteArray>()
    private val passwords = mutableMapOf<String, CharArray>()

    override fun storeEncryptedSignature(
        organizationId: String,
        signatureId: String,
        fileName: String,
        content: ByteArray,
    ): String {
        val key = "$organizationId/$signatureId/$fileName"
        files[key] = content
        return key
    }

    override fun storePasswordSecret(organizationId: String, signatureId: String, password: CharArray): String {
        val key = "$organizationId/$signatureId/password"
        passwords[key] = password.copyOf()
        return key
    }

    override fun readSignatureContent(encryptedFileObjectKey: String): ByteArray =
        files.getValue(encryptedFileObjectKey)

    override fun readPassword(encryptedPasswordRef: String): CharArray =
        passwords.getValue(encryptedPasswordRef).copyOf()
}

private class StaticSignatureCertificateInspector : SignatureCertificateInspector {
    override fun inspectPkcs12(
        content: ByteArray,
        password: CharArray,
        fileName: String
    ): SignatureCertificateMetadata =
        SignatureCertificateMetadata(
            certificateType = SignatureCertificateType.P12,
            subject = "CN=Hermes Demo",
            issuer = "CN=CA",
            serialNumber = "ABC123",
            validFrom = Instant.parse("2026-05-18T11:00:00Z"),
            validTo = Instant.parse("2026-08-18T12:00:00Z"),
            sha256Fingerprint = "A".repeat(64),
        )
}
