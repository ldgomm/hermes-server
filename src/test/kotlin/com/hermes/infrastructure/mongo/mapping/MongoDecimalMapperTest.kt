package com.hermes.infrastructure.mongo.mapping

import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MongoDecimalMapperTest {
    @Test
    fun `writes money as Decimal128 with two decimals`() {
        val document = Document()

        MongoDecimalMapper.writeMoney(document, "amount", BigDecimal("10.235"))

        val stored = document["amount"] as Decimal128
        assertEquals("10.24", stored.bigDecimalValue().toPlainString())
    }

    @Test
    fun `reads Decimal128 back to BigDecimal`() {
        val document = Document("amount", Decimal128(BigDecimal("99.50")))

        val result = MongoDecimalMapper.readRequired(document, "amount")

        assertEquals("99.50", result.toPlainString())
    }

    @Test
    fun `rejects invalid decimal strings`() {
        val document = Document("amount", "not-a-decimal")

        assertFailsWith<IllegalArgumentException> {
            MongoDecimalMapper.readRequired(document, "amount")
        }
    }
}
