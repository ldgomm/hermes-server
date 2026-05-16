package com.hermes.application.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordPolicyTest {

    private val policy = PasswordPolicy()

    @Test
    fun `accepts strong passwords`() {
        val result = policy.validate(
            password = "VeryStrong#2026",
            email = "owner@hermes.local",
            displayName = "Owner Hermes",
        )

        assertTrue(result.valid)
    }

    @Test
    fun `rejects weak passwords`() {
        val result = policy.validate(
            password = "weak",
            email = "owner@hermes.local",
            displayName = "Owner Hermes",
        )

        assertFalse(result.valid)
        assertTrue(PasswordFailure.TOO_SHORT in result.failures)
        assertTrue(PasswordFailure.MISSING_UPPERCASE in result.failures)
        assertTrue(PasswordFailure.MISSING_DIGIT in result.failures)
        assertTrue(PasswordFailure.MISSING_SYMBOL in result.failures)
    }

    @Test
    fun `rejects passwords containing email local part`() {
        val result = policy.validate(
            password = "Owner2026#owner",
            email = "owner@hermes.local",
            displayName = null,
        )

        assertFalse(result.valid)
        assertTrue(PasswordFailure.CONTAINS_EMAIL_LOCAL_PART in result.failures)
    }
}
