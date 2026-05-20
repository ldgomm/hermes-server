package com.hermes.backend.routes

import com.hermes.application.admin.tax.*
import com.hermes.application.auth.*
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.catalog.CatalogTaxProfileAssignmentRecord
import com.hermes.application.catalog.OrganizationCatalogTaxProfileRepository
import com.hermes.application.tax.*
import com.hermes.backend.admin.tax.AdminTaxModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.tax.*
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminTaxRoutesIntegrationTest {
    @Test
    fun `GET admin tax readiness returns mobile friendly response`() = testApplication {
        val fixture = fixture()
        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminTaxRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    adminTaxModule = fixture.adminTaxModule,
                )
            }
        }

        val response = client.get("/api/v1/admin/tax/readiness") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST and PUT admin tax rate routes mutate tax configuration`() = testApplication {
        val fixture = fixture()
        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminTaxRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    adminTaxModule = fixture.adminTaxModule,
                )
            }
        }

        val createResponse = client.post("/api/v1/admin/tax/rates") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "ec_iva_test_admin_2026",
                  "name": "IVA Test Admin",
                  "kind": "IVA",
                  "rate": "8.0000",
                  "status": "ACTIVE",
                  "sriTaxCode": "2",
                  "sriRateCode": "8",
                  "legalBasis": "Configuración de prueba",
                  "effectiveFrom": "2026-01-01T00:00:00Z",
                  "reason": "Crear tarifa admin de prueba"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        val updateResponse = client.put("/api/v1/admin/tax/rates/taxr_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "IVA Test Admin actualizado",
                  "reason": "Actualizar tarifa admin de prueba"
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(fixture.audit.events.any { it.action.name == "TAX_RATE_CREATED" })
        assertTrue(fixture.audit.events.any { it.action.name == "TAX_RATE_UPDATED" })
    }

    @Test
    fun `POST catalog tax profile assignment delegates to catalog tax use case`() = testApplication {
        val fixture = fixture()
        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminTaxRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    adminTaxModule = fixture.adminTaxModule,
                )
            }
        }

        val response = client.post("/api/v1/admin/catalog/local/items/item_1/tax-profile") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "taxProfileCode": "iva_0",
                  "reason": "Corregir perfil tributario del item"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("taxp_ec_iva_0", fixture.catalogAssignments.assignedProfileByItem.getValue("item_1"))
    }

    private fun fixture(): Fixture {
        val now = Instant.parse("2026-05-20T12:00:00Z")
        val authRepository = FakeAuthContextRepository()
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
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
        val membership = OrganizationMembership.owner("mem_1", organization.id, user.id, role.id, now)
        val session = UserSession.create("ses_1", user.id, now, now.plusSeconds(3600))

        authRepository.users[user.id] = user
        authRepository.organizations[organization.id] = organization
        authRepository.memberships[membership.id] = membership
        authRepository.roles[role.id] = role
        authRepository.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-auth-tests-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val rates = InMemoryTaxRateRepository()
        val profiles = InMemoryTaxProfileRepository()
        val settings = InMemoryOrganizationTaxSettingsRepository()
        val audit = RecordingTaxAuditStore()
        EcuadorTaxSeed.rates.forEach(rates::create)
        EcuadorTaxSeed.profiles.forEach(profiles::create)
        settings.create(
            OrganizationTaxSettings(
                id = "taxset_org_1",
                organizationId = "org_1",
                regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
                defaultTaxProfileCode = "iva_current_full",
                enabledTaxProfileCodes = setOf(
                    "iva_current_full",
                    "iva_0",
                    "exempt_iva",
                    "not_subject_to_iva",
                    "no_tax_internal"
                ),
                allowTaxInclusivePrices = true,
                allowManualLineDiscounts = true,
                requireTaxProfileForCatalogItems = true,
                status = OrganizationTaxSettingsStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                createdBy = user.id,
                updatedBy = user.id,
            ),
        )
        val catalogAssignments = InMemoryOrganizationCatalogTaxProfileRepository()

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            adminTaxModule = AdminTaxModule(
                searchRatesUseCase = SearchAdminTaxRatesUseCase(InMemoryAdminTaxRateQueryRepository(rates)),
                getRateUseCase = TaxGetRateUseCase(rates),
                createRateUseCase = TaxCreateRateUseCase(rates, PredictableTaxIdGenerator(), audit, clock),
                updateRateUseCase = TaxUpdateRateUseCase(rates, audit, clock),
                searchProfilesUseCase = SearchAdminTaxProfilesUseCase(InMemoryAdminTaxProfileQueryRepository(profiles)),
                getProfileUseCase = TaxGetProfileUseCase(profiles),
                createProfileUseCase = TaxCreateProfileUseCase(
                    profiles,
                    rates,
                    PredictableTaxIdGenerator(),
                    audit,
                    clock
                ),
                updateProfileUseCase = TaxUpdateProfileUseCase(profiles, rates, audit, clock),
                assignTaxProfileToCatalogItemUseCase = AssignTaxProfileToCatalogItemUseCase(
                    catalogRepository = catalogAssignments,
                    profileRepository = profiles,
                    settingsRepository = settings,
                    clock = clock,
                ),
                readinessUseCase = GetAdminTaxReadinessUseCase(settings, profiles, clock),
            ),
            accessToken = accessToken,
            audit = audit,
            catalogAssignments = catalogAssignments,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val adminTaxModule: AdminTaxModule,
        val accessToken: String,
        val audit: RecordingTaxAuditStore,
        val catalogAssignments: InMemoryOrganizationCatalogTaxProfileRepository,
    )

    private class InMemoryAdminTaxRateQueryRepository(
        private val delegate: InMemoryTaxRateRepository,
    ) : AdminTaxRateQueryRepository {
        override fun search(query: AdminTaxRateSearchQuery): List<TaxRate> =
            delegate.findActive()
                .filter { query.kind == null || it.kind == query.kind }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { query.effectiveAt == null || it.isEffectiveAt(query.effectiveAt) }
                .take(query.limit)
    }

    private class InMemoryAdminTaxProfileQueryRepository(
        private val delegate: InMemoryTaxProfileRepository,
    ) : AdminTaxProfileQueryRepository {
        override fun search(query: AdminTaxProfileSearchQuery): List<TaxProfile> =
            delegate.findActive()
                .filter { query.treatment == null || it.treatment == query.treatment }
                .filter { query.statuses.isEmpty() || it.status in query.statuses }
                .filter { query.effectiveAt == null || it.isEffectiveAt(query.effectiveAt) }
                .take(query.limit)
    }

    private class InMemoryOrganizationCatalogTaxProfileRepository : OrganizationCatalogTaxProfileRepository {
        val assignedProfileByItem: MutableMap<String, String> = mutableMapOf("item_1" to "prof_current")

        override fun assignTaxProfile(
            organizationId: String,
            catalogItemId: String,
            taxProfileId: String,
            updatedAt: Instant,
        ): CatalogTaxProfileAssignmentRecord {
            val previous = assignedProfileByItem[catalogItemId]
            assignedProfileByItem[catalogItemId] = taxProfileId
            return CatalogTaxProfileAssignmentRecord(
                organizationId = organizationId,
                catalogItemId = catalogItemId,
                previousTaxProfileId = previous,
                taxProfileId = taxProfileId,
                updatedAt = updatedAt,
            )
        }
    }
}
