package com.hermes.application.catalog

import java.util.UUID

fun interface CatalogIdGenerator {
    fun newId(prefix: String): String
}

class UuidCatalogIdGenerator : CatalogIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}
