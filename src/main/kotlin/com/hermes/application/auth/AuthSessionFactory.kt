package com.hermes.application.auth

import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import java.time.Instant

class AuthSessionFactory(
    private val idGenerator: AuthIdGenerator,
    private val tokenGenerator: SecureTokenGenerator,
    private val jwtTokenService: JwtTokenService,
    private val policy: AuthSecurityPolicy = AuthSecurityPolicy(),
) {
    fun create(userId: String, now: Instant, userAgent: String? = null, ipAddress: String? = null): CreatedSessionBundle {
        val session = UserSession.create(
            id = idGenerator.newId("ses"),
            userId = userId,
            now = now,
            expiresAt = now.plus(policy.sessionTtl),
            userAgent = userAgent,
            ipAddress = ipAddress,
        )
        val rawRefreshToken = tokenGenerator.generate()
        val refreshToken = RefreshToken(
            id = idGenerator.newId("rt"),
            sessionId = session.id,
            userId = userId,
            tokenHash = TokenHasher.sha256(rawRefreshToken),
            createdAt = now,
            expiresAt = now.plus(policy.refreshTokenTtl),
        )
        val accessToken = jwtTokenService.issueAccessToken(
            userId = userId,
            sessionId = session.id,
            issuedAt = now,
        )

        return CreatedSessionBundle(
            session = session,
            refreshToken = refreshToken,
            rawRefreshToken = rawRefreshToken,
            accessToken = accessToken,
        )
    }
}
