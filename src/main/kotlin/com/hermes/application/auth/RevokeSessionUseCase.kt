package com.hermes.application.auth

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class RevokeSessionUseCase(
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun revokeSession(command: RevokeSessionCommand): RevokeSessionResult {
        val now = Instant.now(clock)
        val session = sessionRepository.findSessionById(command.sessionId)
            ?: throw DomainRuleViolation("Session does not exist.")

        if (session.userId != command.actorUserId) {
            throw DomainRuleViolation("Cannot revoke another user's session without admin flow.")
        }

        val revoked = session.revoke(now, command.reason)
        sessionRepository.update(revoked)
        val revokedTokens = refreshTokenRepository.revokeActiveBySessionIds(setOf(session.id), now)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.SESSION_REVOKED,
                actorUserId = command.actorUserId,
                targetUserId = session.userId,
                organizationId = null,
                sessionId = session.id,
                ipAddress = session.ipAddress,
                userAgent = session.userAgent,
                message = command.reason,
                createdAt = now,
            ),
        )

        return RevokeSessionResult(revokedSessions = 1, revokedRefreshTokens = revokedTokens)
    }

    fun revokeAllUserSessions(command: RevokeAllUserSessionsCommand): RevokeSessionResult {
        val now = Instant.now(clock)
        val revokingSelf = command.actorUserId == command.targetUserId
        if (!revokingSelf) {
            PermissionRules.assertCanPerform(
                effectivePermissions = command.actorEffectivePermissions,
                permission = PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE,
            )
        }

        val activeSessions = sessionRepository.findActiveByUserId(command.targetUserId)
        if (activeSessions.isEmpty()) {
            return RevokeSessionResult(revokedSessions = 0, revokedRefreshTokens = 0)
        }

        activeSessions.forEach { session ->
            sessionRepository.update(session.revoke(now, command.reason))
        }
        val revokedTokens = refreshTokenRepository.revokeActiveBySessionIds(activeSessions.map { it.id }.toSet(), now)

        auditLogger.log(
            CredentialAuditEvent(
                action = if (revokingSelf) {
                    CredentialAuditAction.ALL_SESSIONS_REVOKED
                } else {
                    CredentialAuditAction.USER_SESSIONS_REVOKED_BY_ADMIN
                },
                actorUserId = command.actorUserId,
                targetUserId = command.targetUserId,
                organizationId = command.organizationId,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                message = command.reason,
                createdAt = now,
            ),
        )

        return RevokeSessionResult(
            revokedSessions = activeSessions.size,
            revokedRefreshTokens = revokedTokens,
        )
    }
}
