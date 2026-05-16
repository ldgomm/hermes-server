package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RefreshSessionUseCaseTest {
    @Test
    fun `rotates refresh token and issues new access token`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val session = UserSession.create("ses_1", user.id, now, now.plusSeconds(3600))
        val rawRefresh = "raw-refresh-token"
        val refreshToken = RefreshToken(
            id = "rt_existing",
            sessionId = session.id,
            userId = user.id,
            tokenHash = TokenHasher.sha256(rawRefresh),
            createdAt = now,
            expiresAt = now.plusSeconds(3600),
        )
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            sessions = mutableMapOf(session.id to session),
            refreshTokens = mutableMapOf(refreshToken.id to refreshToken),
        )
        val useCase = state.refreshUseCase()

        val result = useCase.execute(RefreshSessionCommand(rawRefresh))

        assertEquals(user.id, result.userId)
        assertEquals(session.id, result.sessionId)
        assertNotEquals(rawRefresh, result.refreshToken)
        assertEquals(2, state.refreshTokens.size)
        assertEquals(CredentialAuditAction.REFRESH_TOKEN_ROTATED, state.auditLogger.events.last().action)
    }

    @Test
    fun `detects reuse and revokes session`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val session = UserSession.create("ses_1", user.id, now, now.plusSeconds(3600))
        val rawRefresh = "raw-refresh-token"
        val refreshToken = RefreshToken(
            id = "rt_1",
            sessionId = session.id,
            userId = user.id,
            tokenHash = TokenHasher.sha256(rawRefresh),
            createdAt = now,
            expiresAt = now.plusSeconds(3600),
            usedAt = now.plusSeconds(10),
            revokedAt = now.plusSeconds(10),
        )
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            sessions = mutableMapOf(session.id to session),
            refreshTokens = mutableMapOf(refreshToken.id to refreshToken),
        )
        val useCase = state.refreshUseCase()

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(RefreshSessionCommand(rawRefresh))
        }

        assertEquals(UserSessionStatus.REVOKED, state.sessions.getValue(session.id).status)
        assertEquals(CredentialAuditAction.REFRESH_TOKEN_REUSE_DETECTED, state.auditLogger.events.last().action)
    }
}
