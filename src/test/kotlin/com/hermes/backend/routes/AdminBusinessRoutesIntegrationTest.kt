package com.hermes.backend.routes

import com.hermes.application.admin.business.*
import com.hermes.application.auth.*
import com.hermes.backend.admin.business.AdminBusinessModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.*
import com.hermes.domain.session.UserSession
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

class AdminBusinessRoutesIntegrationTest {
    @Test
    fun `GET admin business readiness and lists return active organization data`() = testApplication {
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

        val readiness = client.get("/api/v1/admin/business/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val activities = client.get("/api/v1/admin/activities") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val branches = client.get("/api/v1/admin/branches") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val emissionPoints = client.get("/api/v1/admin/emission-points") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.OK, readiness.status)
        assertEquals(HttpStatusCode.OK, activities.status)
        assertEquals(HttpStatusCode.OK, branches.status)
        assertEquals(HttpStatusCode.OK, emissionPoints.status)
        assertTrue(readiness.bodyAsText().contains("\"overallStatus\":\"READY\""))
        assertTrue(activities.bodyAsText().contains("Restaurante"))
        assertTrue(branches.bodyAsText().contains("Sucursal principal"))
        assertTrue(emissionPoints.bodyAsText().contains("001-001"))
    }

    @Test
    fun `GET admin business rejects actor without organization view permission`() = testApplication {
        val fixture = fixture(permissions = setOf(PermissionCatalog.SALES_VIEW))

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

        val response = client.get("/api/v1/admin/business") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun fixture(
        permissions: Set<String> = setOf(
            PermissionCatalog.ORGANIZATION_VIEW,
            PermissionCatalog.ACTIVITIES_VIEW,
            PermissionCatalog.BRANCHES_VIEW,
            PermissionCatalog.SETTINGS_BRANCHES_VIEW,
            PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW,
        ),
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
            id = "role_admin_test",
            code = SystemRoleCode.ORGANIZATION_ADMIN.code,
            organizationId = null,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.SYSTEM,
            name = "Admin test",
            description = "Admin test role",
            permissionKeys = permissions,
            systemRole = true,
            critical = false,
            editable = false,
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
            secret = "test-jwt-secret-for-hermes-admin-business-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val businessRepository = RouteFakeAdminBusinessRepository().apply {
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
            )
            activities += AdminBusinessActivitySummary(
                id = "act_1",
                organizationId = "org_1",
                code = "restaurant",
                name = "Restaurante",
                activityType = "restaurant",
                workflowMode = "order",
                status = "active",
                requiresScheduling = false,
                tracksInventory = true,
                allowsReceivables = true,
            )
            branches += AdminBusinessBranchSummary(
                id = "br_1",
                organizationId = "org_1",
                code = "001",
                name = "Sucursal principal",
                type = "main",
                status = "active",
            )
            emissionPoints += AdminBusinessEmissionPointSummary(
                id = "ep_1",
                organizationId = "org_1",
                branchId = "br_1",
                establishmentCode = "001",
                emissionPointCode = "001",
                displayName = "Caja principal",
                status = "active",
            )
            taxSettings = true
            sriSettings = true
            activeOwnerOrAdmin = true
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

private class RouteFakeAdminBusinessRepository : AdminBusinessRepository {
    var business: AdminBusinessProfile? = null
    val activities: MutableList<AdminBusinessActivitySummary> = mutableListOf()
    val branches: MutableList<AdminBusinessBranchSummary> = mutableListOf()
    val emissionPoints: MutableList<AdminBusinessEmissionPointSummary> = mutableListOf()
    var taxSettings: Boolean = false
    var sriSettings: Boolean = false
    var activeOwnerOrAdmin: Boolean = false

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business?.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> =
        activities.filter { it.organizationId == organizationId }

    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> =
        branches.filter { it.organizationId == organizationId }

    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> =
        emissionPoints.filter { it.organizationId == organizationId }

    override fun hasTaxSettings(organizationId: String): Boolean = taxSettings
    override fun hasSriSettings(organizationId: String): Boolean = sriSettings
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = activeOwnerOrAdmin
}