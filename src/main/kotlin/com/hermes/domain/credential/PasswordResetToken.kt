package com.hermes.domain.credential

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class PasswordResetToken(
    val id: String,
    val userId: String,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val usedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val requestedByIp: String? = null,
    val requestedByUserAgent: String? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Password reset token id cannot be blank.")
        if (userId.isBlank()) throw DomainRuleViolation("Password reset token user id cannot be blank.")
        if (tokenHash.isBlank()) throw DomainRuleViolation("Password reset token hash cannot be blank.")
        if (!expiresAt.isAfter(createdAt)) throw DomainRuleViolation("Password reset token expiration must be after creation.")
        if (version < 1) throw DomainRuleViolation("Password reset token version must be positive.")
    }

    val isUsed: Boolean get() = usedAt != null
    val isRevoked: Boolean get() = revokedAt != null

    fun assertUsable(now: Instant) {
        if (isUsed) throw DomainRuleViolation("Password reset token has already been used.")
        if (isRevoked) throw DomainRuleViolation("Password reset token has been revoked.")
        if (!expiresAt.isAfter(now)) throw DomainRuleViolation("Password reset token has expired.")
    }

    fun markUsed(now: Instant): PasswordResetToken {
        assertUsable(now)
        return copy(usedAt = now, version = version + 1)
    }

    fun revoke(now: Instant): PasswordResetToken {
        if (isRevoked) throw DomainRuleViolation("Password reset token is already revoked.")
        return copy(revokedAt = now, version = version + 1)
    }
}
