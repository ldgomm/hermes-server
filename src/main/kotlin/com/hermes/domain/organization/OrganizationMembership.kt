package com.hermes.domain.organization

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class OrganizationMembership(
    val id: String,
    val organizationId: String,
    val userId: String,
    val roleIds: Set<String>,
    val status: MembershipStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val invitedBy: String? = null,
    val acceptedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Membership id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Membership organization id cannot be blank.")
        if (userId.isBlank()) throw DomainRuleViolation("Membership user id cannot be blank.")
        if (roleIds.isEmpty()) throw DomainRuleViolation("Membership requires at least one role.")
        if (roleIds.any { it.isBlank() }) throw DomainRuleViolation("Membership role ids cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Membership version must be greater than zero.")
        if (status == MembershipStatus.PENDING_INVITATION && invitedBy.isNullOrBlank()) {
            throw DomainRuleViolation("Pending membership requires invitedBy.")
        }
        if (status == MembershipStatus.REVOKED && revokedAt == null) {
            throw DomainRuleViolation("Revoked membership requires revokedAt.")
        }
    }

    fun assertCanAccessOrganization() {
        if (status != MembershipStatus.ACTIVE) {
            throw DomainRuleViolation("Only active memberships can access an organization.")
        }
    }

    fun assignRoles(roleIds: Set<String>, updatedAt: Instant): OrganizationMembership {
        if (roleIds.isEmpty()) throw DomainRuleViolation("Membership requires at least one role.")
        if (roleIds.any { it.isBlank() }) throw DomainRuleViolation("Membership role ids cannot be blank.")

        return copy(
            roleIds = roleIds,
            updatedAt = updatedAt,
            version = version + 1,
        )
    }

    companion object {
        fun owner(
            id: String,
            organizationId: String,
            userId: String,
            ownerRoleId: String,
            now: Instant,
        ): OrganizationMembership = OrganizationMembership(
            id = id,
            organizationId = organizationId,
            userId = userId,
            roleIds = setOf(ownerRoleId),
            status = MembershipStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            acceptedAt = now,
        )
    }
}
