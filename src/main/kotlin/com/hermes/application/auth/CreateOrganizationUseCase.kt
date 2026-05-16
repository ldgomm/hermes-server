package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

class CreateOrganizationUseCase(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val idGenerator: AuthIdGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CreateOrganizationCommand): CreateOrganizationResult {
        val now = Instant.now(clock)
        val user = userRepository.findUserById(command.ownerUserId)
            ?: throw DomainRuleViolation("Owner user does not exist.")
        user.assertCanAuthenticate()

        val countryCode = command.countryCode.trim().uppercase()
        val taxId = command.taxId.trim()

        if (organizationRepository.existsByTaxId(countryCode, taxId)) {
            throw DomainRuleViolation("Organization tax id already exists.")
        }

        val organization = Organization.create(
            id = idGenerator.newId("org"),
            countryCode = countryCode,
            taxId = taxId,
            legalName = command.legalName,
            commercialName = command.commercialName,
            ownerUserId = user.id,
            now = now,
        )

        organizationRepository.create(organization)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.ORGANIZATION_CREATED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = organization.id,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )

        return CreateOrganizationResult(organization)
    }
}
