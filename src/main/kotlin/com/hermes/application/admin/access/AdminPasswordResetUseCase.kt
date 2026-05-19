package com.hermes.application.admin.access

import com.hermes.application.auth.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class AdminResetUserPasswordUseCase(
    private val accessRepository: AdminAccessRepository,
    private val credentialRepository: UserCredentialRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val tokenGenerator: SecureTokenGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: AdminResetUserPasswordCommand): AdminResetUserPasswordResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD,
        )

        val now = Instant.now(clock)
        val organizationId = command.organizationId.required("Organization id")
        val actorUserId = command.actorUserId.required("Actor user id")
        val userId = command.userId.required("User id")
        val reason = command.reason.required("Password reset reason")

        val record = accessRepository.findUserAccess(organizationId, userId)
            ?: throw DomainRuleViolation("Target user does not belong to this organization.")

        val credential = credentialRepository.findByUserId(userId)
            ?: throw DomainRuleViolation("Target user credential does not exist.")

        val temporaryPassword =
            command.temporaryPassword?.takeIf { it.isNotBlank() } ?: (tokenGenerator.generate().take(24) + "aA1!")

        passwordPolicy.assertValid(
            password = temporaryPassword,
            email = record.user.email,
            displayName = record.user.displayName,
        )

        val updatedCredential = credential.replacePassword(
            newPasswordHash = passwordHasher.hash(temporaryPassword.toCharArray()),
            changedAt = now,
        ).forcePasswordChange(now)

        credentialRepository.update(updatedCredential)

        val activeSessions = if (command.revokeSessions) {
            accessRepository.findActiveSessionsByUserId(userId)
        } else {
            emptyList()
        }

        activeSessions.forEach { session ->
            accessRepository.updateSession(session.revoke(now, reason))
        }

        val revokedTokens = accessRepository.revokeActiveRefreshTokensBySessionIds(
            sessionIds = activeSessions.map { it.id }.toSet(),
            revokedAt = now,
        )

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.TEMPORARY_PASSWORD_CREATED,
                actorUserId = actorUserId,
                targetUserId = userId,
                organizationId = organizationId,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                message = reason,
                createdAt = now,
            )
        )

        return AdminResetUserPasswordResult(
            userId = userId,
            credentialId = updatedCredential.id,
            temporaryPassword = temporaryPassword,
            mustChangePassword = updatedCredential.mustChangePassword,
            revokedSessions = activeSessions.size,
            revokedRefreshTokens = revokedTokens,
            changedAt = now,
        )
    }
}
