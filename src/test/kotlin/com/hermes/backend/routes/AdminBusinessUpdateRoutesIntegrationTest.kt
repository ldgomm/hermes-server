package com.hermes.backend.routes

import com.hermes.application.admin.business.*
import com.hermes.application.auth.*
import com.hermes.backend.admin.business.AdminBusinessModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.UserSession
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminBusinessUpdateRoutesIntegrationTest {
    @Test
    fun `PUT admin business updates active organization settings`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminBusinessRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    adminBusinessModule = fixture.adminBusinessModule,
                )
            }
        }

        val response = client.put("/api/v1/admin/business") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "legalName": "Hermes Demo Actualizada S.A.",
                  "commercialName": "Hermes Admin",
                  "defaultCurrency": "usd",
                  "timezone": "America/Guayaquil",
                  "reason": "Corrección de datos administrativos"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Hermes Demo Actualizada S.A."))
        assertTrue(body.contains("Hermes Admin"))
        assertTrue(body.contains("\"defaultCurrency\":\"USD\""))
    }

    @Test
    fun `PUT admin business rejects actor without update permission`() = testApplication {
        val fixture = fixture(permissions = setOf(PermissionCatalog.ORGANIZATION_VIEW))

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminBusinessRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    adminBusinessModule = fixture.adminBusinessModule,
                )
            }
        }

        val response = client.put("/api/v1/admin/business") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "commercialName": "No permitido",
                  "reason": "Intento sin permiso"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun fixture(
        permissions: Set<String> = setOf(PermissionCatalog.ALL),
    ): Fixture {
        val now = Instant.parse("2026-05-19T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val authRepository = FakeAuthContextRepository()
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
        val role = RoleDefinition(
            id = "role_admin_update_test",
            code = "admin_business_update_test",
            organizationId = organization.id,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Admin business update test",
            description = "Custom test role for Admin Business update routes",
            permissionKeys = permissions,
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        )
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
        authRepository.users[user.id] = user
        authRepository.organizations[organization.id] = organization
        authRepository.roles[role.id] = role
        authRepository.memberships[membership.id] = membership
        authRepository.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-admin-business-update-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val businessRepository = RouteFakeAdminBusinessMutationRepository().apply {
            business = AdminBusinessProfile(
                id = "org_1",
                countryCode = "EC",
                taxId = "1790000000001",
                legalName = "Hermes Demo S.A.",
                commercialName = "Hermes Demo",
                status = "active",
                ownerUserId = user.id,
                createdAt = now,
                updatedAt = now,
                version = 1,
            )
        }

        return Fixture(
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
            adminBusinessModule = AdminBusinessModule(
                getBusinessUseCase = GetAdminBusinessUseCase(businessRepository),
                getReadinessUseCase = GetAdminBusinessReadinessUseCase(businessRepository, clock),
                listActivitiesUseCase = ListAdminActivitiesUseCase(businessRepository),
                listBranchesUseCase = ListAdminBranchesUseCase(businessRepository),
                listEmissionPointsUseCase = ListAdminEmissionPointsUseCase(businessRepository),
                updateBusinessUseCase = UpdateAdminBusinessUseCase(
                    readRepository = businessRepository,
                    mutationRepository = businessRepository,
                    clock = clock,
                ),
            ),
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticateRequestUseCase: AuthenticateRequestUseCase,
        val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
        val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
        val adminBusinessModule: AdminBusinessModule,
        val accessToken: String,
    )
}

private class RouteFakeAdminBusinessMutationRepository : AdminBusinessRepository, AdminBusinessMutationRepository {
    var business: AdminBusinessProfile? = null

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business?.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = emptyList()
    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> = emptyList()
    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emptyList()
    override fun hasTaxSettings(organizationId: String): Boolean = false
    override fun hasSriSettings(organizationId: String): Boolean = false
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = false

    override fun existsBusinessWithTaxId(
        countryCode: String,
        taxId: String,
        excludeOrganizationId: String,
    ): Boolean = false

    override fun updateBusiness(patch: AdminBusinessUpdatePatch): AdminBusinessProfile {
        val current = business ?: throw DomainRuleViolation("Organization does not exist.")
        val updated = current.copy(
            countryCode = patch.countryCode ?: current.countryCode,
            taxId = patch.taxId ?: current.taxId,
            legalName = patch.legalName ?: current.legalName,
            commercialName = patch.commercialName ?: current.commercialName,
            defaultCurrency = patch.defaultCurrency ?: current.defaultCurrency,
            timezone = patch.timezone ?: current.timezone,
            updatedAt = patch.updatedAt,
            version = current.version + 1,
        )
        business = updated
        return updated
    }
}
