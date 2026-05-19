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

class AdminEmissionPointsRoutesIntegrationTest {
    @Test
    fun `POST admin emission points creates emission point`() = testApplication {
        val fixture = fixture13A5()

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

        val response = client.post("/api/v1/admin/emission-points") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "branchId": "br_1",
                  "establishmentCode": "1",
                  "emissionPointCode": "2",
                  "displayName": "Caja principal",
                  "status": "active",
                  "reason": "Crear punto de emisión piloto"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ep_route_1"))
        assertTrue(body.contains("001"))
        assertTrue(body.contains("002"))
        assertTrue(body.contains("001-002"))
        assertTrue(body.contains("Caja principal"))
    }

    @Test
    fun `GET admin emission point returns detail`() = testApplication {
        val fixture = fixture13A5().also {
            it.repository.emissionPoints["ep_1"] = routeEmissionPoint13A5(id = "ep_1", displayName = "Caja 1")
        }

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

        val response = client.get("/api/v1/admin/emission-points/ep_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ep_1"))
        assertTrue(body.contains("Caja 1"))
    }

    @Test
    fun `PUT admin emission point updates display name`() = testApplication {
        val fixture = fixture13A5().also {
            it.repository.emissionPoints["ep_1"] = routeEmissionPoint13A5(id = "ep_1", displayName = "Caja vieja")
        }

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

        val response = client.put("/api/v1/admin/emission-points/ep_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "displayName": "Caja nueva",
                  "reason": "Actualizar nombre"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Caja nueva"))
    }

    @Test
    fun `POST admin emission points deactivate rejects actor without manage permission`() = testApplication {
        val fixture = fixture13A5(permissions = setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW)).also {
            it.repository.emissionPoints["ep_1"] = routeEmissionPoint13A5(id = "ep_1", status = "active")
        }

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

        val response = client.post("/api/v1/admin/emission-points/ep_1/deactivate") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody("""{ "reason": "No permitido" }""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun fixture13A5(
        permissions: Set<String> = setOf(
            PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW,
            PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE,
            PermissionCatalog.ORGANIZATION_VIEW,
        ),
    ): Fixture13A5 {
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
            id = "role_admin_emission_points_test",
            code = "org_admin_emission_points_test",
            organizationId = organization.id,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Admin emission points test",
            description = "Admin emission points test role",
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
            secret = "test-jwt-secret-for-hermes-admin-emission-points-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val repository = RouteFakeAdminEmissionPointRepository13A5(
            business = AdminBusinessProfile(
                id = organization.id,
                countryCode = "EC",
                taxId = "1790000000001",
                legalName = "Hermes Demo S.A.",
                commercialName = "Hermes Demo",
                status = "active",
                ownerUserId = user.id,
                createdAt = now,
                updatedAt = now,
            )
        ).also {
            it.branches["br_1"] = routeBranch13A5(id = "br_1", status = "active")
        }

        val adminBusinessModule = AdminBusinessModule(
            getBusinessUseCase = GetAdminBusinessUseCase(repository),
            getReadinessUseCase = GetAdminBusinessReadinessUseCase(repository, clock),
            listActivitiesUseCase = ListAdminActivitiesUseCase(repository),
            listBranchesUseCase = ListAdminBranchesUseCase(repository),
            listEmissionPointsUseCase = ListAdminEmissionPointsUseCase(repository),
            getEmissionPointUseCase = GetAdminEmissionPointUseCase(repository),
            createEmissionPointUseCase = CreateAdminEmissionPointUseCase(
                repository = repository,
                idGenerator = { "ep_route_1" },
                clock = clock,
            ),
            updateEmissionPointUseCase = UpdateAdminEmissionPointUseCase(
                repository = repository,
                clock = clock,
            ),
            changeEmissionPointStatusUseCase = ChangeAdminEmissionPointStatusUseCase(
                repository = repository,
                clock = clock,
            ),
        )

        return Fixture13A5(
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
            adminBusinessModule = adminBusinessModule,
            accessToken = accessToken,
            repository = repository,
        )
    }
}

private data class Fixture13A5(
    val authenticateRequestUseCase: AuthenticateRequestUseCase,
    val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    val adminBusinessModule: AdminBusinessModule,
    val accessToken: String,
    val repository: RouteFakeAdminEmissionPointRepository13A5,
)

private class RouteFakeAdminEmissionPointRepository13A5(
    private val business: AdminBusinessProfile,
) : AdminBusinessRepository, AdminEmissionPointMutationRepository {
    val branches: MutableMap<String, AdminBusinessBranchSummary> = linkedMapOf()
    val emissionPoints: MutableMap<String, AdminBusinessEmissionPointSummary> = linkedMapOf()

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = emptyList()
    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> =
        branches.values.filter { it.organizationId == organizationId }

    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> =
        emissionPoints.values.filter { it.organizationId == organizationId }

    override fun hasTaxSettings(organizationId: String): Boolean = true
    override fun hasSriSettings(organizationId: String): Boolean = true
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = true

    override fun findEmissionPoint(
        organizationId: String,
        emissionPointId: String
    ): AdminBusinessEmissionPointSummary? =
        emissionPoints[emissionPointId]?.takeIf { it.organizationId == organizationId }

    override fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary? =
        branches[branchId]?.takeIf { it.organizationId == organizationId }

    override fun existsEmissionPointCodes(
        organizationId: String,
        establishmentCode: String,
        emissionPointCode: String,
        excludeEmissionPointId: String?,
    ): Boolean = emissionPoints.values.any {
        it.organizationId == organizationId &&
                it.establishmentCode == establishmentCode &&
                it.emissionPointCode == emissionPointCode &&
                it.id != excludeEmissionPointId
    }

    override fun createEmissionPoint(draft: AdminEmissionPointCreateDraft): AdminBusinessEmissionPointSummary {
        val emissionPoint = AdminBusinessEmissionPointSummary(
            id = draft.id,
            organizationId = draft.organizationId,
            branchId = draft.branchId,
            establishmentCode = draft.establishmentCode,
            emissionPointCode = draft.emissionPointCode,
            displayName = draft.displayName,
            status = draft.status,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        emissionPoints[emissionPoint.id] = emissionPoint
        return emissionPoint
    }

    override fun updateEmissionPoint(patch: AdminEmissionPointUpdatePatch): AdminBusinessEmissionPointSummary {
        val current = findEmissionPoint(patch.organizationId, patch.emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        val updated = current.copy(
            branchId = patch.branchId ?: current.branchId,
            establishmentCode = patch.establishmentCode ?: current.establishmentCode,
            emissionPointCode = patch.emissionPointCode ?: current.emissionPointCode,
            displayName = patch.displayName ?: current.displayName,
            updatedAt = patch.updatedAt,
        )
        emissionPoints[updated.id] = updated
        return updated
    }

    override fun updateEmissionPointStatus(patch: AdminEmissionPointStatusPatch): AdminBusinessEmissionPointSummary {
        val current = findEmissionPoint(patch.organizationId, patch.emissionPointId)
            ?: throw DomainRuleViolation("Emission point does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        emissionPoints[updated.id] = updated
        return updated
    }
}

private fun routeBranch13A5(
    id: String,
    organizationId: String = "org_1",
    code: String = id,
    name: String = "Branch $id",
    status: String = "active",
): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = "branch",
    status = status,
)

private fun routeEmissionPoint13A5(
    id: String,
    organizationId: String = "org_1",
    branchId: String = "br_1",
    establishmentCode: String = "001",
    emissionPointCode: String = "001",
    displayName: String = "Caja 1",
    status: String = "active",
): AdminBusinessEmissionPointSummary = AdminBusinessEmissionPointSummary(
    id = id,
    organizationId = organizationId,
    branchId = branchId,
    establishmentCode = establishmentCode,
    emissionPointCode = emissionPointCode,
    displayName = displayName,
    status = status,
)
