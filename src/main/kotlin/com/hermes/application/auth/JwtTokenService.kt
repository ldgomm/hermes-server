package com.hermes.application.auth

import java.time.Instant

data class JwtAccessToken(
    val token: String,
    val expiresAt: Instant,
)

data class JwtClaims(
    val userId: String,
    val sessionId: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val issuer: String,
)

interface JwtTokenService {
    fun issueAccessToken(userId: String, sessionId: String, issuedAt: Instant = Instant.now()): JwtAccessToken
    fun validateAccessToken(token: String, now: Instant = Instant.now()): JwtClaims
}
