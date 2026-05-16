package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ChangePasswordCommand): ChangePasswordResult {
        val now = Instant.now(clock)
        val user = userRepository.findUserById(command.userId)
            ?: throw DomainRuleViolation("User does not exist.")
        user.assertCanAuthenticate()

        val credential = credentialRepository.findByUserId(user.id)
            ?: throw DomainRuleViolation("User credential does not exist.")

        if (!passwordHasher.verify(command.currentPassword.toCharArray(), credential.passwordHash)) {
            throw DomainRuleViolation("Current password is invalid.")
        }

        passwordPolicy.assertValid(command.newPassword, email = user.email, displayName = user.displayName)

        val updatedCredential = credential.replacePassword(
            newPasswordHash = passwordHasher.hash(command.newPassword.toCharArray()),
            changedAt = now,
        )
        credentialRepository.update(updatedCredential)

        val sessionsToRevoke = if (command.revokeOtherSessions) {
            sessionRepository.findActiveByUserId(user.id).filter { it.id != command.sessionId }
        } else {
            emptyList()
        }
        sessionsToRevoke.forEach { session ->
            sessionRepository.update(session.revoke(now, "Password changed"))
        }
        val revokedTokens = refreshTokenRepository.revokeActiveBySessionIds(
            sessionsToRevoke.map { it.id }.toSet(),
            now,
        )

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.PASSWORD_CHANGED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = command.sessionId,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            )
        )
        if (credential.mustChangePassword) {
            auditLogger.log(
                CredentialAuditEvent(
                    action = CredentialAuditAction.PASSWORD_CHANGE_REQUIRED_COMPLETED,
                    actorUserId = user.id,
                    targetUserId = user.id,
                    organizationId = null,
                    sessionId = command.sessionId,
                    ipAddress = command.ipAddress,
                    userAgent = command.userAgent,
                    createdAt = now,
                )
            )
        }

        return ChangePasswordResult(
            userId = user.id,
            changedAt = now,
            revokedSessions = sessionsToRevoke.size,
            revokedRefreshTokens = revokedTokens,
        )
    }
}
