package com.hermes.application.auth

import com.hermes.domain.session.RefreshToken
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class RefreshSessionUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: UserSessionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenService: JwtTokenService,
    private val tokenGenerator: SecureTokenGenerator,
    private val idGenerator: AuthIdGenerator,
    private val securityPolicy: AuthSecurityPolicy = AuthSecurityPolicy(),
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RefreshSessionCommand): RefreshSessionResult {
        val now = Instant.now(clock)
        val tokenHash = TokenHasher.sha256(command.refreshToken)
        val existingToken = refreshTokenRepository.findRefreshTokenByHash(tokenHash)
            ?: throw DomainRuleViolation("Invalid refresh token.")

        val session = sessionRepository.findSessionById(existingToken.sessionId)
            ?: throw DomainRuleViolation("Refresh token session does not exist.")

        if (existingToken.isUsed || existingToken.isRevoked) {
            refreshTokenRepository.update(existingToken.markReuseDetected(now))
            sessionRepository.update(session.revoke(now, "Refresh token reuse detected"))
            auditLogger.log(
                CredentialAuditEvent(
                    action = CredentialAuditAction.REFRESH_TOKEN_REUSE_DETECTED,
                    actorUserId = existingToken.userId,
                    targetUserId = existingToken.userId,
                    organizationId = null,
                    sessionId = session.id,
                    ipAddress = null,
                    userAgent = null,
                    createdAt = now,
                ),
            )
            throw DomainRuleViolation("Refresh token reuse detected.")
        }

        existingToken.assertUsable(now)
        session.assertUsable(now)

        val user = userRepository.findUserById(existingToken.userId)
            ?: throw DomainRuleViolation("Refresh token user does not exist.")
        user.assertCanAuthenticate()

        val newRawRefreshToken = tokenGenerator.generate()
        val newRefreshTokenId = idGenerator.newId("rt")
        val rotatedOldToken = existingToken.rotate(
            usedAt = now,
            replacementId = newRefreshTokenId,
        )
        val newRefreshToken = RefreshToken(
            id = newRefreshTokenId,
            sessionId = session.id,
            userId = user.id,
            tokenHash = TokenHasher.sha256(newRawRefreshToken),
            createdAt = now,
            expiresAt = now.plus(securityPolicy.refreshTokenTtl),
        )
        refreshTokenRepository.rotate(rotatedOldToken, newRefreshToken)

        val accessToken = jwtTokenService.issueAccessToken(
            userId = user.id,
            sessionId = session.id,
            issuedAt = now,
        )

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.REFRESH_TOKEN_ROTATED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = session.id,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )

        return RefreshSessionResult(
            accessToken = accessToken.token,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = newRawRefreshToken,
            refreshTokenExpiresAt = newRefreshToken.expiresAt,
            sessionId = session.id,
            userId = user.id,
        )
    }
}
