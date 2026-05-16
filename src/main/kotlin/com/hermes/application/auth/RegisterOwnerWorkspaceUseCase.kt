package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant

/**
 * Atomic onboarding use case for the first business owner.
 *
 * The repository should persist user, credential, organization and owner membership
 * in one database transaction whenever the backing store supports it.
 */
class RegisterOwnerWorkspaceUseCase(
    private val repository: OwnerWorkspaceRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val idGenerator: AuthIdGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RegisterOwnerWorkspaceCommand): RegisterOwnerWorkspaceResult {
        val now = Instant.now(clock)
        val email = EmailNormalizer.normalize(command.ownerEmail)
        val countryCode = command.organizationCountryCode.trim().uppercase()
        val taxId = command.organizationTaxId.trim()

        if (repository.emailExists(email)) {
            throw DomainRuleViolation("User email already exists.")
        }
        if (repository.organizationTaxIdExists(countryCode, taxId)) {
            throw DomainRuleViolation("Organization tax id already exists.")
        }

        val ownerRole = repository.findSystemRoleByCode(SystemRoleCode.ORGANIZATION_OWNER.code)
            ?: throw DomainRuleViolation("Organization owner role is not seeded.")

        passwordPolicy.assertValid(
            password = command.ownerPassword,
            email = email,
            displayName = command.ownerDisplayName,
        )

        val user = User.createOwner(
            id = idGenerator.newId("usr"),
            email = email,
            displayName = command.ownerDisplayName,
            phone = command.ownerPhone,
            now = now,
        )
        val credential = UserCredential.createPasswordCredential(
            id = idGenerator.newId("cred"),
            userId = user.id,
            passwordHash = passwordHasher.hash(command.ownerPassword.toCharArray()),
            now = now,
        )
        val organization = Organization.create(
            id = idGenerator.newId("org"),
            countryCode = countryCode,
            taxId = taxId,
            legalName = command.organizationLegalName,
            commercialName = command.organizationCommercialName,
            ownerUserId = user.id,
            now = now,
        )
        val membership = OrganizationMembership.owner(
            id = idGenerator.newId("mem"),
            organizationId = organization.id,
            userId = user.id,
            ownerRoleId = ownerRole.id,
            now = now,
        )

        repository.createOwnerWorkspace(
            user = user,
            credential = credential,
            organization = organization,
            membership = membership,
        )

        listOf(
            CredentialAuditAction.USER_CREATED,
            CredentialAuditAction.CREDENTIAL_CREATED,
            CredentialAuditAction.ORGANIZATION_CREATED,
            CredentialAuditAction.MEMBERSHIP_CREATED,
            CredentialAuditAction.ORGANIZATION_OWNER_ASSIGNED,
        ).forEach { action ->
            auditLogger.log(
                CredentialAuditEvent(
                    action = action,
                    actorUserId = user.id,
                    targetUserId = user.id,
                    organizationId = organization.id,
                    sessionId = null,
                    ipAddress = null,
                    userAgent = null,
                    createdAt = now,
                ),
            )
        }

        return RegisterOwnerWorkspaceResult(
            user = user,
            credential = credential,
            organization = organization,
            membership = membership,
        )
    }
}
