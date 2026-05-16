package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertTrue
import java.time.Instant

class EffectivePermissionResolverUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-16T00:00:00Z")

    @Test
    fun `resolves effective permissions from active membership roles`() {
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

        val useCase = EffectivePermissionResolverUseCase(repository)
        val result = useCase.execute(
            ResolveEffectivePermissionsCommand(
                userId = user.id,
                organizationId = organization.id,
            ),
        )

        assertTrue(PermissionCatalog.ORGANIZATION_VIEW in result.permissions)
        assertTrue(PermissionCatalog.SALES_CREATE in result.permissions)
    }
}
