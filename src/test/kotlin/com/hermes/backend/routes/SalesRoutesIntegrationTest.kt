package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.application.sales.salesFixture
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.backend.sales.SalesModule
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
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

class SalesRoutesIntegrationTest {
    @Test
    fun `POST GET and overlap validation for reservation routes`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                salesRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    salesModule = fixture.salesModule,
                )
            }
        }

        val createResponse = client.post("/organizations/org_1/reservations") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(reservationJson())
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertTrue(createResponse.bodyAsText().contains("\"id\":\"res_1\""))

        val getResponse = client.get("/organizations/org_1/reservations/res_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("\"resourceId\":\"quad_1\""))

        val overlappingResponse = client.post("/organizations/org_1/reservations") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(reservationJson())
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, overlappingResponse.status)
        assertTrue(overlappingResponse.bodyAsText().contains("Reservation slot is not available"))
    }

    private fun reservationJson(): String =
        """
        {
          "branchId": "br_1",
          "activityId": "act_tourism",
          "customerId": "cust_1",
          "customerSnapshot": {
            "customerId": "cust_1",
            "displayName": "Ana Cliente",
            "taxId": "1720000001",
            "taxIdType": "cedula",
            "email": "ana@example.com"
          },
          "resourceId": "quad_1",
          "startAt": "2026-05-18T11:00:00Z",
          "endAt": "2026-05-18T12:00:00Z",
          "partySize": 2,
          "notes": "Traer casco extra"
        }
        """.trimIndent()

    private fun fixture(): Fixture {
        val now = Instant.parse("2026-05-18T10:00:00Z")
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

        val salesFixture = salesFixture()
        val salesModule = SalesModule(
            createQuickSaleUseCase = salesFixture.createQuickSaleUseCase,
            addSaleItemUseCase = salesFixture.addSaleItemUseCase,
            getSaleUseCase = salesFixture.getSaleUseCase,
            searchSalesUseCase = salesFixture.searchSalesUseCase,
            changeSaleStatusUseCase = salesFixture.changeSaleStatusUseCase,
            changeSaleItemStatusUseCase = salesFixture.changeSaleItemStatusUseCase,
            cancelSaleUseCase = salesFixture.cancelSaleUseCase,
            closeSaleUseCase = salesFixture.closeSaleUseCase,
            createReservationUseCase = salesFixture.createReservationUseCase,
            getReservationUseCase = salesFixture.getReservationUseCase,
            searchReservationsUseCase = salesFixture.searchReservationsUseCase,
        )

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            salesModule = salesModule,
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val salesModule: SalesModule,
        val accessToken: String,
    )
}
