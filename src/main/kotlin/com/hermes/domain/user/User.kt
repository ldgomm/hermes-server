package com.hermes.domain.user

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val status: UserStatus,
    val phone: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val blockedAt: Instant? = null,
    val blockedReason: String? = null,
    val archivedAt: Instant? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("User id cannot be blank.")
        if (email.isBlank()) throw DomainRuleViolation("User email cannot be blank.")
        if (email != email.trim().lowercase()) {
            throw DomainRuleViolation("User email must be normalized before creating the user.")
        }
        if (!EMAIL_REGEX.matches(email)) {
            throw DomainRuleViolation("User email is not valid.")
        }
        if (displayName.isBlank()) throw DomainRuleViolation("User display name cannot be blank.")
        if (phone != null && phone.isBlank()) throw DomainRuleViolation("User phone cannot be blank when provided.")
        if (version < 1) throw DomainRuleViolation("User version must be greater than zero.")
        if (status == UserStatus.BLOCKED && blockedAt == null) {
            throw DomainRuleViolation("Blocked user requires blockedAt.")
        }
        if (status == UserStatus.BLOCKED && blockedReason.isNullOrBlank()) {
            throw DomainRuleViolation("Blocked user requires blockedReason.")
        }
        if (status == UserStatus.ARCHIVED && archivedAt == null) {
            throw DomainRuleViolation("Archived user requires archivedAt.")
        }
    }

    fun assertCanAuthenticate() {
        when (status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.INVITED -> throw DomainRuleViolation("Invited user must accept invitation before authenticating.")
            UserStatus.SUSPENDED -> throw DomainRuleViolation("Suspended user cannot authenticate.")
            UserStatus.BLOCKED -> throw DomainRuleViolation("Blocked user cannot authenticate.")
            UserStatus.DISABLED -> throw DomainRuleViolation("Disabled user cannot authenticate.")
            UserStatus.ARCHIVED -> throw DomainRuleViolation("Archived user cannot authenticate.")
        }
    }

    fun block(reason: String, blockedAt: Instant): User {
        if (status == UserStatus.BLOCKED) throw DomainRuleViolation("User is already blocked.")
        if (status == UserStatus.ARCHIVED) throw DomainRuleViolation("Archived user cannot be blocked.")
        if (reason.isBlank()) throw DomainRuleViolation("Block reason cannot be blank.")

        return copy(
            status = UserStatus.BLOCKED,
            blockedAt = blockedAt,
            blockedReason = reason.trim(),
            updatedAt = blockedAt,
            version = version + 1,
        )
    }

    fun unblock(unblockedAt: Instant): User {
        if (status != UserStatus.BLOCKED) throw DomainRuleViolation("Only blocked users can be unblocked.")

        return copy(
            status = UserStatus.ACTIVE,
            blockedAt = null,
            blockedReason = null,
            updatedAt = unblockedAt,
            version = version + 1,
        )
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

        fun createOwner(
            id: String,
            email: String,
            displayName: String,
            now: Instant,
            phone: String? = null,
        ): User = User(
            id = id,
            email = email.trim().lowercase(),
            displayName = displayName.trim(),
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            status = UserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        )
    }
}
