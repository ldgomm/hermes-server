package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.auth.FakeAuthContextRepository
import com.hermes.application.auth.HmacJwtTokenService
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueQueryRepository
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueSearchQuery
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesUseCase
import com.hermes.backend.electronicinvoicing.ElectronicInvoicingModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerationCommand
import com.hermes.domain.electronicinvoicing.SriAccessKeyGenerator
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriNumericCode
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ElectronicInvoiceRoutesIntegrationTest {
    @Test
    fun `lists and gets electronic invoices through api routes`() = testApplication {
        val fixture = fixture()
        fixture.issueRepository.records += routeIssueRecord(id = "edoc_1", organizationId = ORG, saleId = SALE, sequential = 1)
        fixture.issueRepository.records += routeIssueRecord(id = "edoc_2", organizationId = ORG, saleId = "sale_2", sequential = 2)
            .copy(status = ElectronicDocumentStatus.AUTHORIZED, authorizedAt = NOW.plusSeconds(20))
        fixture.issueRepository.records += routeIssueRecord(id = "edoc_other_org", organizationId = "org_other", saleId = SALE, sequential = 3)

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                electronicInvoiceRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    electronicInvoicingModule = fixture.electronicInvoicingModule,
                )
            }
        }

        val listResponse = client.get("/api/v1/electronic-invoices?environment=test&limit=50") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.OK, listResponse.status)
        val listBody = listResponse.bodyAsText()
        assertTrue(listBody.contains("\"documents\""), listBody)
        assertTrue(listBody.contains("edoc_1"), listBody)
        assertTrue(listBody.contains("edoc_2"), listBody)
        assertTrue(!listBody.contains("edoc_other_org"), listBody)
        assertTrue(!listBody.contains("unsignedXmlObjectKey"), listBody)

        val detailResponse = client.get("/api/v1/electronic-invoices/edoc_2") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.OK, detailResponse.status)
        val detailBody = detailResponse.bodyAsText()
        assertTrue(detailBody.contains("\"id\":\"edoc_2\""), detailBody)
        assertTrue(detailBody.contains("\"status\":\"AUTHORIZED\""), detailBody)
        assertTrue(detailBody.contains("\"artifacts\""), detailBody)
    }

    @Test
    fun `requires electronic invoice list permission`() = testApplication {
        val fixture = fixture(includeElectronicInvoicePermissions = false)

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                electronicInvoiceRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    electronicInvoicingModule = fixture.electronicInvoicingModule,
                )
            }
        }

        val response = client.get("/api/v1/electronic-invoices") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header("X-Organization-Id", ORG)
    }

    private fun fixture(includeElectronicInvoicePermissions: Boolean = true): Fixture {
        val authRepository = FakeAuthContextRepository()
        val user = User.createOwner(
            id = USER,
            email = "owner@hermes.local",
            displayName = "Owner",
            now = NOW,
        )
        val organization = Organization.create(
            id = ORG,
            countryCode = "EC",
            taxId = "1790000000001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes Demo",
            ownerUserId = user.id,
            now = NOW,
        )
        val baseRole = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER)
        val role = if (includeElectronicInvoicePermissions) {
            baseRole
        } else {
            baseRole.copy(
                id = "role_without_electronic_invoices",
                code = "without_electronic_invoices",
                permissionKeys = baseRole.permissionKeys.filterNot { it.startsWith("documents.electronic_invoice.") }.toSet(),
            )
        }
        val membership = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = organization.id,
            userId = user.id,
            ownerRoleId = role.id,
            now = NOW,
        )
        val session = UserSession.create(
            id = "ses_1",
            userId = user.id,
            now = NOW,
            expiresAt = NOW.plusSeconds(3600),
        )

        authRepository.users[user.id] = user
        authRepository.organizations[organization.id] = organization
        authRepository.memberships[membership.id] = membership
        authRepository.roles[role.id] = role
        authRepository.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-electronic-invoice-tests-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = NOW).token
        val issueRepository = RoutesElectronicInvoiceQueryRepository()

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            electronicInvoicingModule = ElectronicInvoicingModule(
                getElectronicInvoiceUseCase = GetElectronicInvoiceUseCase(issueRepository),
                listElectronicInvoicesUseCase = ListElectronicInvoicesUseCase(issueRepository),
            ),
            issueRepository = issueRepository,
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val electronicInvoicingModule: ElectronicInvoicingModule,
        val issueRepository: RoutesElectronicInvoiceQueryRepository,
        val accessToken: String,
    )

    private companion object {
        const val ORG = "org_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}

private class RoutesElectronicInvoiceQueryRepository : ElectronicInvoiceIssueQueryRepository {
    val records: MutableList<ElectronicInvoiceIssueRecord> = mutableListOf()

    override fun findById(organizationId: String, documentId: String): ElectronicInvoiceIssueRecord? =
        records.firstOrNull { it.organizationId == organizationId && it.id == documentId }

    override fun search(query: ElectronicInvoiceIssueSearchQuery): List<ElectronicInvoiceIssueRecord> = records
        .asSequence()
        .filter { it.organizationId == query.organizationId }
        .filter { query.saleId == null || it.saleId == query.saleId }
        .filter { query.statuses.isEmpty() || it.status in query.statuses }
        .filter { query.environment == null || it.environment == query.environment }
        .filter { query.from == null || !it.issuedAt.isBefore(query.from) }
        .filter { query.to == null || !it.issuedAt.isAfter(query.to) }
        .sortedByDescending { it.issuedAt }
        .take(query.limit)
        .toList()
}

private fun routeIssueRecord(
    id: String,
    organizationId: String,
    saleId: String,
    sequential: Int,
    issuedAt: Instant = Instant.parse("2026-05-18T12:00:00Z").plusSeconds(sequential.toLong()),
): ElectronicInvoiceIssueRecord {
    val series = SriSeries("001", "002")
    val accessKey = SriAccessKeyGenerator.generate(
        SriAccessKeyGenerationCommand(
            issuedDate = LocalDate.of(2026, 5, 18),
            documentType = SriDocumentType.INVOICE,
            ruc = "1790012345001",
            environment = SriEnvironment.TEST,
            series = series,
            sequential = SriSequential(sequential),
            numericCode = SriNumericCode("12345678"),
        )
    )

    return ElectronicInvoiceIssueRecord.accessKeyGenerated(
        id = id,
        organizationId = organizationId,
        branchId = "br_1",
        emissionPointId = "emi_1",
        saleId = saleId,
        environment = SriEnvironment.TEST,
        documentType = SriDocumentType.INVOICE,
        series = series,
        documentNumber = "001-002-${sequential.toString().padStart(9, '0')}",
        accessKey = accessKey,
        authorizationNumber = accessKey.value,
        issuedAt = issuedAt,
        actorUserId = "usr_1",
    ).copy(
        signedXmlObjectKey = "internal/object/key/not-exposed/$id.xml",
        signedXmlSha256 = "a".repeat(64),
    )
}
