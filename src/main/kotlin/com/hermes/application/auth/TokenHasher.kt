package com.hermes.application.auth

import java.security.MessageDigest
import java.util.*

object TokenHasher {
    fun sha256(rawToken: String): String {
        require(rawToken.isNotBlank()) { "Token cannot be blank." }

        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun matches(rawToken: String, expectedHash: String): Boolean {
        if (rawToken.isBlank() || expectedHash.isBlank()) return false
        return MessageDigest.isEqual(
            sha256(rawToken).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8),
        )
    }
}
