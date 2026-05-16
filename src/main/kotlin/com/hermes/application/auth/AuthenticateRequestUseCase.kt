package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class AuthenticateRequestUseCase(
    private val repository: AuthContextRepository,
    private val jwtTokenService: JwtTokenService,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(accessToken: String): AuthenticatedPrincipal {
        if (accessToken.isBlank()) {
            throw DomainRuleViolation("Access token is required.")
        }

        val now = Instant.now(clock)
        val claims = jwtTokenService.validateAccessToken(accessToken, now)
        val session = repository.findSessionById(claims.sessionId)
            ?: throw DomainRuleViolation("Session does not exist.")

        if (session.userId != claims.userId) {
            throw DomainRuleViolation("Access token session does not belong to user.")
        }

        session.assertUsable(now)

        val user = repository.findUserById(claims.userId)
            ?: throw DomainRuleViolation("Authenticated user does not exist.")
        user.assertCanAuthenticate()

        return AuthenticatedPrincipal(
            user = user,
            session = session,
            claims = claims,
        )
    }
}
