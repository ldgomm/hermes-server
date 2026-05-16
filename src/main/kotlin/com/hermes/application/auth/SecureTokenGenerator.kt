package com.hermes.application.auth

import java.security.SecureRandom
import java.util.Base64

class SecureTokenGenerator(
    private val random: SecureRandom = SecureRandom(),
    private val bytes: Int = DEFAULT_BYTES,
) {
    init {
        require(bytes >= 32) { "Secure token must contain at least 32 random bytes." }
    }

    fun generate(): String {
        val value = ByteArray(bytes)
        random.nextBytes(value)
        return encoder.encodeToString(value)
    }

    companion object {
        private const val DEFAULT_BYTES = 48
        private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    }
}
