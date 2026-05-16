package com.hermes.domain.user

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserTest {

    @Test
    fun `creates owner with normalized email`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")

        val user = User.createOwner(
            id = "usr_1",
            email = " OWNER@Hermes.Local ",
            displayName = "Owner",
            now = now,
        )

        assertEquals("owner@hermes.local", user.email)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `blocked user cannot authenticate`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner(
            id = "usr_1",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        ).block(
            reason = "security review",
            blockedAt = now.plusSeconds(60),
        )

        assertFailsWith<DomainRuleViolation> {
            user.assertCanAuthenticate()
        }
    }
}
