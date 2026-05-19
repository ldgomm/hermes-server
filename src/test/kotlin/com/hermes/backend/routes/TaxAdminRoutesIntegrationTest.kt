package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.application.tax.*
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.backend.tax.TaxModule
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.tax.EcuadorTaxSeed
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxRegimeCode
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

class TaxAdminRoutesIntegrationTest {
    @Test
    fun `POST and PATCH admin tax rate routes mutate tax configuration`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                taxAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    taxModule = fixture.taxModule,
                )
            }
        }

        val createResponse = client.post("/admin/tax-rates") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "ec_iva_test_2026",
                  "name": "IVA Test 8%",
                  "kind": "IVA",
                  "rate": "8.0000",
                  "status": "ACTIVE",
                  "sriTaxCode": "2",
                  "sriRateCode": "8",
                  "legalBasis": "Test basis",
                  "effectiveFrom": "2026-01-01T00:00:00Z",
                  "reason": "Crear tarifa de prueba"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val patchResponse = client.patch("/admin/tax-rates/taxr_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "IVA Test 8 actualizado",
                  "reason": "Actualizar nombre de prueba"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, patchResponse.status)
        assertTrue(fixture.audit.events.any { it.action.name == "TAX_RATE_CREATED" })
        assertTrue(fixture.audit.events.any { it.action.name == "TAX_RATE_UPDATED" })
    }

    @Test
    fun `POST admin tax profile route creates profile`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                taxAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    taxModule = fixture.taxModule,
                )
            }
        }

        val response = client.post("/admin/tax-profiles") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "code": "iva_test_special",
                  "name": "IVA especial test",
                  "treatment": "IVA_REDUCED_OR_SPECIAL",
                  "status": "ACTIVE",
                  "taxRateCode": "ec_iva_13_2026",
                  "legalBasis": "Test basis",
                  "effectiveFrom": "2026-01-01T00:00:00Z",
                  "reason": "Crear perfil de prueba"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(fixture.audit.events.any { it.action.name == "TAX_PROFILE_CREATED" })
    }

    @Test
    fun `PATCH organization tax settings and GET tax audit route are consultable`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                taxAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    taxModule = fixture.taxModule,
                )
            }
        }

        val patchResponse = client.patch("/organizations/org_1/tax-settings") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "allowTaxInclusivePrices": false,
                  "reason": "Política de precios sin IVA incluido"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, patchResponse.status)

        val auditResponse = client.get("/organizations/org_1/tax/audit?actions=ORGANIZATION_TAX_SETTINGS_UPDATED") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.OK, auditResponse.status)
    }

    private fun fixture(): Fixture {
        val now = Instant.parse("2026-05-17T12:00:00Z")
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

        val rateRepository = InMemoryTaxRateRepository()
        val profileRepository = InMemoryTaxProfileRepository()
        val settingsRepository = InMemoryOrganizationTaxSettingsRepository()
        val idGenerator = PredictableTaxIdGenerator()
        val audit = RecordingTaxAuditStore()

        EcuadorTaxSeed.rates.forEach(rateRepository::create)
        EcuadorTaxSeed.profiles.forEach(profileRepository::create)
        settingsRepository.create(
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
                    "no_tax_internal",
                ),
                allowTaxInclusivePrices = true,
                allowManualLineDiscounts = true,
                requireTaxProfileForCatalogItems = true,
                status = OrganizationTaxSettingsStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                createdBy = user.id,
                updatedBy = user.id,
            )
        )

        val taxModule = TaxModule(
            listActiveRatesUseCase = TaxListActiveRatesUseCase(rateRepository),
            getRateUseCase = TaxGetRateUseCase(rateRepository),
            createRateUseCase = TaxCreateRateUseCase(rateRepository, idGenerator, audit, clock),
            updateRateUseCase = TaxUpdateRateUseCase(rateRepository, audit, clock),
            listActiveProfilesUseCase = TaxListActiveProfilesUseCase(profileRepository),
            getProfileUseCase = TaxGetProfileUseCase(profileRepository),
            createProfileUseCase = TaxCreateProfileUseCase(
                profileRepository,
                rateRepository,
                idGenerator,
                audit,
                clock
            ),
            updateProfileUseCase = TaxUpdateProfileUseCase(profileRepository, rateRepository, audit, clock),
            getOrganizationSettingsUseCase = TaxGetOrganizationSettingsUseCase(settingsRepository),
            updateOrganizationSettingsUseCase = TaxUpdateOrganizationSettingsUseCase(
                settingsRepository,
                profileRepository,
                audit,
                clock
            ),
            calculatePreviewUseCase = TaxCalculatePreviewUseCase(profileRepository, settingsRepository, audit, clock),
            listAuditEventsUseCase = TaxListAuditEventsUseCase(audit, audit, clock),
        )

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            taxModule = taxModule,
            accessToken = accessToken,
            audit = audit,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val taxModule: TaxModule,
        val accessToken: String,
        val audit: RecordingTaxAuditStore,
    )
}
