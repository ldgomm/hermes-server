package com.hermes.application.auth

import java.time.Duration

data class AuthSecurityPolicy(
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(30),
    val sessionTtl: Duration = Duration.ofDays(30),
    val maxFailedLoginAttempts: Int = 5,
    val credentialLockDuration: Duration = Duration.ofMinutes(15),
) {
    init {
        require(!accessTokenTtl.isZero && !accessTokenTtl.isNegative) { "Access token TTL must be positive." }
        require(!refreshTokenTtl.isZero && !refreshTokenTtl.isNegative) { "Refresh token TTL must be positive." }
        require(!sessionTtl.isZero && !sessionTtl.isNegative) { "Session TTL must be positive." }
        require(maxFailedLoginAttempts >= 1) { "Max failed login attempts must be at least 1." }
        require(!credentialLockDuration.isZero && !credentialLockDuration.isNegative) {
            "Credential lock duration must be positive."
        }
    }
}
