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

class AdminActivitiesRoutesIntegrationTest {
    @Test
    fun `POST admin activities creates activity`() = testApplication {
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

        val response = client.post("/api/v1/admin/activities") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "Turismo Principal",
                  "name": "Experiencias turísticas",
                  "description": "Reservas y actividades",
                  "activityType": "tourism",
                  "workflowMode": "reservation",
                  "requiresScheduling": true,
                  "tracksInventory": false,
                  "allowsReceivables": true,
                  "sortOrder": 20,
                  "reason": "Crear actividad de turismo"
                }
                """.trimIndent()
            )
        }

        val body = response.requireStatus(HttpStatusCode.Created)

        val created = fixture.repository.activities["act_route_1"]
            ?: error("Activity was not persisted. Body: $body")

        assertEquals("act_route_1", created.id)
        assertEquals("org_1", created.organizationId)
        assertEquals("turismo_principal", created.code)
        assertEquals("Experiencias turísticas", created.name)
        assertEquals("tourism", created.activityType)
        assertEquals("reservation", created.workflowMode)
        assertTrue(created.requiresScheduling)
        assertEquals(false, created.tracksInventory)
        assertTrue(created.allowsReceivables)
        assertEquals(20, created.sortOrder)
    }

    @Test
    fun `PUT admin activities updates activity`() = testApplication {
        val fixture = fixture().also {
            it.repository.activities["act_1"] = routeActivity(id = "act_1", code = "restaurante", name = "Restaurante")
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

        val response = client.put("/api/v1/admin/activities/act_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Restaurante principal",
                  "tracksInventory": true,
                  "reason": "Actualizar actividad"
                }
                """.trimIndent()
            )
        }

        val body = response.requireStatus(HttpStatusCode.OK)

        val updated = fixture.repository.activities["act_1"]
            ?: error("Activity was not persisted after update. Body: $body")

        assertEquals("Restaurante principal", updated.name)
        assertTrue(updated.tracksInventory)
    }

    @Test
    fun `POST admin activities rejects actor without create permission`() = testApplication {
        val fixture = fixture(permissions = setOf(PermissionCatalog.ACTIVITIES_VIEW))

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

        val response = client.post("/api/v1/admin/activities") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "retail",
                  "name": "Retail",
                  "activityType": "retail",
                  "workflowMode": "quick_sale",
                  "reason": "No permitido"
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
            id = "role_admin_activities_test",
            code = "org_admin_activities_test",
            organizationId = organization.id,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Admin activities test",
            description = "Admin activities test role",
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
            secret = "test-jwt-secret-for-hermes-admin-activities-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val repository = RouteFakeAdminActivityRepository(
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
            getActivityUseCase = GetAdminActivityUseCase(repository),
            createActivityUseCase = CreateAdminActivityUseCase(
                repository = repository,
                idGenerator = AdminBusinessIdGenerator { "act_route_1" },
                clock = clock,
            ),
            updateActivityUseCase = UpdateAdminActivityUseCase(repository, clock = clock),
            changeActivityStatusUseCase = ChangeAdminActivityStatusUseCase(repository, clock = clock),
        )

        return Fixture(
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
            adminBusinessModule = module,
            accessToken = accessToken,
            repository = repository,
        )
    }

    private data class Fixture(
        val authenticateRequestUseCase: AuthenticateRequestUseCase,
        val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
        val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
        val adminBusinessModule: AdminBusinessModule,
        val accessToken: String,
        val repository: RouteFakeAdminActivityRepository,
    )
}

private class RouteFakeAdminActivityRepository(
    private val business: AdminBusinessProfile,
) : AdminBusinessRepository, AdminActivityMutationRepository {
    val activities: MutableMap<String, AdminBusinessActivitySummary> = linkedMapOf()

    override fun findBusiness(organizationId: String): AdminBusinessProfile? =
        business.takeIf { it.id == organizationId }

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> =
        activities.values.filter { it.organizationId == organizationId && it.status != "archived" }

    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> = emptyList()
    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emptyList()
    override fun hasTaxSettings(organizationId: String): Boolean = false
    override fun hasSriSettings(organizationId: String): Boolean = false
    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean = true

    override fun findActivity(organizationId: String, activityId: String): AdminBusinessActivitySummary? =
        activities[activityId]?.takeIf { it.organizationId == organizationId }

    override fun existsActivityCode(organizationId: String, code: String, excludeActivityId: String?): Boolean =
        activities.values.any { it.organizationId == organizationId && it.code == code && it.id != excludeActivityId }

    override fun createActivity(draft: AdminActivityCreateDraft): AdminBusinessActivitySummary {
        val activity = AdminBusinessActivitySummary(
            id = draft.id,
            organizationId = draft.organizationId,
            code = draft.code,
            name = draft.name,
            description = draft.description,
            activityType = draft.activityType,
            workflowMode = draft.workflowMode,
            status = draft.status,
            requiresScheduling = draft.requiresScheduling,
            tracksInventory = draft.tracksInventory,
            allowsReceivables = draft.allowsReceivables,
            sortOrder = draft.sortOrder,
            createdAt = draft.createdAt,
            updatedAt = draft.createdAt,
        )
        activities[activity.id] = activity
        return activity
    }

    override fun updateActivity(patch: AdminActivityUpdatePatch): AdminBusinessActivitySummary {
        val current = findActivity(patch.organizationId, patch.activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        val updated = current.copy(
            code = patch.code ?: current.code,
            name = patch.name ?: current.name,
            description = if (patch.changeDescription) patch.description else current.description,
            activityType = patch.activityType ?: current.activityType,
            workflowMode = patch.workflowMode ?: current.workflowMode,
            requiresScheduling = patch.requiresScheduling ?: current.requiresScheduling,
            tracksInventory = patch.tracksInventory ?: current.tracksInventory,
            allowsReceivables = patch.allowsReceivables ?: current.allowsReceivables,
            sortOrder = patch.sortOrder ?: current.sortOrder,
            updatedAt = patch.updatedAt,
        )
        activities[updated.id] = updated
        return updated
    }

    override fun updateActivityStatus(patch: AdminActivityStatusPatch): AdminBusinessActivitySummary {
        val current = findActivity(patch.organizationId, patch.activityId)
            ?: throw DomainRuleViolation("Business activity does not exist.")
        val updated = current.copy(status = patch.status, updatedAt = patch.updatedAt)
        activities[updated.id] = updated
        return updated
    }
}

private fun routeActivity(
    id: String,
    organizationId: String = "org_1",
    code: String = "activity",
    name: String = "Activity",
): AdminBusinessActivitySummary = AdminBusinessActivitySummary(
    id = id,
    organizationId = organizationId,
    code = code,
    name = name,
    description = null,
    activityType = "restaurant",
    workflowMode = "order",
    status = "active",
    requiresScheduling = false,
    tracksInventory = false,
    allowsReceivables = true,
    sortOrder = 1,
)

private suspend fun HttpResponse.requireStatus(expected: HttpStatusCode): String {
    val body = bodyAsText()

    if (status != expected) {
        error("Expected status $expected but got $status. Body: $body")
    }

    return body
}