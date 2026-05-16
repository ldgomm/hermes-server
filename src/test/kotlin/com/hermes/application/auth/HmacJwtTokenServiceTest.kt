package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HmacJwtTokenServiceTest {
    private val service = HmacJwtTokenService(
        secret = "01234567890123456789012345678901",
        issuer = "hermes-test",
        accessTokenTtlSeconds = 900,
    )

    @Test
    fun `issues and validates access token`() {
        val issuedAt = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val token = service.issueAccessToken(
            userId = "usr_1",
            sessionId = "ses_1",
            issuedAt = issuedAt,
        )

        val claims = service.validateAccessToken(
            token = token.token,
            now = issuedAt.plusSeconds(10),
        )

        assertEquals("usr_1", claims.userId)
        assertEquals("ses_1", claims.sessionId)
        assertEquals(issuedAt.plusSeconds(900), claims.expiresAt)
    }

    @Test
    fun `rejects expired access token`() {
        val issuedAt = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val token = service.issueAccessToken("usr_1", "ses_1", issuedAt)

        assertFailsWith<DomainRuleViolation> {
            service.validateAccessToken(token.token, issuedAt.plusSeconds(901))
        }
    }

    @Test
    fun `rejects tampered access token`() {
        val issuedAt = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val token = service.issueAccessToken("usr_1", "ses_1", issuedAt)
        val tampered = token.token.dropLast(2) + "xx"

        assertFailsWith<DomainRuleViolation> {
            service.validateAccessToken(tampered, issuedAt.plusSeconds(10))
        }
    }
}
