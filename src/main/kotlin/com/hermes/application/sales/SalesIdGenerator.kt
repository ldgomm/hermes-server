package com.hermes.application.sales

import java.util.UUID

fun interface SalesIdGenerator {
    fun newId(prefix: String): String
}

class UuidSalesIdGenerator : SalesIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

object SaleNumberFactory {
    fun fromId(saleId: String): String = "SALE-${saleId.takeLast(10).uppercase()}"
}
