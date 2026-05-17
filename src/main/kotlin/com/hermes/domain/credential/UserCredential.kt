package com.hermes.domain.credential

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class UserCredential(
    val id: String,
    val userId: String,
    val passwordHash: String,
    val status: CredentialStatus,
    val mustChangePassword: Boolean,
    val temporaryPassword: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastPasswordChangedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val failedAttempts: Int = 0,
    val lockedUntil: Instant? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Credential id cannot be blank.")
        if (userId.isBlank()) throw DomainRuleViolation("Credential user id cannot be blank.")
        if (passwordHash.isBlank()) throw DomainRuleViolation("Credential password hash cannot be blank.")
        if (failedAttempts < 0) throw DomainRuleViolation("Credential failed attempts cannot be negative.")
        if (version < 1) throw DomainRuleViolation("Credential version must be greater than zero.")

        if (status == CredentialStatus.REVOKED && revokedAt == null) {
            throw DomainRuleViolation("Revoked credential requires revokedAt.")
        }

        if (status == CredentialStatus.LOCKED && lockedUntil == null) {
            throw DomainRuleViolation("Locked credential requires lockedUntil.")
        }

        if (temporaryPassword && !mustChangePassword) {
            throw DomainRuleViolation("Temporary credential must require password change.")
        }
    }

    /**
     * Normal application access.
     */
    fun assertCanAuthenticate(now: Instant) {
        CredentialRules.assertCanAuthenticate(status)
        assertNotCurrentlyLocked(now)
    }

    /**
     * Login access.
     *
     * Temporary credentials and credentials requiring password change can login,
     * but the caller must enforce the password-change flow before normal access.
     */
    fun assertCanStartLogin(now: Instant) {
        CredentialRules.assertCanStartLogin(status)
        assertNotCurrentlyLocked(now)
    }

    fun recordFailedAttempt(
        now: Instant,
        maxAttempts: Int,
        lockDurationSeconds: Long,
    ): UserCredential {
        if (maxAttempts < 1) throw DomainRuleViolation("Max attempts must be greater than zero.")
        if (lockDurationSeconds < 1) throw DomainRuleViolation("Lock duration must be greater than zero.")

        val nextAttempts = failedAttempts + 1
        val shouldLock = nextAttempts >= maxAttempts

        return copy(
            failedAttempts = nextAttempts,
            status = if (shouldLock) CredentialStatus.LOCKED else status,
            lockedUntil = if (shouldLock) now.plusSeconds(lockDurationSeconds) else lockedUntil,
            updatedAt = now,
            version = version + 1,
        )
    }

    fun recordSuccessfulAuthentication(now: Instant): UserCredential =
        copy(
            failedAttempts = 0,
            lockedUntil = null,
            updatedAt = now,
            version = version + 1,
        )

    fun replacePassword(
        newPasswordHash: String,
        changedAt: Instant,
    ): UserCredential {
        if (newPasswordHash.isBlank()) {
            throw DomainRuleViolation("New password hash cannot be blank.")
        }

        CredentialRules.assertCanStartPasswordChange(status)

        return copy(
            passwordHash = newPasswordHash,
            status = CredentialStatus.ACTIVE,
            mustChangePassword = false,
            temporaryPassword = false,
            failedAttempts = 0,
            lockedUntil = null,
            lastPasswordChangedAt = changedAt,
            updatedAt = changedAt,
            version = version + 1,
        )
    }

    fun forcePasswordChange(updatedAt: Instant): UserCredential {
        CredentialRules.assertCanForcePasswordChange(status)

        return copy(
            status = CredentialStatus.FORCE_CHANGE_REQUIRED,
            mustChangePassword = true,
            updatedAt = updatedAt,
            version = version + 1,
        )
    }

    fun revoke(revokedAt: Instant): UserCredential {
        CredentialRules.assertCanRevoke(status)

        return copy(
            status = CredentialStatus.REVOKED,
            revokedAt = revokedAt,
            updatedAt = revokedAt,
            version = version + 1,
        )
    }

    private fun assertNotCurrentlyLocked(now: Instant) {
        if (lockedUntil != null && now.isBefore(lockedUntil)) {
            throw DomainRuleViolation("Credential is temporarily locked.")
        }
    }

    companion object {
        fun createPasswordCredential(
            id: String,
            userId: String,
            passwordHash: String,
            now: Instant,
            temporary: Boolean = false,
        ): UserCredential =
            UserCredential(
                id = id,
                userId = userId,
                passwordHash = passwordHash,
                status = if (temporary) CredentialStatus.TEMPORARY else CredentialStatus.ACTIVE,
                mustChangePassword = temporary,
                temporaryPassword = temporary,
                createdAt = now,
                updatedAt = now,
                lastPasswordChangedAt = if (temporary) null else now,
            )
    }
}