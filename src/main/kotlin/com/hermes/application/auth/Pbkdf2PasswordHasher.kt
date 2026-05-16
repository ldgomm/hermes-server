package com.hermes.application.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Pbkdf2PasswordHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val saltBytes: Int = DEFAULT_SALT_BYTES,
    private val keyBytes: Int = DEFAULT_KEY_BYTES,
    private val random: SecureRandom = SecureRandom(),
) : PasswordHasher {

    init {
        require(iterations >= 120_000) { "PBKDF2 iterations are too low." }
        require(saltBytes >= 16) { "PBKDF2 salt must be at least 16 bytes." }
        require(keyBytes >= 32) { "PBKDF2 key must be at least 32 bytes." }
    }

    override fun hash(password: CharArray): String {
        require(password.isNotEmpty()) { "Password cannot be empty." }

        val salt = ByteArray(saltBytes)
        random.nextBytes(salt)

        val hash = derive(password = password, salt = salt, iterations = iterations, keyBytes = keyBytes)

        return listOf(
            ALGORITHM_ID,
            iterations.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(hash),
        ).joinToString(separator = "$")
    }

    override fun verify(password: CharArray, encodedHash: String): Boolean {
        if (password.isEmpty()) return false

        val parts = encodedHash.split("$")
        if (parts.size != 4 || parts[0] != ALGORITHM_ID) return false

        val parsedIterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { decoder.decode(parts[2]) }.getOrNull() ?: return false
        val expectedHash = runCatching { decoder.decode(parts[3]) }.getOrNull() ?: return false

        val actualHash = derive(
            password = password,
            salt = salt,
            iterations = parsedIterations,
            keyBytes = expectedHash.size,
        )

        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    private fun derive(
        password: CharArray,
        salt: ByteArray,
        iterations: Int,
        keyBytes: Int,
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyBytes * 8)
        return try {
            SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val ALGORITHM_ID = "pbkdf2_sha256"
        private const val JCA_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val DEFAULT_ITERATIONS = 210_000
        private const val DEFAULT_SALT_BYTES = 32
        private const val DEFAULT_KEY_BYTES = 32

        private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
