package com.hermes.domain.session

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class RefreshToken(
    val id: String,
    val sessionId: String,
    val userId: String,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val replacedByTokenId: String? = null,
    val reuseDetectedAt: Instant? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Refresh token id cannot be blank.")
        if (sessionId.isBlank()) throw DomainRuleViolation("Refresh token session id cannot be blank.")
        if (userId.isBlank()) throw DomainRuleViolation("Refresh token user id cannot be blank.")
        if (tokenHash.isBlank()) throw DomainRuleViolation("Refresh token hash cannot be blank.")
        if (!expiresAt.isAfter(createdAt)) throw DomainRuleViolation("Refresh token expiration must be after creation.")
        if (version < 1) throw DomainRuleViolation("Refresh token version must be greater than zero.")
    }

    val isRevoked: Boolean get() = revokedAt != null
    val isUsed: Boolean get() = usedAt != null

    fun assertUsable(now: Instant) {
        if (isRevoked) throw DomainRuleViolation("Refresh token has been revoked.")
        if (isUsed) throw DomainRuleViolation("Refresh token has already been used.")
        if (!expiresAt.isAfter(now)) throw DomainRuleViolation("Refresh token has expired.")
    }

    fun rotate(usedAt: Instant, replacementId: String): RefreshToken {
        assertUsable(usedAt)
        if (replacementId.isBlank()) throw DomainRuleViolation("Replacement token id cannot be blank.")

        return copy(
            usedAt = usedAt,
            revokedAt = usedAt,
            replacedByTokenId = replacementId,
            version = version + 1,
        )
    }

    fun markReuseDetected(detectedAt: Instant): RefreshToken =
        copy(
            reuseDetectedAt = detectedAt,
            revokedAt = revokedAt ?: detectedAt,
            version = version + 1,
        )
}
