package com.hermes.application.auth

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionRevocationUseCaseTest {
    @Test
    fun `user can revoke own session`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val session = UserSession.create("ses_1", "usr_1", now, now.plusSeconds(3600))
        val refreshToken = RefreshToken(
            id = "rt_1",
            sessionId = session.id,
            userId = session.userId,
            tokenHash = TokenHasher.sha256("raw"),
            createdAt = now,
            expiresAt = now.plusSeconds(3600),
        )
        val state = FakeAuthState(
            sessions = mutableMapOf(session.id to session),
            refreshTokens = mutableMapOf(refreshToken.id to refreshToken),
        )
        val useCase = state.revokeUseCase()

        val result = useCase.revokeSession(
            RevokeSessionCommand(
                sessionId = session.id,
                actorUserId = session.userId,
                reason = "logout",
            ),
        )

        assertEquals(1, result.revokedSessions)
        assertEquals(1, result.revokedRefreshTokens)
        assertEquals(UserSessionStatus.REVOKED, state.sessions.getValue(session.id).status)
        assertEquals(CredentialAuditAction.SESSION_REVOKED, state.auditLogger.events.last().action)
    }

    @Test
    fun `admin needs permission to revoke another user sessions`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val session = UserSession.create("ses_1", "usr_target", now, now.plusSeconds(3600))
        val state = FakeAuthState(sessions = mutableMapOf(session.id to session))
        val useCase = state.revokeUseCase()

        assertFailsWith<DomainRuleViolation> {
            useCase.revokeAllUserSessions(
                RevokeAllUserSessionsCommand(
                    targetUserId = "usr_target",
                    actorUserId = "usr_admin",
                    reason = "admin revoke",
                    actorEffectivePermissions = emptySet(),
                ),
            )
        }

        val result = useCase.revokeAllUserSessions(
            RevokeAllUserSessionsCommand(
                targetUserId = "usr_target",
                actorUserId = "usr_admin",
                reason = "admin revoke",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE),
            ),
        )

        assertEquals(1, result.revokedSessions)
        assertEquals(CredentialAuditAction.USER_SESSIONS_REVOKED_BY_ADMIN, state.auditLogger.events.last().action)
    }
}
