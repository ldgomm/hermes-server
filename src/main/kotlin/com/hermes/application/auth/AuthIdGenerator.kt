package com.hermes.application.auth

import java.util.UUID

fun interface AuthIdGenerator {
    fun newId(prefix: String): String
}

class UuidAuthIdGenerator : AuthIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}
