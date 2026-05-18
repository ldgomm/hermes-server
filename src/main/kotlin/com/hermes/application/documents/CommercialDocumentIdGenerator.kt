package com.hermes.application.documents

import java.util.*

fun interface CommercialDocumentIdGenerator {
    fun newId(prefix: String): String
}

class UuidCommercialDocumentIdGenerator : CommercialDocumentIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}
