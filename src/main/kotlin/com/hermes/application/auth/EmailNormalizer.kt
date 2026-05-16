package com.hermes.application.auth

object EmailNormalizer {
    fun normalize(email: String): String =
        email.trim().lowercase()
}
