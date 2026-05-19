package com.hermes.application.electronicinvoicing

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.signature.ElectronicSignature
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Principal
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SignElectronicDocumentUseCaseTest {
    private val now = Instant.parse("2026-05-18T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `loads active signature signs xml marks signature as used and audits usage`() {
        val repository = InMemorySignatureRepository(signature())
        val vault = FakeSignatureVault(signature())
        val audit = RecordingSignatureAuditLogger()
        val useCase = SignElectronicDocumentUseCase(
            signatureVault = vault,
            signatureRepository = repository,
            xmlSigner = FakeXmlSigner(),
            auditLogger = audit,
            clock = clock,
        )

        val result = useCase.execute(
            SignElectronicDocumentCommand(
                organizationId = "org_1",
                documentId = "doc_1",
                unsignedXml = "<factura id=\"comprobante\"/>".toByteArray(),
                accessKey = "1234567890123456789012345678901234567890123456789",
            )
        )

        assertEquals("sig_1", result.signatureId)
        assertEquals(now, repository.updated!!.lastUsedAt)
        assertEquals(ElectronicSignatureUsageAuditAction.ELECTRONIC_SIGNATURE_USED, audit.events.single().action)
        assertNotNull(result.signedXml)
    }

    private fun signature(): ElectronicSignature = ElectronicSignature.upload(
        id = "sig_1",
        organizationId = "org_1",
        storageKey = "encrypted/signatures/org_1/sig_1.p12",
        passwordSecretRef = "secret_ref_1",
        subject = "HERMES TEST",
        issuer = "TEST ISSUER",
        validFrom = Instant.parse("2026-01-01T00:00:00Z"),
        validTo = Instant.parse("2028-01-01T00:00:00Z"),
        uploadedBy = "usr_1",
        uploadedAt = Instant.parse("2026-01-01T00:00:00Z"),
    ).markValidated(now)
}

private class InMemorySignatureRepository(
    private val signature: ElectronicSignature,
) : ElectronicSignatureRepository {
    var updated: ElectronicSignature? = null
    override fun create(signature: ElectronicSignature) = Unit
    override fun update(signature: ElectronicSignature) {
        updated = signature
    }

    override fun findById(id: String): ElectronicSignature? = if (id == signature.id) signature else null
    override fun findActiveByOrganizationId(organizationId: String): ElectronicSignature? =
        if (organizationId == signature.organizationId) signature else null

    override fun findByOrganizationId(organizationId: String): List<ElectronicSignature> =
        if (organizationId == signature.organizationId) listOf(signature) else emptyList()
}

private class FakeSignatureVault(
    private val signature: ElectronicSignature,
) : ElectronicSignatureVault {
    private val material = testMaterial()
    override fun loadActiveForSigning(organizationId: String, now: Instant): ElectronicSignatureSigningMaterial =
        ElectronicSignatureSigningMaterial(signature, material)

    override fun loadForSigning(
        organizationId: String,
        signatureId: String,
        now: Instant,
    ): ElectronicSignatureSigningMaterial = ElectronicSignatureSigningMaterial(signature, material)
}

private class FakeXmlSigner : XmlSigner {
    override fun sign(command: SignXmlCommand, keyMaterial: XmlSigningKeyMaterial): SignedXml = SignedXml(
        signatureId = command.signatureId,
        signedXml = "<signed/>".toByteArray(),
        signedXmlSha256 = "a".repeat(64),
        signedAt = command.signedAt,
        certificateSerialNumber = "1",
        certificateFingerprintSha256 = "b".repeat(64),
        signatureAlgorithm = "rsa-sha1",
        digestAlgorithm = "sha1",
        xadesBesObjectIncluded = true,
    )
}

private class RecordingSignatureAuditLogger : ElectronicSignatureUsageAuditLogger {
    val events = mutableListOf<ElectronicSignatureUsageAuditEvent>()
    override fun log(event: ElectronicSignatureUsageAuditEvent) {
        events += event
    }
}

private fun testMaterial(): XmlSigningKeyMaterial {
    val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }.generateKeyPair()
    val certificate = FakeX509Certificate(keyPair.public)
    return XmlSigningKeyMaterial(
        privateKey = keyPair.private,
        certificate = certificate,
        certificateChain = listOf(certificate),
        alias = "fake",
        certificateFingerprintSha256 = "c".repeat(64),
    )
}

private class FakeX509Certificate(private val publicKey: PublicKey) : X509Certificate() {
    override fun getEncoded(): ByteArray = "fake-certificate".toByteArray()
    override fun verify(key: PublicKey?) = Unit
    override fun verify(key: PublicKey?, sigProvider: String?) = Unit
    override fun toString(): String = "FakeX509Certificate"
    override fun getPublicKey(): PublicKey = publicKey
    override fun checkValidity() = Unit
    override fun checkValidity(date: Date?) = Unit
    override fun getVersion(): Int = 3
    override fun getSerialNumber(): BigInteger = BigInteger.ONE
    override fun getIssuerDN(): Principal = Principal { "CN=Issuer" }
    override fun getSubjectDN(): Principal = Principal { "CN=Subject" }
    override fun getNotBefore(): Date = Date.from(Instant.parse("2026-01-01T00:00:00Z"))
    override fun getNotAfter(): Date = Date.from(Instant.parse("2028-01-01T00:00:00Z"))
    override fun getTBSCertificate(): ByteArray = ByteArray(0)
    override fun getSignature(): ByteArray = ByteArray(0)
    override fun getSigAlgName(): String = "SHA256withRSA"
    override fun getSigAlgOID(): String = "1.2.840.113549.1.1.11"
    override fun getSigAlgParams(): ByteArray? = null
    override fun getIssuerUniqueID(): BooleanArray? = null
    override fun getSubjectUniqueID(): BooleanArray? = null
    override fun getKeyUsage(): BooleanArray? = null
    override fun getBasicConstraints(): Int = -1
    override fun hasUnsupportedCriticalExtension(): Boolean = false
    override fun getCriticalExtensionOIDs(): MutableSet<String>? = null
    override fun getNonCriticalExtensionOIDs(): MutableSet<String>? = null
    override fun getExtensionValue(oid: String?): ByteArray? = null
}
