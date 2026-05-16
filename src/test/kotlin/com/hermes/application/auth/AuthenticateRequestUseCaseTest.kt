package com.hermes.application.auth

import com.hermes.domain.session.UserSession
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthenticateRequestUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-16T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val jwt = HmacJwtTokenService(
        secret = "12345678901234567890123456789012",
        accessTokenTtlSeconds = 900,
    )

    @Test
    fun `authenticates request when token session and user are valid`() {
        val repository = FakeAuthContextRepository()
        val user = User.createOwner("usr_1", "owner@example.com", "Owner", now)
        val session = UserSession.create("ses_1", user.id, now, now.plusSeconds(3600))
        repository.users[user.id] = user
        repository.sessions[session.id] = session

        val token = jwt.issueAccessToken(user.id, session.id, now).token
        val useCase = AuthenticateRequestUseCase(repository, jwt, clock)

        val result = useCase.execute(token)

        assertEquals(user.id, result.user.id)
        assertEquals(session.id, result.session.id)
    }

    @Test
    fun `rejects token when session is revoked`() {
        val repository = FakeAuthContextRepository()
        val user = User.createOwner("usr_1", "owner@example.com", "Owner", now)
        val session = UserSession
            .create("ses_1", user.id, now, now.plusSeconds(3600))
            .revoke(now.plusSeconds(1), "test")
        repository.users[user.id] = user
        repository.sessions[session.id] = session

        val token = jwt.issueAccessToken(user.id, session.id, now).token
        val useCase = AuthenticateRequestUseCase(repository, jwt, clock)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(token)
        }
    }
}
