package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class LoginUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordHasher: PasswordHasher,
    private val sessionFactory: AuthSessionFactory,
    private val securityPolicy: AuthSecurityPolicy = AuthSecurityPolicy(),
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: LoginCommand): AuthTokenResult {
        val now = Instant.now(clock)
        val email = EmailNormalizer.normalize(command.email)
        val user = userRepository.findUserByEmail(email)

        if (user == null) {
            auditLoginFailed(now, null, command, "User not found")
            throw invalidCredentials()
        }

        val credential = credentialRepository.findByUserId(user.id)
        if (credential == null) {
            auditLoginFailed(now, user.id, command, "Credential not found")
            throw invalidCredentials()
        }

        try {
            user.assertCanAuthenticate()
            credential.assertCanStartLogin(now)
        } catch (error: DomainRuleViolation) {
            auditLoginFailed(now, user.id, command, error.message)
            throw error
        }

        val passwordMatches = passwordHasher.verify(command.password.toCharArray(), credential.passwordHash)
        if (!passwordMatches) {
            val updatedCredential = credential.recordFailedAttempt(
                now = now,
                maxAttempts = securityPolicy.maxFailedLoginAttempts,
                lockDurationSeconds = securityPolicy.credentialLockDuration.seconds,
            )
            credentialRepository.update(updatedCredential)
            auditLoginFailed(now, user.id, command, "Invalid password")
            throw invalidCredentials()
        }

        val updatedCredential = credential.recordSuccessfulAuthentication(now)
        credentialRepository.update(updatedCredential)

        val bundle = sessionFactory.create(
            userId = user.id,
            now = now,
            userAgent = command.userAgent,
            ipAddress = command.ipAddress,
        )
        sessionRepository.create(bundle.session)
        refreshTokenRepository.create(bundle.refreshToken)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.LOGIN_SUCCEEDED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = bundle.session.id,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            ),
        )

        return AuthTokenResult(
            accessToken = bundle.accessToken.token,
            accessTokenExpiresAt = bundle.accessToken.expiresAt,
            refreshToken = bundle.rawRefreshToken,
            refreshTokenExpiresAt = bundle.refreshToken.expiresAt,
            sessionId = bundle.session.id,
            userId = user.id,
            mustChangePassword = updatedCredential.mustChangePassword,
        )
    }

    private fun auditLoginFailed(now: Instant, userId: String?, command: LoginCommand, message: String?) {
        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.LOGIN_FAILED,
                actorUserId = userId,
                targetUserId = userId,
                organizationId = null,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                message = message,
                createdAt = now,
            ),
        )
    }

    private fun invalidCredentials(): DomainRuleViolation =
        DomainRuleViolation("Invalid email or password.")
}
