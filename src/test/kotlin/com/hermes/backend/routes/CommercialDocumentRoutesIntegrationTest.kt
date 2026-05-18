package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.application.documents.*
import com.hermes.application.sales.InMemoryOperationalSaleRepository
import com.hermes.application.sales.confirmedSale
import com.hermes.backend.documents.CommercialDocumentsModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
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

class CommercialDocumentRoutesIntegrationTest {
    @Test
    fun `creates internal ticket lists downloads and emails document`() = testApplication {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                commercialDocumentRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    commercialDocumentsModule = fixture.documentsModule,
                )
            }
        }

        val createResponse = client.post("/organizations/$ORG/sales/$SALE/documents/internal-ticket") {
            auth(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "issuedAt": "$NOW",
                  "notes": "Documento interno de prueba"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        assertTrue(createBody.contains("\"id\":\"doc_1\""), createBody)
        assertTrue(createBody.contains("\"documentType\":\"internal_ticket\""), createBody)
        assertTrue(createBody.contains("\"status\":\"GENERATED\""), createBody)

        val listResponse = client.get("/organizations/$ORG/documents?saleId=$SALE&documentType=internal_ticket") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("\"documents\""))
        assertTrue(listResponse.bodyAsText().contains("doc_1"))

        val pdfResponse = client.get("/organizations/$ORG/documents/doc_1/pdf") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.OK, pdfResponse.status)
        assertTrue(pdfResponse.bodyAsText().startsWith("%PDF"))

        val emailResponse = client.post("/organizations/$ORG/documents/doc_1/email") {
            auth(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "emailTo": "cliente@example.com",
                  "subject": "Tu documento",
                  "message": "Adjunto documento"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, emailResponse.status)
        val emailBody = emailResponse.bodyAsText()
        assertTrue(emailBody.contains("\"delivered\":true"), emailBody)
        assertTrue(emailBody.contains("\"emailTo\":\"cliente@example.com\""), emailBody)
        assertEquals(1, fixture.emailSender.sent.size)
        assertTrue(fixture.audit.events.isNotEmpty())
    }

    @Test
    fun `registers physical sale note through route and rejects cross organization access`() = testApplication {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                commercialDocumentRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    commercialDocumentsModule = fixture.documentsModule,
                )
            }
        }

        val createResponse = client.post("/organizations/$ORG/sales/$SALE/documents/physical-sale-note") {
            auth(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "physicalDocumentNumber": "001-001-000000123",
                  "issuedAt": "$NOW",
                  "notes": "Nota física registrada"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createBody = createResponse.bodyAsText()
        assertTrue(createBody.contains("\"documentType\":\"physical_sale_note_registry\""), createBody)
        assertTrue(createBody.contains("001-001-000000123"), createBody)

        val wrongOrgResponse = client.get("/organizations/org_other/documents/doc_1") {
            auth(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, wrongOrgResponse.status)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.auth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header("X-Organization-Id", ORG)
    }

    private fun fixture(): Fixture {
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
        val role = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER)
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
            secret = "test-jwt-secret-for-hermes-auth-tests-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = NOW).token

        val sales = InMemoryOperationalSaleRepository()
        val documents = InMemoryCommercialDocumentRepositoryForTest()
        val storage = InMemoryCommercialDocumentFileStorage()
        val renderer = SimpleCommercialDocumentPdfRenderer()
        val idGenerator = DeterministicCommercialDocumentIdGenerator()
        val numberGenerator = SequentialCommercialDocumentNumberGenerator()
        val audit = RecordingCommercialDocumentAuditLogger()
        val emailSender = RecordingCommercialDocumentEmailSender()

        val module = CommercialDocumentsModule(
            generateInternalTicketUseCase = GenerateInternalTicketUseCase(
                saleRepository = sales,
                documentRepository = documents,
                numberGenerator = numberGenerator,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                auditLogger = audit,
                clock = clock,
            ),
            registerPhysicalSaleNoteUseCase = RegisterPhysicalSaleNoteUseCase(
                saleRepository = sales,
                documentRepository = documents,
                idGenerator = idGenerator,
                pdfRenderer = renderer,
                fileStorage = storage,
                auditLogger = audit,
                clock = clock,
            ),
            getCommercialDocumentUseCase = GetCommercialDocumentUseCase(
                documentRepository = documents,
                auditLogger = audit,
                clock = clock,
            ),
            searchCommercialDocumentsUseCase = SearchCommercialDocumentsUseCase(
                documentRepository = documents,
                auditLogger = audit,
                clock = clock,
            ),
            downloadCommercialDocumentPdfUseCase = DownloadCommercialDocumentPdfUseCase(
                documentRepository = documents,
                fileStorage = storage,
                auditLogger = audit,
                clock = clock,
            ),
            emailCommercialDocumentUseCase = EmailCommercialDocumentUseCase(
                documentRepository = documents,
                fileStorage = storage,
                emailSender = emailSender,
                auditLogger = audit,
                clock = clock,
            ),
        )

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            documentsModule = module,
            sales = sales,
            emailSender = emailSender,
            audit = audit,
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val documentsModule: CommercialDocumentsModule,
        val sales: InMemoryOperationalSaleRepository,
        val emailSender: RecordingCommercialDocumentEmailSender,
        val audit: RecordingCommercialDocumentAuditLogger,
        val accessToken: String,
    )

    private companion object {
        const val ORG = "org_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T12:00:00Z")
    }
}
