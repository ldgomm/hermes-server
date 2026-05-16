package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MeUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-16T00:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val jwt = HmacJwtTokenService(
        secret = "12345678901234567890123456789012",
        accessTokenTtlSeconds = 900,
    )

    @Test
    fun `returns user session memberships active organization and permissions`() {
        val repository = FakeAuthContextRepository()
        val user = User.createOwner("usr_1", "owner@example.com", "Owner", now)
        val session = UserSession.create("ses_1", user.id, now, now.plusSeconds(3600))
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
        repository.sessions[session.id] = session
        repository.organizations[organization.id] = organization
        repository.roles[role.id] = role
        repository.memberships[membership.id] = membership

        val authenticate = AuthenticateRequestUseCase(repository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(repository)
        val permissions = EffectivePermissionResolverUseCase(repository)
        val useCase = MeUseCase(repository, authenticate, activeOrganization, permissions)
        val token = jwt.issueAccessToken(user.id, session.id, now).token

        val result = useCase.execute(
            GetMeCommand(
                accessToken = token,
                requestedOrganizationId = organization.id,
            ),
        )

        assertEquals(user.id, result.principal.user.id)
        assertEquals(session.id, result.principal.session.id)
        assertEquals(1, result.memberships.size)
        assertEquals(organization.id, result.activeOrganization?.organization?.id)
        assertTrue(PermissionCatalog.ORGANIZATION_VIEW in result.effectivePermissions!!.permissions)
    }
}
