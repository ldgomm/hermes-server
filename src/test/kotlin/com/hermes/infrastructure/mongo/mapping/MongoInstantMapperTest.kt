package com.hermes.infrastructure.mongo.mapping

import org.bson.Document
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MongoInstantMapperTest {
    @Test
    fun `writes instant as java Date`() {
        val instant = Instant.parse("2026-05-16T01:00:00.123Z")
        val document = Document()

        MongoInstantMapper.write(document, "createdAt", instant)

        assertEquals(Date.from(instant), document["createdAt"])
    }

    @Test
    fun `reads ISO string as instant`() {
        val document = Document("createdAt", "2026-05-16T01:00:00Z")

        val result = MongoInstantMapper.readRequired(document, "createdAt")

        assertEquals(Instant.parse("2026-05-16T01:00:00Z"), result)
    }

    @Test
    fun `rejects invalid instant string`() {
        val document = Document("createdAt", "yesterday")

        assertFailsWith<IllegalArgumentException> {
            MongoInstantMapper.readRequired(document, "createdAt")
        }
    }
}
