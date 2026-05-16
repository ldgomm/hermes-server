package com.hermes.application.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Pbkdf2PasswordHasherTest {

    private val hasher = Pbkdf2PasswordHasher()

    @Test
    fun `hashes and verifies passwords`() {
        val hash = hasher.hash("VeryStrong#2026".toCharArray())

        assertTrue(hasher.verify("VeryStrong#2026".toCharArray(), hash))
        assertFalse(hasher.verify("WrongPassword#2026".toCharArray(), hash))
    }

    @Test
    fun `hash includes algorithm and parameters`() {
        val hash = hasher.hash("VeryStrong#2026".toCharArray())

        assertTrue(hash.startsWith("${Pbkdf2PasswordHasher.ALGORITHM_ID}$"))
        assertTrue(hash.split("$").size == 4)
    }
}
