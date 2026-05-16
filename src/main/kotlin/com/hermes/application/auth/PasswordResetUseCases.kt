package com.hermes.application.auth

import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Duration
import java.time.Instant

class RequestPasswordResetUseCase(
    private val userRepository: UserRepository,
    private val resetTokenRepository: PasswordResetTokenRepository,
    private val idGenerator: AuthIdGenerator,
    private val tokenGenerator: SecureTokenGenerator,
    private val delivery: PasswordResetDelivery = NoopPasswordResetDelivery,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val resetTokenTtl: Duration = Duration.ofMinutes(30),
    private val exposeTokenInResult: Boolean = false,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RequestPasswordResetCommand): RequestPasswordResetResult {
        val now = Instant.now(clock)
        val email = EmailNormalizer.normalize(command.email)
        val user = userRepository.findUserByEmail(email) ?: return RequestPasswordResetResult()

        val rawToken = tokenGenerator.generate()
        val token = PasswordResetToken(
            id = idGenerator.newId("prt"),
            userId = user.id,
            tokenHash = TokenHasher.sha256(rawToken),
            createdAt = now,
            expiresAt = now.plus(resetTokenTtl),
            requestedByIp = command.ipAddress,
            requestedByUserAgent = command.userAgent,
        )
        resetTokenRepository.revokeActiveForUser(user.id, now)
        resetTokenRepository.create(token)

        val resetUrl = delivery.buildResetUrl(rawToken)
        delivery.deliverPasswordReset(email, rawToken, resetUrl)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.PASSWORD_RESET_REQUESTED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            )
        )

        return RequestPasswordResetResult(
            accepted = true,
            rawResetToken = rawToken.takeIf { exposeTokenInResult },
            resetUrl = resetUrl.takeIf { exposeTokenInResult },
            expiresAt = token.expiresAt.takeIf { exposeTokenInResult },
        )
    }
}

class ConfirmPasswordResetUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val resetTokenRepository: PasswordResetTokenRepository,
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ConfirmPasswordResetCommand): ConfirmPasswordResetResult {
        val now = Instant.now(clock)
        val tokenHash = TokenHasher.sha256(command.resetToken)
        val token = resetTokenRepository.findPasswordResetTokenByHash(tokenHash)
            ?: throw DomainRuleViolation("Password reset token is invalid.")
        token.assertUsable(now)

        val user = userRepository.findUserById(token.userId)
            ?: throw DomainRuleViolation("Password reset user does not exist.")
        val credential = credentialRepository.findByUserId(user.id)
            ?: throw DomainRuleViolation("User credential does not exist.")

        passwordPolicy.assertValid(command.newPassword, email = user.email, displayName = user.displayName)

        val updatedCredential = credential.replacePassword(
            newPasswordHash = passwordHasher.hash(command.newPassword.toCharArray()),
            changedAt = now,
        )
        credentialRepository.update(updatedCredential)

        val usedToken = token.markUsed(now)
        resetTokenRepository.update(usedToken)

        val activeSessions = sessionRepository.findActiveByUserId(user.id)
        activeSessions.forEach { session ->
            sessionRepository.update(session.revoke(now, "Password reset completed"))
        }
        val revokedTokens = refreshTokenRepository.revokeActiveBySessionIds(activeSessions.map { it.id }.toSet(), now)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.PASSWORD_RESET_COMPLETED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            )
        )

        return ConfirmPasswordResetResult(
            userId = user.id,
            resetToken = usedToken,
            revokedSessions = activeSessions.size,
            revokedRefreshTokens = revokedTokens,
        )
    }
}
