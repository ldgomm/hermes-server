package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant

class CreateTemporaryUserUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: MembershipMutationRepository,
    private val roleRepository: RoleQueryRepository,
    private val idGenerator: AuthIdGenerator,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val tokenGenerator: SecureTokenGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateTemporaryUserCommand): CreateTemporaryUserResult {
        val now = Instant.now(clock)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.CREDENTIALS_USERS_CREATE)

        organizationRepository.findOrganizationById(command.organizationId)
            ?: throw DomainRuleViolation("Organization does not exist.")

        val email = EmailNormalizer.normalize(command.email)
        if (command.roleIds.isEmpty()) throw DomainRuleViolation("Temporary user requires at least one role.")

        val roles = roleRepository.findRolesByIds(command.roleIds)
        if (roles.size != command.roleIds.size) {
            throw DomainRuleViolation("One or more roles do not exist.")
        }
        if (roles.any { it.isPlatformRole }) {
            throw DomainRuleViolation("Organization users cannot receive platform roles.")
        }

        val user = userRepository.findUserByEmail(email) ?: User.createOwner(
            id = idGenerator.newId("usr"),
            email = email,
            displayName = command.displayName,
            phone = command.phone,
            now = now,
        ).also(userRepository::create)

        if (membershipRepository.findByOrganizationIdAndUserId(command.organizationId, user.id) != null) {
            throw DomainRuleViolation("User already belongs to this organization.")
        }

        val temporaryPassword = command.temporaryPassword?.takeIf { it.isNotBlank() } ?: tokenGenerator.generate().take(24) + "aA1!"
        passwordPolicy.assertValid(temporaryPassword, email = email, displayName = command.displayName)

        val existingCredential = credentialRepository.findByUserId(user.id)
        val credential = if (existingCredential == null) {
            UserCredential.createPasswordCredential(
                id = idGenerator.newId("cred"),
                userId = user.id,
                passwordHash = passwordHasher.hash(temporaryPassword.toCharArray()),
                now = now,
                temporary = true,
            ).also(credentialRepository::create)
        } else {
            existingCredential.replacePassword(
                newPasswordHash = passwordHasher.hash(temporaryPassword.toCharArray()),
                changedAt = now,
            ).forcePasswordChange(now).also(credentialRepository::update)
        }

        val membership = OrganizationMembership.owner(
            id = idGenerator.newId("mem"),
            organizationId = command.organizationId,
            userId = user.id,
            ownerRoleId = command.roleIds.first(),
            now = now,
        ).assignRoles(command.roleIds, now)
        membershipRepository.create(membership)

        listOf(CredentialAuditAction.USER_CREATED_BY_ADMIN, CredentialAuditAction.TEMPORARY_PASSWORD_CREATED).forEach { action ->
            auditLogger.log(
                CredentialAuditEvent(
                    action = action,
                    actorUserId = command.actorUserId,
                    targetUserId = user.id,
                    organizationId = command.organizationId,
                    sessionId = null,
                    ipAddress = command.ipAddress,
                    userAgent = command.userAgent,
                    createdAt = now,
                )
            )
        }

        return CreateTemporaryUserResult(
            user = user,
            credential = credential,
            membership = membership,
            temporaryPassword = temporaryPassword,
        )
    }
}
