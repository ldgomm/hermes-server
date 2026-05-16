package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant

class ActiveOrganizationResolverUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-16T00:00:00Z")

    @Test
    fun `resolves requested organization when user has active membership`() {
        val repository = baseRepository()
        val useCase = ActiveOrganizationResolverUseCase(repository)

        val result = useCase.execute(
            ResolveActiveOrganizationCommand(
                userId = "usr_1",
                requestedOrganizationId = "org_1",
            ),
        )

        assertEquals("org_1", result?.organization?.id)
        assertEquals("mem_1", result?.membership?.id)
    }

    @Test
    fun `requires explicit organization when user has multiple active memberships`() {
        val repository = baseRepository()
        val secondOrganization = Organization.create(
            id = "org_2",
            countryCode = "EC",
            taxId = "1799999999002",
            legalName = "Second Org",
            commercialName = "Second",
            ownerUserId = "usr_1",
            now = now,
        )
        repository.organizations[secondOrganization.id] = secondOrganization
        repository.memberships["mem_2"] = OrganizationMembership.owner(
            id = "mem_2",
            organizationId = secondOrganization.id,
            userId = "usr_1",
            ownerRoleId = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER).id,
            now = now,
        )

        val useCase = ActiveOrganizationResolverUseCase(repository)

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(ResolveActiveOrganizationCommand(userId = "usr_1"))
        }
    }

    private fun baseRepository(): FakeAuthContextRepository {
        val repository = FakeAuthContextRepository()
        val user = User.createOwner("usr_1", "owner@example.com", "Owner", now)
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1799999999001",
            legalName = "Test Org",
            commercialName = "Test",
            ownerUserId = user.id,
            now = now,
        )
        val role = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER)
        val membership = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = organization.id,
            userId = user.id,
            ownerRoleId = role.id,
            now = now,
        )
        repository.users[user.id] = user
        repository.organizations[organization.id] = organization
        repository.roles[role.id] = role
        repository.memberships[membership.id] = membership
        return repository
    }
}
