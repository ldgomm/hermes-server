package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.UserStatus
import java.time.Clock
import java.time.Instant

class AcceptInvitationUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val membershipRepository: MembershipMutationRepository,
    private val invitationRepository: InvitationRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val idGenerator: AuthIdGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: AcceptInvitationCommand): AcceptInvitationResult {
        val now = Instant.now(clock)
        val tokenHash = TokenHasher.sha256(command.invitationToken)
        val invitation = invitationRepository.findInvitationByTokenHash(tokenHash)
            ?: throw DomainRuleViolation("Invitation token is invalid.")
        invitation.assertAcceptable(now)

        val user = userRepository.findUserByEmail(invitation.email)
            ?: throw DomainRuleViolation("Invited user does not exist.")
        val updatedUser = user.copy(
            displayName = command.displayName?.trim()?.takeIf { it.isNotBlank() } ?: user.displayName,
            phone = command.phone?.trim()?.takeIf { it.isNotBlank() } ?: user.phone,
            status = UserStatus.ACTIVE,
            updatedAt = now,
            version = user.version + 1,
        )

        passwordPolicy.assertValid(
            password = command.password,
            email = updatedUser.email,
            displayName = updatedUser.displayName,
        )

        val existingCredential = credentialRepository.findByUserId(user.id)
        val credential = if (existingCredential == null) {
            UserCredential.createPasswordCredential(
                id = idGenerator.newId("cred"),
                userId = user.id,
                passwordHash = passwordHasher.hash(command.password.toCharArray()),
                now = now,
            ).also(credentialRepository::create)
        } else {
            existingCredential.replacePassword(
                newPasswordHash = passwordHasher.hash(command.password.toCharArray()),
                changedAt = now,
            ).also(credentialRepository::update)
        }

        val membership = membershipRepository.findByOrganizationIdAndUserId(invitation.organizationId, user.id)
            ?: throw DomainRuleViolation("Invitation membership does not exist.")
        if (membership.status != MembershipStatus.PENDING_INVITATION) {
            throw DomainRuleViolation("Invitation membership is not pending.")
        }

        val activatedMembership = membership.copy(
            status = MembershipStatus.ACTIVE,
            acceptedAt = now,
            updatedAt = now,
            version = membership.version + 1,
        )

        userRepository.update(updatedUser)
        membershipRepository.update(activatedMembership)

        val acceptedInvitation = invitation.accept(now = now, userId = user.id)
        invitationRepository.update(acceptedInvitation)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.INVITATION_ACCEPTED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = invitation.organizationId,
                sessionId = null,
                ipAddress = command.ipAddress,
                userAgent = command.userAgent,
                createdAt = now,
            )
        )

        return AcceptInvitationResult(
            user = updatedUser,
            credential = credential,
            membership = activatedMembership,
            invitation = acceptedInvitation,
        )
    }
}
