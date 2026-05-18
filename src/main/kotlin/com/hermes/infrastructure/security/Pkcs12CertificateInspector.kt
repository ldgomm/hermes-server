package com.hermes.infrastructure.security

import com.hermes.application.signature.SignatureCertificateInspector
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.signature.SignatureCertificateMetadata
import com.hermes.domain.signature.SignatureCertificateRules
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.time.ZoneOffset
import java.util.Enumeration

class Pkcs12CertificateInspector : SignatureCertificateInspector {
    override fun inspectPkcs12(
        content: ByteArray,
        password: CharArray,
        fileName: String,
    ): SignatureCertificateMetadata {
        SignatureCertificateRules.assertFileNameAllowed(fileName)
        if (content.isEmpty()) throw DomainRuleViolation("Signature file content cannot be empty.")
        if (password.isEmpty()) throw DomainRuleViolation("Signature password cannot be empty.")

        val keyStore = KeyStore.getInstance("PKCS12")
        ByteArrayInputStream(content).use { input ->
            keyStore.load(input, password)
        }

        val aliases = keyStore.aliases().toList()
        val alias = aliases.firstOrNull { keyStore.getCertificate(it) is X509Certificate }
            ?: throw DomainRuleViolation("PKCS#12 file does not contain an X509 certificate.")

        val certificate = keyStore.getCertificate(alias) as X509Certificate
        certificate.checkValidity()

        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(separator = "") { byte -> "%02X".format(byte) }

        return SignatureCertificateMetadata(
            certificateType = SignatureCertificateRules.inferCertificateType(fileName),
            subject = certificate.subjectX500Principal.name,
            issuer = certificate.issuerX500Principal.name,
            serialNumber = certificate.serialNumber.toString(16).uppercase(),
            validFrom = certificate.notBefore.toInstant().atOffset(ZoneOffset.UTC).toInstant(),
            validTo = certificate.notAfter.toInstant().atOffset(ZoneOffset.UTC).toInstant(),
            sha256Fingerprint = fingerprint,
        )
    }
}

private fun <T> Enumeration<T>.toList(): List<T> {
    val result = mutableListOf<T>()
    while (hasMoreElements()) result += nextElement()
    return result
}
