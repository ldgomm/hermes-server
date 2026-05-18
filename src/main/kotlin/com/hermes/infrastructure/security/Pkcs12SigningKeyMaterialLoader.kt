package com.hermes.infrastructure.security

import com.hermes.application.electronicinvoicing.XmlSigningKeyMaterial
import com.hermes.application.electronicinvoicing.XmlSigningKeyMaterialLoader
import com.hermes.domain.shared.DomainRuleViolation
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate

class Pkcs12SigningKeyMaterialLoader : XmlSigningKeyMaterialLoader {
    override fun loadPkcs12(content: ByteArray, password: CharArray): XmlSigningKeyMaterial {
        if (content.isEmpty()) throw DomainRuleViolation("PKCS#12 content cannot be empty.")
        if (password.isEmpty()) throw DomainRuleViolation("PKCS#12 password cannot be empty.")

        val keyStore = KeyStore.getInstance("PKCS12")
        ByteArrayInputStream(content).use { input -> keyStore.load(input, password) }

        val aliases = keyStore.aliases().toList()
        val alias = aliases.firstOrNull { keyStore.isKeyEntry(it) }
            ?: throw DomainRuleViolation("PKCS#12 file does not contain a private key entry.")

        val key = keyStore.getKey(alias, password)
            ?: throw DomainRuleViolation("PKCS#12 private key could not be read.")
        if (key !is PrivateKey) {
            throw DomainRuleViolation("PKCS#12 key entry is not a private key.")
        }

        val certificate = keyStore.getCertificate(alias)
            ?: throw DomainRuleViolation("PKCS#12 private key entry does not contain a certificate.")
        if (certificate !is X509Certificate) {
            throw DomainRuleViolation("PKCS#12 certificate is not X509.")
        }

        val chain = keyStore.getCertificateChain(alias)
            ?.mapNotNull { it as? X509Certificate }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(certificate)

        return XmlSigningKeyMaterial(
            privateKey = key,
            certificate = certificate,
            certificateChain = chain,
            alias = alias,
            certificateFingerprintSha256 = sha256Hex(certificate.encoded),
        )
    }

    private fun sha256Hex(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
