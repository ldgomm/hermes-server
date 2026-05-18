package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.application.payments.*
import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.backend.payments.PaymentsModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashSession
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.sale.*
import com.hermes.domain.session.UserSession
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import com.hermes.domain.user.User
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaymentsRoutesIntegrationTest {
    @Test
    fun `cash receivable and close cash session flow works through Ktor routes`() = testApplication {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                paymentsRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    paymentsModule = fixture.paymentsModule,
                )
            }
        }

        val openResponse = client.post("/organizations/$ORG/cash/sessions") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", ORG)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "branchId": "$BRANCH",
                  "openingBalance": { "amount": "20.00", "currency": "USD" },
                  "openedAt": "${NOW}",
                  "notes": "Apertura de prueba"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, openResponse.status)
        val openBody = openResponse.body<String>()
        assertTrue(openBody.contains("\"id\":\"cash_1\""), openBody)
        assertTrue(openBody.contains("\"status\":\"OPEN\""), openBody)

        val paymentResponse = client.post("/organizations/$ORG/sales/$SALE/payments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", ORG)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "amount": { "amount": "10.00", "currency": "USD" },
                  "method": "CASH",
                  "paidAt": "${NOW.plusSeconds(60)}",
                  "notes": "Abono inicial",
                  "markRemainingAsReceivable": true,
                  "receivableDueAt": "${NOW.plusSeconds(86400)}"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, paymentResponse.status)
        val paymentBody = paymentResponse.body<String>()
        assertTrue(paymentBody.contains("\"id\":\"pay_1\""), paymentBody)
        assertTrue(paymentBody.contains("\"salePaymentStatus\":\"PARTIALLY_PAID\""), paymentBody)
        assertTrue(paymentBody.contains("\"receivable\""), paymentBody)
        assertTrue(paymentBody.contains("\"id\":\"recv_1\""), paymentBody)

        val collectionResponse = client.post("/organizations/$ORG/receivables/recv_1/collections") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", ORG)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "saleId": "$SALE",
                  "amount": { "amount": "14.00", "currency": "USD" },
                  "method": "CASH",
                  "collectedAt": "${NOW.plusSeconds(120)}",
                  "notes": "Saldo cobrado"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, collectionResponse.status)
        val collectionBody = collectionResponse.body<String>()
        assertTrue(collectionBody.contains("\"id\":\"recv_1\""), collectionBody)
        assertTrue(collectionBody.contains("\"balanceDue\":{\"amount\":\"0.00\""), collectionBody)

        val closeResponse = client.post("/organizations/$ORG/cash/sessions/cash_1/close") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", ORG)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "countedCashAmount": { "amount": "44.00", "currency": "USD" },
                  "reason": "Cierre normal",
                  "closedAt": "${NOW.plusSeconds(180)}",
                  "notes": "Sin diferencias"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, closeResponse.status)
        val closeBody = closeResponse.body<String>()
        assertTrue(closeBody.contains("\"status\":\"CLOSED\""), closeBody)
        assertTrue(closeBody.contains("\"expectedCashAmount\":{\"amount\":\"44.00\""), closeBody)

        assertEquals(SalePaymentStatus.PAID, fixture.sales.sales.getValue(SALE).paymentStatus)
        assertEquals(Money.of("24.00"), fixture.sales.sales.getValue(SALE).paidAmount)
        assertEquals(2, fixture.payments.payments.size)
        assertEquals(
            2,
            fixture.cashMovements.movements.count { it.cashSessionId == "cash_1" && it.type.name == "SALE_PAYMENT" })
        assertNotNull(fixture.receivables.findById(ORG, "recv_1"))
        assertTrue(fixture.audit.events.isNotEmpty())
    }

    @Test
    fun `external payment without reference is rejected by route`() = testApplication {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                paymentsRoutes(
                    authenticateRequestUseCase = fixture.authenticate,
                    activeOrganizationResolverUseCase = fixture.activeOrganization,
                    effectivePermissionResolverUseCase = fixture.effectivePermissions,
                    paymentsModule = fixture.paymentsModule,
                )
            }
        }

        val response = client.post("/organizations/$ORG/sales/$SALE/payments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", ORG)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "amount": { "amount": "24.00", "currency": "USD" },
                  "method": "BANK_TRANSFER",
                  "paidAt": "${NOW}"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    private fun fixture(): Fixture {
        val authRepository = RoutePaymentAuthRepository()
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
        val token = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = NOW).token

        val sales = RoutePaymentSaleRepository()
        val payments = RoutePaymentRepository()
        val cashSessions = RoutePaymentCashSessionRepository()
        val cashMovements = RoutePaymentCashMovementRepository()
        val receivables = RoutePaymentReceivableRepository()
        val audit = RoutePaymentAuditLogger()
        val idGenerator = DeterministicPaymentsIdGenerator()
        val settlementRepository = DirectPaymentSettlementRepository(
            paymentRepository = payments,
            saleRepository = sales,
            cashSessionRepository = cashSessions,
            cashMovementRepository = cashMovements,
            receivableRepository = receivables,
        )

        val paymentsModule = PaymentsModule(
            registerPaymentUseCase = RegisterPaymentUseCase(
                saleRepository = sales,
                paymentRepository = payments,
                cashSessionRepository = cashSessions,
                cashMovementRepository = cashMovements,
                receivableRepository = receivables,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            openCashSessionUseCase = OpenCashSessionUseCase(
                cashSessionRepository = cashSessions,
                cashMovementRepository = cashMovements,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            registerCashMovementUseCase = RegisterCashMovementUseCase(
                cashSessionRepository = cashSessions,
                cashMovementRepository = cashMovements,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            closeCashSessionUseCase = CloseCashSessionUseCase(
                cashSessionRepository = cashSessions,
                auditLogger = audit,
                clock = clock,
            ),
            createReceivableForSaleUseCase = CreateReceivableForSaleUseCase(
                saleRepository = sales,
                receivableRepository = receivables,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            registerReceivableCollectionUseCase = RegisterReceivableCollectionUseCase(
                saleRepository = sales,
                receivableRepository = receivables,
                cashSessionRepository = cashSessions,
                cashMovementRepository = cashMovements,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
        )

        return Fixture(
            authenticate = authenticate,
            activeOrganization = activeOrganization,
            effectivePermissions = effectivePermissions,
            paymentsModule = paymentsModule,
            accessToken = token,
            sales = sales,
            payments = payments,
            cashSessions = cashSessions,
            cashMovements = cashMovements,
            receivables = receivables,
            audit = audit,
        )
    }

    private data class Fixture(
        val authenticate: AuthenticateRequestUseCase,
        val activeOrganization: ActiveOrganizationResolverUseCase,
        val effectivePermissions: EffectivePermissionResolverUseCase,
        val paymentsModule: PaymentsModule,
        val accessToken: String,
        val sales: RoutePaymentSaleRepository,
        val payments: RoutePaymentRepository,
        val cashSessions: RoutePaymentCashSessionRepository,
        val cashMovements: RoutePaymentCashMovementRepository,
        val receivables: RoutePaymentReceivableRepository,
        val audit: RoutePaymentAuditLogger,
    )

    private fun confirmedSale(): Sale = Sale.createDraft(
        id = SALE,
        organizationId = ORG,
        branchId = BRANCH,
        activityId = ACTIVITY,
        saleType = SaleType.SALE,
        workflowMode = SaleWorkflowMode.QUICK_SALE,
        saleNumber = "S-000001",
        customerId = CUSTOMER,
        customerSnapshot = CustomerSnapshot(
            customerId = CUSTOMER,
            displayName = "Ana Cliente",
            taxId = "9999999999999",
            taxIdType = "final_consumer",
            email = "ana@example.com",
        ),
        createdAt = NOW,
    ).addItem(sampleItem(), NOW).confirm(NOW)

    private fun sampleItem(): SaleItem = SaleItem.create(
        id = "sitem_1",
        catalogItemId = "cat_1",
        name = "Parrillada",
        unitPrice = Money.of("12.00"),
        quantity = Quantity.units(2),
        catalogSnapshot = CatalogItemSnapshot(
            catalogItemId = "cat_1",
            sourceTemplateId = "tpl_1",
            globalCatalogId = "gcat_parrillada",
            productFamilyId = null,
            name = "Parrillada",
            type = CatalogItemType.PRODUCT,
            taxProfileId = "tax_iva_0",
            unitCode = "unit",
        ),
        taxProfileSnapshot = TaxProfileSnapshotForSale(
            code = "iva_0",
            taxName = "IVA",
            rate = Percentage.zero(),
            sriTaxCode = "2",
            sriRateCode = "0",
            treatment = TaxTreatment.IVA_ZERO,
            legalBasis = "Test",
            effectiveFrom = LocalDate.parse("2026-01-01"),
            source = "test",
        ),
    )

    private companion object {
        const val ORG = "org_1"
        const val BRANCH = "br_1"
        const val ACTIVITY = "act_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        const val CUSTOMER = "cus_1"
        val NOW: Instant = Instant.parse("2026-05-18T20:00:00Z")
    }
}

private class RoutePaymentAuthRepository : AuthContextRepository {
    val users = linkedMapOf<String, User>()
    val sessions = linkedMapOf<String, UserSession>()
    val organizations = linkedMapOf<String, Organization>()
    val memberships = linkedMapOf<String, OrganizationMembership>()
    val roles = linkedMapOf<String, RoleDefinition>()

    override fun findUserById(userId: String): User? = users[userId]
    override fun findSessionById(sessionId: String): UserSession? = sessions[sessionId]
    override fun findMembershipsByUserId(userId: String): List<OrganizationMembership> =
        memberships.values.filter { it.userId == userId }

    override fun findOrganizationById(organizationId: String): Organization? = organizations[organizationId]
    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> = roleIds.mapNotNull(roles::get)
}

private class RoutePaymentSaleRepository : OperationalSaleRepository {
    val sales = linkedMapOf<String, Sale>()

    override fun create(sale: Sale) {
        if (sales.containsKey(sale.id)) throw DomainRuleViolation("Sale already exists.")
        sales[sale.id] = sale
    }

    override fun update(sale: Sale) {
        if (!sales.containsKey(sale.id)) throw DomainRuleViolation("Sale does not exist.")
        sales[sale.id] = sale
    }

    override fun findById(organizationId: String, saleId: String): Sale? =
        sales[saleId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun search(query: SaleSearchQuery): List<Sale> = sales.values
        .asSequence()
        .filter { it.organizationId == query.organizationId.trim() }
        .filter { query.statuses.isEmpty() || it.operationalStatus in query.statuses }
        .filter { query.customerId == null || it.customerId == query.customerId }
        .filter { query.activityId == null || it.activityId == query.activityId }
        .filter { query.from == null || !it.createdAt.isBefore(query.from) }
        .filter { query.to == null || !it.createdAt.isAfter(query.to) }
        .sortedByDescending { it.createdAt }
        .take(query.limit.coerceIn(1, 500))
        .toList()
}

private class RoutePaymentRepository : PaymentRepository {
    val payments = mutableListOf<Payment>()
    override fun create(payment: Payment) {
        payments += payment
    }

    override fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment> =
        payments.filter { it.organizationId == organizationId.trim() && it.saleId == saleId.trim() }
}

private class RoutePaymentCashSessionRepository : PaymentCashSessionRepository {
    val sessions = linkedMapOf<String, CashSession>()

    override fun create(session: CashSession) {
        if (sessions.containsKey(session.id)) throw DomainRuleViolation("Cash session already exists.")
        sessions[session.id] = session
    }

    override fun findById(organizationId: String, cashSessionId: String): CashSession? =
        sessions[cashSessionId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun findOpenByOrganization(organizationId: String): CashSession? =
        sessions.values.firstOrNull { it.organizationId == organizationId.trim() && it.status.name == "OPEN" }

    override fun findOpenByBranch(organizationId: String, branchId: String): CashSession? =
        sessions.values.firstOrNull {
            it.organizationId == organizationId.trim() && it.branchId == branchId.trim() && it.status.name == "OPEN"
        }

    override fun update(session: CashSession) {
        sessions[session.id] = session
    }
}

private class RoutePaymentCashMovementRepository : CashMovementRepository {
    val movements = mutableListOf<CashMovement>()
    override fun create(movement: CashMovement) {
        movements += movement
    }

    override fun findByCashSession(organizationId: String, cashSessionId: String): List<CashMovement> =
        movements.filter { it.organizationId == organizationId.trim() && it.cashSessionId == cashSessionId.trim() }
}

private class RoutePaymentReceivableRepository : ReceivableRepository {
    val receivables = linkedMapOf<String, Receivable>()
    override fun create(receivable: Receivable) {
        receivables[receivable.id] = receivable
    }

    override fun update(receivable: Receivable) {
        receivables[receivable.id] = receivable
    }

    override fun findById(organizationId: String, receivableId: String): Receivable? =
        receivables[receivableId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun findBySaleId(organizationId: String, saleId: String): Receivable? =
        receivables.values.firstOrNull { it.organizationId == organizationId.trim() && it.saleId == saleId.trim() }
}

private class RoutePaymentAuditLogger : PaymentAuditLogger {
    val events = mutableListOf<PaymentAuditEvent>()
    override fun log(event: PaymentAuditEvent) {
        events += event
    }
}

private class DeterministicPaymentsIdGenerator : PaymentsIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val normalized = prefix.trim().lowercase()
        val next = counters.getOrDefault(normalized, 0) + 1
        counters[normalized] = next
        return "${normalized}_$next"
    }
}
