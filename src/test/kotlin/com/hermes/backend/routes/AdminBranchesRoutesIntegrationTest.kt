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

class AdminBranchesRoutesIntegrationTest {
    @Test
    fun `POST admin branches creates branch`() = testApplication {
        val fixture = fixture13A4()

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

        val response = client.post("/api/v1/admin/branches") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "Principal 01",
                  "name": "Sucursal principal",
                  "type": "main",
                  "status": "active",
                  "location": {
                    "countryCode": "EC",
                    "province": "Pichincha",
                    "city": "Mejía",
                    "sector": "Tambillo",
                    "addressLine": "El Murco",
                    "latitude": -0.40,
                    "longitude": -78.55,
                    "privacyMode": "approximate_public"
                  },
                  "businessHoursId": "hours_main",
                  "reason": "Crear sucursal piloto"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        println("STATUS=${response.status}")
        println("BODY=$body")
        assertTrue(body.contains("br_route_1"))
        assertTrue(body.contains("principal_01"))
        assertTrue(body.contains("Sucursal principal"))
        assertTrue(body.contains("approximate_public"))
    }

    @Test
    fun `PUT admin branches updates branch`() = testApplication {
        val fixture = fixture13A4().also {
            it.repository.branches["br_1"] = routeBranch13A4(id = "br_1", code = "principal", name = "Principal")
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

        val response = client.put("/api/v1/admin/branches/br_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Sucursal principal actualizada",
                  "businessHoursId": "hours_new",
                  "reason": "Actualizar sucursal"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Sucursal principal actualizada"))
        assertTrue(body.contains("hours_new"))
    }

    @Test
    fun `POST admin branches deactivate rejects actor without update permission`() = testApplication {
        val fixture = fixture13A4(permissions = setOf(PermissionCatalog.BRANCHES_VIEW)).also {
            it.repository.branches["br_1"] = routeBranch13A4(id = "br_1", code = "principal", name = "Principal")
            it.repository.branches["br_2"] = routeBranch13A4(id = "br_2", code = "norte", name = "Norte")
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

        val response = client.post("/api/v1/admin/branches/br_1/deactivate") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody("""{ "reason": "No permitido" }""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun fixture13A4(
        permissions: Set<String> = setOf(
            PermissionCatalog.BRANCHES_VIEW,
            PermissionCatalog.BRANCHES_CREATE,
            PermissionCatalog.BRANCHES_UPDATE,
            PermissionCatalog.SETTINGS_BRANCHES_VIEW,
            PermissionCatalog.SETTINGS_BRANCHES_MANAGE,
            PermissionCatalog.ORGANIZATION_VIEW,
        ),
    ): Fixture13A4 {
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
            id = "role_admin_branches_test",
            code = "org_admin_branches_test",
            organizationId = organization.id,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Admin branches test",
            description = "Admin branches test role",
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
            secret = "test-jwt-secret-for-hermes-admin-branches-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val repository = RouteFakeAdminBranchRepository13A4(
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
        )

        val module = AdminBusinessModule(
            getBusinessUseCase = GetAdminBusinessUseCase(repository),
            getReadinessUseCase = GetAdminBusinessReadinessUseCase(repository, clock),
            listActivitiesUseCase = ListAdminActivitiesUseCase(repository),
            listBranchesUseCase = ListAdminBranchesUseCase(repository),
            listEmissionPointsUseCase = ListAdminEmissionPointsUseCase(repository),
            getBranchUseCase = GetAdminBranchUseCase(repository),
            createBranchUseCase = CreateAdminBranchUseCase(
                repository = repository,
                idGenerator = AdminBusinessIdGenerator { "br_route_1" },
                clock = clock,
            ),
            updateBranchUseCase = UpdateAdminBranchUseCase(repository, clock = clock),
            changeBranchStatusUseCase = ChangeAdminBranchStatusUseCase(repository, clock = clock),
        )

        return Fixture13A4(
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
            adminBusinessModule = module,
            accessToken = accessToken,
            repository = repository,
        )
    }

    private data class Fixture13A4(
        val authenticateRequestUseCase: AuthenticateRequestUseCase,
        val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
        val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
        val adminBusinessModule: AdminBusinessModule,
        val accessToken: String,
        val repository: RouteFakeAdminBranchRepository13A4,
    )
}

private class RouteFakeAdminBranchRepository13A4(
    private val business: AdminBusinessProfile,
) : AdminBusinessRepository, AdminBranchMutationRepository {
    val branches: MutableMap<String, AdminBusinessBranchSummary> = linkedMapOf()
    val activeEmissionPointBranchIds: MutableSet<String> = mutableSetOf()

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = emptyList()
    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> =
        branches.values.filter { it.organizationId == organizationId && it.status != "archived" }

    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emptyList()
    override fun hasTaxSettings(organizationId: String): Boolean = false
    override fun hasSriSettings(organizationId: String): Boolean = false
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = true

    override fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary? =
        branches[branchId]?.takeIf { it.organizationId == organizationId }

    override fun existsBranchCode(organizationId: String, code: String, excludeBranchId: String?): Boolean =
        branches.values.any { it.organizationId == organizationId && it.code == code && it.id != excludeBranchId }

    override fun hasActiveMainBranch(organizationId: String, excludeBranchId: String?): Boolean =
        branches.values.any {
            it.organizationId == organizationId && it.type == "main" && it.status == "active" && it.id != excludeBranchId
        }

    override fun countActiveBranches(organizationId: String, excludeBranchId: String?): Int =
        branches.values.count { it.organizationId == organizationId && it.status == "active" && it.id != excludeBranchId }

    override fun hasActiveEmissionPoints(organizationId: String, branchId: String): Boolean =
        branchId in activeEmissionPointBranchIds

    override fun createBranch(draft: AdminBranchCreateDraft): AdminBusinessBranchSummary {
        val branch = AdminBusinessBranchSummary(
            id = draft.id,
            organizationId = draft.organizationId,
            code = draft.code,
            name = draft.name,
            type = draft.type,
            status = draft.status,
            location = draft.location,
            businessHoursId = draft.businessHoursId,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        branches[branch.id] = branch
        return branch
    }

    override fun updateBranch(patch: AdminBranchUpdatePatch): AdminBusinessBranchSummary {
        val current = findBranch(patch.organizationId, patch.branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        val updated = current.copy(
            code = patch.code ?: current.code,
            name = patch.name ?: current.name,
            type = patch.type ?: current.type,
            location = if (patch.changeLocation) patch.location else current.location,
            businessHoursId = if (patch.changeBusinessHoursId) patch.businessHoursId else current.businessHoursId,
            updatedAt = patch.updatedAt,
        )
        branches[updated.id] = updated
        return updated
    }

    override fun updateBranchStatus(patch: AdminBranchStatusPatch): AdminBusinessBranchSummary {
        val current = findBranch(patch.organizationId, patch.branchId)
            ?: throw DomainRuleViolation("Branch does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        branches[updated.id] = updated
        return updated
    }
}

private fun routeBranch13A4(
    id: String,
    organizationId: String = "org_1",
    code: String = "branch",
    name: String = "Branch",
): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    type = "branch",
    status = "active",
    location = AdminBranchLocation(countryCode = "EC"),
    businessHoursId = null,
)
