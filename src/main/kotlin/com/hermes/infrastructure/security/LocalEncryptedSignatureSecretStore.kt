package com.hermes.infrastructure.security

import com.hermes.application.electronicinvoicing.ElectronicSignatureSecretReader
import com.hermes.application.signature.SignatureSecretStore
import com.hermes.domain.shared.DomainRuleViolation
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Local encrypted fallback for development/test/self-hosted deployments.
 * Production can replace this adapter with KMS/Object Storage without changing application use cases.
 */
class LocalEncryptedSignatureSecretStore(
    private val baseDir: Path,
    secret: String,
    private val random: SecureRandom = SecureRandom(),
) : SignatureSecretStore, ElectronicSignatureSecretReader {
    private val key =
        SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(StandardCharsets.UTF_8)), "AES")

    init {
        if (secret.length < 32) throw IllegalArgumentException("SIGNATURE_SECRET_KEY must contain at least 32 characters.")
        Files.createDirectories(baseDir)
    }

    override fun storeEncryptedSignature(
        organizationId: String,
        signatureId: String,
        fileName: String,
        content: ByteArray,
    ): String {
        if (content.isEmpty()) throw DomainRuleViolation("Electronic signature content cannot be empty.")
        val key = objectKey(organizationId, signatureId, "certificate.p12.enc")
        writeEncrypted(key, content)
        return key
    }

    override fun storePasswordSecret(
        organizationId: String,
        signatureId: String,
        password: CharArray,
    ): String {
        if (password.isEmpty()) throw DomainRuleViolation("Electronic signature password cannot be empty.")
        val key = objectKey(organizationId, signatureId, "password.enc")
        writeEncrypted(key, String(password).toByteArray(StandardCharsets.UTF_8))
        return key
    }

    override fun readSignatureContent(encryptedFileObjectKey: String): ByteArray = readEncrypted(encryptedFileObjectKey)

    override fun readPassword(encryptedPasswordRef: String): CharArray =
        String(readEncrypted(encryptedPasswordRef), StandardCharsets.UTF_8).toCharArray()

    private fun writeEncrypted(objectKey: String, content: ByteArray) {
        val path = resolve(objectKey)
        Files.createDirectories(path.parent)
        Files.write(path, encrypt(content))
    }

    private fun readEncrypted(objectKey: String): ByteArray {
        val path = resolve(objectKey)
        if (!Files.exists(path)) throw DomainRuleViolation("Electronic signature secret does not exist.")
        return decrypt(Files.readAllBytes(path))
    }

    private fun encrypt(content: ByteArray): ByteArray {
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(content)
        return iv + encrypted
    }

    private fun decrypt(content: ByteArray): ByteArray {
        if (content.size <= 12) throw DomainRuleViolation("Encrypted signature secret is invalid.")
        val iv = content.copyOfRange(0, 12)
        val encrypted = content.copyOfRange(12, content.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun objectKey(organizationId: String, signatureId: String, fileName: String): String =
        listOf("signature-secrets", organizationId.safePath(), signatureId.safePath(), fileName).joinToString("/")

    private fun resolve(objectKey: String): Path =
        baseDir.resolve(objectKey.safeRelativePath()).normalize().also { path ->
            if (!path.startsWith(baseDir.normalize())) throw DomainRuleViolation("Invalid signature secret object key.")
        }

    private fun String.safePath(): String = replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    private fun String.safeRelativePath(): String = split('/').joinToString("/") { it.safePath() }

    companion object {
        fun default(): LocalEncryptedSignatureSecretStore = LocalEncryptedSignatureSecretStore(
            baseDir = Paths.get(System.getenv("SIGNATURE_SECRET_DIR") ?: "build/hermes-signature-secrets"),
            secret = System.getenv("SIGNATURE_SECRET_KEY") ?: "local-development-signature-secret-32chars-minimum",
        )
    }
}
