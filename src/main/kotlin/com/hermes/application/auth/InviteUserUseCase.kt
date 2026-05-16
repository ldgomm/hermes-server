package com.hermes.application.auth

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import com.hermes.domain.user.UserStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant

class InviteUserUseCase(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: MembershipMutationRepository,
    private val roleRepository: RoleQueryRepository,
    private val invitationRepository: InvitationRepository,
    private val idGenerator: AuthIdGenerator,
    private val tokenGenerator: SecureTokenGenerator,
    private val delivery: InvitationDelivery = NoopInvitationDelivery,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val invitationTtl: Duration = Duration.ofDays(7),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: InviteUserCommand): InviteUserResult {
        val now = Instant.now(clock)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_INVITE)

        if (command.organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (command.roleIds.isEmpty()) throw DomainRuleViolation("Invitation requires at least one role.")
        organizationRepository.findOrganizationById(command.organizationId)
            ?: throw DomainRuleViolation("Organization does not exist.")

        val email = EmailNormalizer.normalize(command.email)
        val roles = roleRepository.findRolesByIds(command.roleIds)
        if (roles.size != command.roleIds.size) {
            throw DomainRuleViolation("One or more roles do not exist.")
        }
        if (roles.any { it.isPlatformRole }) {
            throw DomainRuleViolation("Organization invitations cannot assign platform roles.")
        }

        invitationRepository.findPendingByOrganizationAndEmail(command.organizationId, email)?.let {
            throw DomainRuleViolation("A pending invitation already exists for this email.")
        }

        val user = userRepository.findUserByEmail(email) ?: User(
            id = idGenerator.newId("usr"),
            email = email,
            displayName = command.displayName.trim(),
            phone = null,
            status = UserStatus.INVITED,
            createdAt = now,
            updatedAt = now,
        ).also(userRepository::create)

        val membership = membershipRepository.findByOrganizationIdAndUserId(command.organizationId, user.id)
            ?: OrganizationMembership(
                id = idGenerator.newId("mem"),
                organizationId = command.organizationId,
                userId = user.id,
                roleIds = command.roleIds,
                status = MembershipStatus.PENDING_INVITATION,
                invitedBy = command.actorUserId,
                createdAt = now,
                updatedAt = now,
            ).also(membershipRepository::create)

        if (membership.status != MembershipStatus.PENDING_INVITATION) {
            throw DomainRuleViolation("User already has a non-pending membership in this organization.")
        }

        val rawToken = tokenGenerator.generate()
        val invitation = Invitation(
            id = idGenerator.newId("inv"),
            organizationId = command.organizationId,
            email = email,
            invitedByUserId = command.actorUserId,
            roleIds = command.roleIds,
            tokenHash = TokenHasher.sha256(rawToken),
            status = InvitationStatus.PENDING,
            createdAt = now,
            expiresAt = now.plus(invitationTtl),
        )
        invitationRepository.create(invitation)
        val invitationUrl = delivery.buildInvitationUrl(rawToken)
        delivery.deliverInvitation(email, rawToken, invitationUrl)

        audit(command, now, user.id, CredentialAuditAction.USER_INVITED)
        audit(command, now, user.id, CredentialAuditAction.INVITATION_CREATED)

        return InviteUserResult(
            invitation = invitation,
            user = user,
            membership = membership,
            rawInvitationToken = rawToken,
            invitationUrl = invitationUrl,
        )
    }

    private fun audit(command: InviteUserCommand, now: Instant, targetUserId: String, action: CredentialAuditAction) {
        auditLogger.log(
            CredentialAuditEvent(
                action = action,
                actorUserId = command.actorUserId,
                targetUserId = targetUserId,
                organizationId = command.organizationId,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            )
        )
    }
}
