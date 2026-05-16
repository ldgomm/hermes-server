package com.hermes.application.auth

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecureTokenGeneratorTest {

    @Test
    fun `generates different long tokens`() {
        val generator = SecureTokenGenerator()

        val first = generator.generate()
        val second = generator.generate()

        assertNotEquals(first, second)
        assertTrue(first.length >= 40)
        assertTrue(second.length >= 40)
    }

    @Test
    fun `hashes token and compares without storing raw value`() {
        val token = SecureTokenGenerator().generate()
        val hash = TokenHasher.sha256(token)

        assertTrue(TokenHasher.matches(token, hash))
    }
}
