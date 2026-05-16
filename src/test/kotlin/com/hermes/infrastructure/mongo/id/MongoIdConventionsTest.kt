package com.hermes.infrastructure.mongo.id

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MongoIdConventionsTest {
    @Test
    fun `accepts semantic prefixed ids`() {
        assertTrue(MongoId.isValid("org_01HXYZABC123"))
        assertTrue(MongoId.isValid("role_owner"))
        assertTrue(MongoId.isValid("sale_1234567890abcdef"))
    }

    @Test
    fun `rejects unsafe ids`() {
        assertFailsWith<IllegalArgumentException> { MongoId.of("") }
        assertFailsWith<IllegalArgumentException> { MongoId.of("org") }
        assertFailsWith<IllegalArgumentException> { MongoId.of("Org_123") }
        assertFailsWith<IllegalArgumentException> { MongoId.of("org_*") }
    }

    @Test
    fun `requires expected prefix`() {
        assertFailsWith<IllegalArgumentException> {
            MongoId.of("sale_123456", MongoIdPrefix.ORGANIZATION)
        }
    }

    @Test
    fun `generates id with requested prefix`() {
        val id = MongoIdGenerator.newId(MongoIdPrefix.ORGANIZATION)

        id.requirePrefix(MongoIdPrefix.ORGANIZATION)
        assertTrue(id.value.startsWith("org_"))
    }
}
