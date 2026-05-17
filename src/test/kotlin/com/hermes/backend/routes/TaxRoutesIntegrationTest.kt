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
import com.hermes.domain.session.UserSession.Companion.create
import com.hermes.domain.tax.*
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

class TaxRoutesIntegrationTest {
    @Test
    fun `GET tax settings returns organization settings`() = testApplication {
        val now = Instant.parse("2026-05-17T12:00:00Z")
        val fixture = fixture(now)

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                taxRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    taxModule = fixture.taxModule,
                )
            }
        }

        val response = client.get("/organizations/org_1/tax-settings") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `POST tax calculate preview calculates totals in backend`() = testApplication {
        val now = Instant.parse("2026-05-17T12:00:00Z")
        val fixture = fixture(now)

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                taxRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    taxModule = fixture.taxModule,
                )
            }
        }

        val response = client.post("/organizations/org_1/tax/calculate-preview") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "occurredAt": "2026-05-17T12:00:00Z",
                  "lines": [
                    {
                      "lineId": "line_1",
                      "description": "Producto gravado",
                      "quantity": {
                        "value": "2",
                        "unitCode": "unit",
                        "allowsDecimal": false
                      },
                      "unitPrice": {
                        "amount": "1.00",
                        "currency": "USD"
                      },
                      "discount": {
                        "amount": "0.00",
                        "currency": "USD"
                      },
                      "taxProfileCode": "iva_current_full",
                      "priceTaxMode": "TAX_EXCLUSIVE"
                    }
                  ]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("grandTotal"), body)
        assertTrue(body.contains("2.26"), body)
    }

    private fun fixture(now: Instant): Fixture {
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

        val session = create(
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

        val authenticate = AuthenticateRequestUseCase(
            repository = authRepository,
            jwtTokenService = jwt,
            clock = clock,
        )

        val activeOrganization = ActiveOrganizationResolverUseCase(
            repository = authRepository,
        )

        val effectivePermissions = EffectivePermissionResolverUseCase(
            repository = authRepository,
        )

        val accessToken = jwt.issueAccessToken(
            userId = user.id,
            sessionId = session.id,
            issuedAt = now,
        ).token

        val rateRepository = InMemoryTaxRateRepository()
        val profileRepository = InMemoryTaxProfileRepository()
        val settingsRepository = InMemoryOrganizationTaxSettingsRepository()
        val idGenerator = PredictableTaxIdGenerator()

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
            ),
        )

        val taxModule = TaxModule(
            listActiveRatesUseCase = TaxListActiveRatesUseCase(
                rateRepository = rateRepository,
            ),
            getRateUseCase = TaxGetRateUseCase(
                rateRepository = rateRepository,
            ),
            createRateUseCase = TaxCreateRateUseCase(
                rateRepository = rateRepository,
                idGenerator = idGenerator,
                clock = clock,
            ),
            updateRateUseCase = TaxUpdateRateUseCase(
                rateRepository = rateRepository,
                clock = clock,
            ),

            listActiveProfilesUseCase = TaxListActiveProfilesUseCase(
                profileRepository = profileRepository,
            ),
            getProfileUseCase = TaxGetProfileUseCase(
                profileRepository = profileRepository,
            ),
            createProfileUseCase = TaxCreateProfileUseCase(
                profileRepository = profileRepository,
                rateRepository = rateRepository,
                idGenerator = idGenerator,
                clock = clock,
            ),
            updateProfileUseCase = TaxUpdateProfileUseCase(
                profileRepository = profileRepository,
                rateRepository = rateRepository,
                clock = clock,
            ),

            getOrganizationSettingsUseCase = TaxGetOrganizationSettingsUseCase(
                settingsRepository = settingsRepository,
            ),
            updateOrganizationSettingsUseCase = TaxUpdateOrganizationSettingsUseCase(
                settingsRepository = settingsRepository,
                profileRepository = profileRepository,
                clock = clock,
            ),

            calculatePreviewUseCase = TaxCalculatePreviewUseCase(
                profileRepository = profileRepository,
                settingsRepository = settingsRepository,
                clock = clock,
            ),
        )

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            taxModule = taxModule,
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val taxModule: TaxModule,
        val accessToken: String,
    )

    private class PredictableTaxIdGenerator : TaxIdGenerator {
        private var counter: Int = 0

        override fun newId(prefix: String): String {
            counter += 1
            return "${prefix.trim().lowercase()}_$counter"
        }
    }

    private class InMemoryTaxRateRepository : TaxRateRepository {
        private val rates = linkedMapOf<String, TaxRate>()

        override fun create(rate: TaxRate) {
            rates[rate.id] = rate
        }

        override fun update(rate: TaxRate) {
            rates[rate.id] = rate
        }

        override fun findById(id: String): TaxRate? =
            rates[id.trim()]

        override fun findByCode(code: String): TaxRate? =
            rates.values.firstOrNull { it.code == code.trim() }

        override fun findActive(): List<TaxRate> =
            rates.values.filter { it.status.name == "ACTIVE" }
    }

    private class InMemoryTaxProfileRepository : TaxProfileRepository {
        private val profiles = linkedMapOf<String, TaxProfile>()

        override fun create(profile: TaxProfile) {
            profiles[profile.id] = profile
        }

        override fun update(profile: TaxProfile) {
            profiles[profile.id] = profile
        }

        override fun findById(id: String): TaxProfile? =
            profiles[id.trim()]

        override fun findByCode(code: String): TaxProfile? =
            profiles.values.firstOrNull { it.code == code.trim() }

        override fun findActive(): List<TaxProfile> =
            profiles.values.filter { it.status.name == "ACTIVE" }
    }

    private class InMemoryOrganizationTaxSettingsRepository : OrganizationTaxSettingsRepository {
        private val settings = linkedMapOf<String, OrganizationTaxSettings>()

        override fun create(settings: OrganizationTaxSettings) {
            this.settings[settings.organizationId] = settings
        }

        override fun update(settings: OrganizationTaxSettings) {
            this.settings[settings.organizationId] = settings
        }

        override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings? =
            settings[organizationId.trim()]
    }
}