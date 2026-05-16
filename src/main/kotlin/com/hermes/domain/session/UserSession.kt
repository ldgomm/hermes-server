package com.hermes.domain.session

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class UserSession(
    val id: String,
    val userId: String,
    val status: UserSessionStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastSeenAt: Instant? = null,
    val revokedAt: Instant? = null,
    val revokedReason: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Session id cannot be blank.")
        if (userId.isBlank()) throw DomainRuleViolation("Session user id cannot be blank.")
        if (!expiresAt.isAfter(createdAt)) throw DomainRuleViolation("Session expiration must be after creation.")
        if (version < 1) throw DomainRuleViolation("Session version must be greater than zero.")
        if (status == UserSessionStatus.REVOKED && revokedAt == null) {
            throw DomainRuleViolation("Revoked session requires revokedAt.")
        }
        if (status == UserSessionStatus.REVOKED && revokedReason.isNullOrBlank()) {
            throw DomainRuleViolation("Revoked session requires revokedReason.")
        }
    }

    fun assertUsable(now: Instant) {
        if (status == UserSessionStatus.REVOKED) throw DomainRuleViolation("Session has been revoked.")
        if (status == UserSessionStatus.EXPIRED || !expiresAt.isAfter(now)) {
            throw DomainRuleViolation("Session has expired.")
        }
    }

    fun touch(now: Instant): UserSession {
        assertUsable(now)
        return copy(lastSeenAt = now, version = version + 1)
    }

    fun revoke(now: Instant, reason: String): UserSession {
        if (status == UserSessionStatus.REVOKED) throw DomainRuleViolation("Session is already revoked.")
        if (reason.isBlank()) throw DomainRuleViolation("Session revoked reason cannot be blank.")

        return copy(
            status = UserSessionStatus.REVOKED,
            revokedAt = now,
            revokedReason = reason.trim(),
            version = version + 1,
        )
    }

    companion object {
        fun create(
            id: String,
            userId: String,
            now: Instant,
            expiresAt: Instant,
            userAgent: String? = null,
            ipAddress: String? = null,
        ): UserSession = UserSession(
            id = id,
            userId = userId,
            status = UserSessionStatus.ACTIVE,
            createdAt = now,
            expiresAt = expiresAt,
            userAgent = userAgent?.trim()?.takeIf { it.isNotBlank() },
            ipAddress = ipAddress?.trim()?.takeIf { it.isNotBlank() },
        )
    }
}
