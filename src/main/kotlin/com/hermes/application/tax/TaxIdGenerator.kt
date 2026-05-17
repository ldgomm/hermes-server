package com.hermes.application.tax

import java.util.*

fun interface TaxIdGenerator {
    fun newId(prefix: String): String
}

class UuidTaxIdGenerator : TaxIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}