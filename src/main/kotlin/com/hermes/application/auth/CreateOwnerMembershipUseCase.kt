package com.hermes.application.auth

import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CreateOwnerMembershipUseCase(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val roleLookupRepository: AuthRoleLookupRepository,
    private val idGenerator: AuthIdGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateOwnerMembershipCommand): CreateOwnerMembershipResult {
        val now = Instant.now(clock)
        val user = userRepository.findUserById(command.userId)
            ?: throw DomainRuleViolation("Owner user does not exist.")
        val organization = organizationRepository.findOrganizationById(command.organizationId)
            ?: throw DomainRuleViolation("Organization does not exist.")

        if (organization.ownerUserId != user.id) {
            throw DomainRuleViolation("Only the organization owner user can receive the initial owner membership.")
        }

        if (membershipRepository.existsByOrganizationIdAndUserId(organization.id, user.id)) {
            throw DomainRuleViolation("User already has a membership in this organization.")
        }

        val ownerRole = roleLookupRepository.findSystemRoleByCode(SystemRoleCode.ORGANIZATION_OWNER.code)
            ?: throw DomainRuleViolation("Organization owner role is not seeded.")

        val membership = OrganizationMembership.owner(
            id = idGenerator.newId("mem"),
            organizationId = organization.id,
            userId = user.id,
            ownerRoleId = ownerRole.id,
            now = now,
        )

        membershipRepository.create(membership)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.MEMBERSHIP_CREATED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = organization.id,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )
        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.ORGANIZATION_OWNER_ASSIGNED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = organization.id,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )

        return CreateOwnerMembershipResult(membership)
    }
}
