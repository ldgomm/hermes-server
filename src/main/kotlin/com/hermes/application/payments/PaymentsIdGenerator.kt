package com.hermes.application.payments

import java.util.UUID

fun interface PaymentsIdGenerator {
    fun newId(prefix: String): String
}

class UuidPaymentsIdGenerator : PaymentsIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}
