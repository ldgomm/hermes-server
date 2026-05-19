package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.backend.plugins.configureSerialization
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeRoutesIntegrationTest {
    @Test
    fun `GET me returns authenticated context`() = testApplication {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val fixture = authFixture(now)

        application {
            configureSerialization()
            configureMeRoutes(fixture.meUseCase)
        }

        val response = client.get("/me") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("usr_1"), body)
        assertTrue(body.contains("org_1"), body)
        assertTrue(body.contains(PermissionCatalog.SALES_VIEW), body)
    }

    private fun authFixture(now: Instant): Fixture {
        val repository = FakeAuthContextRepository()
        val user = User.createOwner(
            id = "usr_1",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790000000001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes Demo",
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
        val session = UserSession.create(
            id = "ses_1",
            userId = user.id,
            now = now,
            expiresAt = now.plusSeconds(3600),
        )
        repository.users[user.id] = user
        repository.organizations[organization.id] = organization
        repository.memberships[membership.id] = membership
        repository.roles[role.id] = role
        repository.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-auth-tests-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val authenticate = AuthenticateRequestUseCase(repository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(repository)
        val effectivePermissions = EffectivePermissionResolverUseCase(repository)
        val meUseCase = MeUseCase(
            repository = repository,
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
        )
        val token = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token
        return Fixture(meUseCase, token)
    }

    private data class Fixture(
        val meUseCase: MeUseCase,
        val accessToken: String,
    )
}
