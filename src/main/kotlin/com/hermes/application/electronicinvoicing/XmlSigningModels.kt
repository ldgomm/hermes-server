package com.hermes.application.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.time.Instant

data class SignXmlCommand(
    val organizationId: String,
    val signatureId: String,
    val xml: ByteArray,
    val accessKey: String? = null,
    val signedAt: Instant = Instant.now(),
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to sign XML.")
        if (signatureId.isBlank()) throw DomainRuleViolation("Electronic signature id is required to sign XML.")
        if (xml.isEmpty()) throw DomainRuleViolation("XML content cannot be empty for signing.")
        accessKey?.let {
            if (!Regex("^[0-9]{49}$").matches(it)) {
                throw DomainRuleViolation("SRI access key must contain exactly 49 digits when provided.")
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignXmlCommand) return false
        return organizationId == other.organizationId &&
                signatureId == other.signatureId &&
                xml.contentEquals(other.xml) &&
                accessKey == other.accessKey &&
                signedAt == other.signedAt
    }

    override fun hashCode(): Int {
        var result = organizationId.hashCode()
        result = 31 * result + signatureId.hashCode()
        result = 31 * result + xml.contentHashCode()
        result = 31 * result + (accessKey?.hashCode() ?: 0)
        result = 31 * result + signedAt.hashCode()
        return result
    }
}

data class SignedXml(
    val signatureId: String,
    val signedXml: ByteArray,
    val signedXmlSha256: String,
    val signedAt: Instant,
    val certificateSerialNumber: String,
    val certificateFingerprintSha256: String,
    val signatureAlgorithm: String,
    val digestAlgorithm: String,
    val xadesBesObjectIncluded: Boolean,
) {
    init {
        if (signatureId.isBlank()) throw DomainRuleViolation("Signed XML signature id cannot be blank.")
        if (signedXml.isEmpty()) throw DomainRuleViolation("Signed XML cannot be empty.")
        if (!Regex("^[A-Fa-f0-9]{64}$").matches(signedXmlSha256)) {
            throw DomainRuleViolation("Signed XML SHA-256 hash is invalid.")
        }
        if (certificateSerialNumber.isBlank()) throw DomainRuleViolation("Certificate serial number cannot be blank.")
        if (!Regex("^[A-Fa-f0-9]{64}$").matches(certificateFingerprintSha256)) {
            throw DomainRuleViolation("Certificate SHA-256 fingerprint is invalid.")
        }
        if (signatureAlgorithm.isBlank()) throw DomainRuleViolation("Signature algorithm cannot be blank.")
        if (digestAlgorithm.isBlank()) throw DomainRuleViolation("Digest algorithm cannot be blank.")
    }

    val signedXmlText: String get() = signedXml.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedXml) return false
        return signatureId == other.signatureId &&
                signedXml.contentEquals(other.signedXml) &&
                signedXmlSha256 == other.signedXmlSha256 &&
                signedAt == other.signedAt &&
                certificateSerialNumber == other.certificateSerialNumber &&
                certificateFingerprintSha256 == other.certificateFingerprintSha256 &&
                signatureAlgorithm == other.signatureAlgorithm &&
                digestAlgorithm == other.digestAlgorithm &&
                xadesBesObjectIncluded == other.xadesBesObjectIncluded
    }

    override fun hashCode(): Int {
        var result = signatureId.hashCode()
        result = 31 * result + signedXml.contentHashCode()
        result = 31 * result + signedXmlSha256.hashCode()
        result = 31 * result + signedAt.hashCode()
        result = 31 * result + certificateSerialNumber.hashCode()
        result = 31 * result + certificateFingerprintSha256.hashCode()
        result = 31 * result + signatureAlgorithm.hashCode()
        result = 31 * result + digestAlgorithm.hashCode()
        result = 31 * result + xadesBesObjectIncluded.hashCode()
        return result
    }
}

data class XmlSigningKeyMaterial(
    val privateKey: PrivateKey,
    val certificate: X509Certificate,
    val certificateChain: List<X509Certificate>,
    val alias: String,
    val certificateFingerprintSha256: String,
) {
    init {
        if (certificateChain.isEmpty()) throw DomainRuleViolation("PKCS#12 certificate chain cannot be empty.")
        if (alias.isBlank()) throw DomainRuleViolation("PKCS#12 certificate alias cannot be blank.")
        if (!Regex("^[A-Fa-f0-9]{64}$").matches(certificateFingerprintSha256)) {
            throw DomainRuleViolation("Certificate SHA-256 fingerprint is invalid.")
        }
    }
}

interface XmlSigner {
    fun sign(command: SignXmlCommand, keyMaterial: XmlSigningKeyMaterial): SignedXml
}

interface XmlSigningKeyMaterialLoader {
    fun loadPkcs12(content: ByteArray, password: CharArray): XmlSigningKeyMaterial
}
