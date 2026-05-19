package com.hermes.infrastructure.mongo.mapping

import org.bson.Document
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.*

object MongoInstantMapper {
    fun toDate(instant: Instant): Date = Date.from(instant)

    fun fromDate(date: Date): Instant = date.toInstant()

    fun readRequired(document: Document, fieldName: String): Instant {
        return readOptional(document, fieldName)
            ?: throw IllegalArgumentException("Required instant field '$fieldName' is missing or null.")
    }

    fun readOptional(document: Document, fieldName: String): Instant? {
        val raw = document[fieldName] ?: return null
        return when (raw) {
            is Date -> raw.toInstant()
            is Instant -> raw
            is String -> parseString(fieldName, raw)
            else -> throw IllegalArgumentException(
                "Instant field '$fieldName' must be Date, Instant, or ISO-8601 string. Current type: ${raw::class.qualifiedName}",
            )
        }
    }

    fun write(document: Document, fieldName: String, instant: Instant): Document {
        document[fieldName] = Date.from(instant)
        return document
    }

    private fun parseString(fieldName: String, value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (error: DateTimeParseException) {
            throw IllegalArgumentException(
                "Instant field '$fieldName' contains an invalid ISO-8601 value: $value", error
            )
        }
    }
}
