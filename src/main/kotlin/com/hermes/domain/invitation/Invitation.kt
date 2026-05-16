package com.hermes.domain.invitation

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class Invitation(
    val id: String,
    val organizationId: String,
    val email: String,
    val invitedByUserId: String,
    val roleIds: Set<String>,
    val tokenHash: String,
    val status: InvitationStatus,
    val createdAt: Instant,
    val expiresAt: Instant,
    val acceptedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val acceptedUserId: String? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Invitation id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Invitation organization id cannot be blank.")
        if (email.isBlank()) throw DomainRuleViolation("Invitation email cannot be blank.")
        if (email != email.trim().lowercase()) throw DomainRuleViolation("Invitation email must be normalized.")
        if (invitedByUserId.isBlank()) throw DomainRuleViolation("Invitation invitedByUserId cannot be blank.")
        if (roleIds.isEmpty()) throw DomainRuleViolation("Invitation requires at least one role.")
        if (roleIds.any { it.isBlank() }) throw DomainRuleViolation("Invitation role ids cannot be blank.")
        if (tokenHash.isBlank()) throw DomainRuleViolation("Invitation token hash cannot be blank.")
        if (!expiresAt.isAfter(createdAt)) throw DomainRuleViolation("Invitation expiration must be after creation.")
        if (version < 1) throw DomainRuleViolation("Invitation version must be positive.")
        if (status == InvitationStatus.ACCEPTED && acceptedAt == null) {
            throw DomainRuleViolation("Accepted invitation requires acceptedAt.")
        }
        if (status == InvitationStatus.REVOKED && revokedAt == null) {
            throw DomainRuleViolation("Revoked invitation requires revokedAt.")
        }
    }

    fun assertAcceptable(now: Instant) {
        if (status != InvitationStatus.PENDING) {
            throw DomainRuleViolation("Only pending invitations can be accepted.")
        }
        if (!expiresAt.isAfter(now)) {
            throw DomainRuleViolation("Invitation has expired.")
        }
    }

    fun accept(now: Instant, userId: String): Invitation {
        assertAcceptable(now)
        if (userId.isBlank()) throw DomainRuleViolation("Invitation accepted user id cannot be blank.")
        return copy(
            status = InvitationStatus.ACCEPTED,
            acceptedAt = now,
            acceptedUserId = userId,
            version = version + 1,
        )
    }

    fun revoke(now: Instant): Invitation {
        if (status == InvitationStatus.REVOKED) throw DomainRuleViolation("Invitation is already revoked.")
        if (status == InvitationStatus.ACCEPTED) throw DomainRuleViolation("Accepted invitation cannot be revoked.")
        return copy(status = InvitationStatus.REVOKED, revokedAt = now, version = version + 1)
    }
}
