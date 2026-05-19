package com.hermes.infrastructure.mongo.mapping

import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.math.RoundingMode

object MongoDecimalMapper {
    const val MONEY_SCALE: Int = 2
    const val QUANTITY_SCALE: Int = 6
    const val PERCENTAGE_SCALE: Int = 4

    fun toDecimal128(
        value: BigDecimal,
        scale: Int? = null,
        roundingMode: RoundingMode = RoundingMode.HALF_UP,
    ): Decimal128 {
        val normalized = scale?.let { value.setScale(it, roundingMode) } ?: value.stripTrailingZeros()
        return Decimal128(normalized)
    }

    fun fromDecimal128(value: Decimal128): BigDecimal = value.bigDecimalValue()

    fun moneyToDecimal128(value: BigDecimal): Decimal128 = toDecimal128(value, scale = MONEY_SCALE)

    fun quantityToDecimal128(value: BigDecimal): Decimal128 = toDecimal128(value, scale = QUANTITY_SCALE)

    fun percentageToDecimal128(value: BigDecimal): Decimal128 = toDecimal128(value, scale = PERCENTAGE_SCALE)

    fun readRequired(document: Document, fieldName: String): BigDecimal {
        return readOptional(document, fieldName)
            ?: throw IllegalArgumentException("Required decimal field '$fieldName' is missing or null.")
    }

    fun readOptional(document: Document, fieldName: String): BigDecimal? {
        val raw = document[fieldName] ?: return null
        return when (raw) {
            is Decimal128 -> raw.bigDecimalValue()
            is BigDecimal -> raw
            is Int -> raw.toBigDecimal()
            is Long -> raw.toBigDecimal()
            is String -> raw.toBigDecimalOrNull()
                ?: throw IllegalArgumentException("Decimal field '$fieldName' contains an invalid string value: $raw")

            else -> throw IllegalArgumentException(
                "Decimal field '$fieldName' must be Decimal128, BigDecimal, number, or numeric string. Current type: ${raw::class.qualifiedName}",
            )
        }
    }

    fun write(
        document: Document,
        fieldName: String,
        value: BigDecimal,
        scale: Int? = null,
    ): Document {
        document[fieldName] = toDecimal128(value, scale = scale)
        return document
    }

    fun writeMoney(document: Document, fieldName: String, value: BigDecimal): Document =
        write(document, fieldName, value, scale = MONEY_SCALE)

    fun writeQuantity(document: Document, fieldName: String, value: BigDecimal): Document =
        write(document, fieldName, value, scale = QUANTITY_SCALE)

    fun writePercentage(document: Document, fieldName: String, value: BigDecimal): Document =
        write(document, fieldName, value, scale = PERCENTAGE_SCALE)
}
