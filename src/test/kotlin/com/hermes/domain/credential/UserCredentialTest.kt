package com.hermes.domain.credential

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class UserCredentialTest {

    @Test
    fun `temporary credential requires password change`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")

        val credential = UserCredential.createPasswordCredential(
            id = "cred_1",
            userId = "usr_1",
            passwordHash = "hash",
            now = now,
            temporary = true,
        )

        assertEquals(CredentialStatus.TEMPORARY, credential.status)
        assertEquals(true, credential.mustChangePassword)
        assertEquals(true, credential.temporaryPassword)
    }

    @Test
    fun `replace password activates credential and clears temporary flags`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val credential = UserCredential.createPasswordCredential(
            id = "cred_1",
            userId = "usr_1",
            passwordHash = "old-hash",
            now = now,
            temporary = true,
        )

        val updated = credential.replacePassword(
            newPasswordHash = "new-hash",
            changedAt = now.plusSeconds(60),
        )

        assertEquals(CredentialStatus.ACTIVE, updated.status)
        assertFalse(updated.mustChangePassword)
        assertFalse(updated.temporaryPassword)
        assertEquals("new-hash", updated.passwordHash)
    }

    @Test
    fun `revoked credential cannot authenticate`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val credential = UserCredential.createPasswordCredential(
            id = "cred_1",
            userId = "usr_1",
            passwordHash = "hash",
            now = now,
        ).revoke(now.plusSeconds(60))

        assertFailsWith<DomainRuleViolation> {
            credential.assertCanAuthenticate(now.plusSeconds(120))
        }
    }
}
