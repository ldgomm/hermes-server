package com.hermes.application.payments

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashSession
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.Receivable
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.money.Money
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.TaxTreatment
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullFlowTest {
    @Test
    fun `open cash collect partial create receivable collect balance and close cash`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        val opened = fixture.openCashSessionUseCase.execute(
            OpenCashSessionCommand(
                organizationId = ORG,
                branchId = BRANCH,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                openingBalance = Money.of("20.00"),
                openedAt = NOW,
                notes = "Apertura",
            )
        )

        assertEquals("cash_1", opened.cashSession.id)
        assertEquals(Money.of("20.00"), opened.cashSession.expectedCashAmount)

        val firstPayment = fixture.registerPaymentUseCase.execute(
            RegisterPaymentCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                amount = Money.of("10.00"),
                method = PaymentMethod.CASH,
                paidAt = NOW.plusSeconds(60),
                markRemainingAsReceivable = true,
                receivableDueAt = NOW.plusSeconds(86_400),
            )
        )

        assertEquals(SalePaymentStatus.PARTIALLY_PAID, firstPayment.sale.paymentStatus)
        assertEquals("recv_1", firstPayment.receivable?.id)
        assertEquals(Money.of("30.00"), firstPayment.cashSession?.expectedCashAmount)

        val collection = fixture.registerReceivableCollectionUseCase.execute(
            RegisterReceivableCollectionCommand(
                organizationId = ORG,
                receivableId = "recv_1",
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                amount = Money.of("14.00"),
                method = PaymentMethod.CASH,
                collectedAt = NOW.plusSeconds(120),
                reference = null,
                notes = "Saldo",
            )
        )

        assertEquals(Money.zero(), collection.receivable.balanceDue)
        assertEquals(SalePaymentStatus.PAID, collection.sale.paymentStatus)
        assertEquals(Money.of("24.00"), collection.sale.paidAmount)

        val closed = fixture.closeCashSessionUseCase.execute(
            CloseCashSessionCommand(
                organizationId = ORG,
                cashSessionId = "cash_1",
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                countedCashAmount = Money.of("44.00"),
                reason = "Cierre normal",
                closedAt = NOW.plusSeconds(180),
                notes = "Sin novedad",
            )
        )

        assertEquals("CLOSED", closed.cashSession.status.name)
        assertEquals(Money.of("44.00"), closed.cashSession.expectedCashAmount)
        assertEquals(2, fixture.payments.payments.size)
        assertEquals(2, fixture.movements.movements.count { it.cashSessionId == "cash_1" && it.type.name == "SALE_PAYMENT" })
        assertTrue(fixture.audit.events.isNotEmpty())
    }

    private fun fixture(): Fixture {
        val sales = TestSaleRepository()
        val payments = TestPaymentRepository()
        val cashSessions = TestCashSessionRepository()
        val movements = TestCashMovementRepository()
        val receivables = TestReceivableRepository()
        val audit = TestPaymentAuditLogger()
        val idGenerator = TestPaymentsIdGenerator()
        val settlementRepository = DirectPaymentSettlementRepository(
            paymentRepository = payments,
            saleRepository = sales,
            cashSessionRepository = cashSessions,
            cashMovementRepository = movements,
            receivableRepository = receivables,
        )
        val clock = Clock.fixed(NOW, ZoneOffset.UTC)

        return Fixture(
            sales = sales,
            payments = payments,
            cashSessions = cashSessions,
            movements = movements,
            receivables = receivables,
            audit = audit,
            openCashSessionUseCase = OpenCashSessionUseCase(cashSessions, movements, idGenerator, audit, clock),
            registerPaymentUseCase = RegisterPaymentUseCase(
                saleRepository = sales,
                paymentRepository = payments,
                cashSessionRepository = cashSessions,
                cashMovementRepository = movements,
                receivableRepository = receivables,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            registerReceivableCollectionUseCase = RegisterReceivableCollectionUseCase(
                saleRepository = sales,
                receivableRepository = receivables,
                cashSessionRepository = cashSessions,
                cashMovementRepository = movements,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = audit,
                clock = clock,
            ),
            closeCashSessionUseCase = CloseCashSessionUseCase(cashSessions, audit, clock),
        )
    }

    private data class Fixture(
        val sales: TestSaleRepository,
        val payments: TestPaymentRepository,
        val cashSessions: TestCashSessionRepository,
        val movements: TestCashMovementRepository,
        val receivables: TestReceivableRepository,
        val audit: TestPaymentAuditLogger,
        val openCashSessionUseCase: OpenCashSessionUseCase,
        val registerPaymentUseCase: RegisterPaymentUseCase,
        val registerReceivableCollectionUseCase: RegisterReceivableCollectionUseCase,
        val closeCashSessionUseCase: CloseCashSessionUseCase,
    )

    private fun permissions(): Set<String> = setOf(
        PermissionCatalog.PAYMENTS_COLLECT,
        PermissionCatalog.PAYMENTS_PARTIAL_COLLECT,
        PermissionCatalog.PAYMENTS_MARK_AS_CREDIT,
        PermissionCatalog.RECEIVABLES_CREATE,
        PermissionCatalog.RECEIVABLES_REGISTER_PAYMENT,
        PermissionCatalog.CASH_SESSION_OPEN,
        PermissionCatalog.CASH_SESSION_CLOSE,
        PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW,
        PermissionCatalog.CASH_MOVEMENTS_REGISTER_OUTFLOW,
        PermissionCatalog.CASH_MOVEMENTS_ADJUST,
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

private class TestSaleRepository : OperationalSaleRepository {
    val sales = linkedMapOf<String, Sale>()
    override fun create(sale: Sale) { sales[sale.id] = sale }
    override fun update(sale: Sale) { sales[sale.id] = sale }
    override fun findById(organizationId: String, saleId: String): Sale? =
        sales[saleId.trim()]?.takeIf { it.organizationId == organizationId.trim() }
    override fun search(query: SaleSearchQuery): List<Sale> = sales.values.filter { it.organizationId == query.organizationId }
}

private class TestPaymentRepository : PaymentRepository {
    val payments = mutableListOf<Payment>()
    override fun create(payment: Payment) { payments += payment }
    override fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment> =
        payments.filter { it.organizationId == organizationId.trim() && it.saleId == saleId.trim() }
}

private class TestCashSessionRepository : PaymentCashSessionRepository {
    val sessions = linkedMapOf<String, CashSession>()
    override fun create(session: CashSession) { sessions[session.id] = session }
    override fun findById(organizationId: String, cashSessionId: String): CashSession? =
        sessions[cashSessionId.trim()]?.takeIf { it.organizationId == organizationId.trim() }
    override fun findOpenByOrganization(organizationId: String): CashSession? =
        sessions.values.firstOrNull { it.organizationId == organizationId.trim() && it.status.name == "OPEN" }
    override fun findOpenByBranch(organizationId: String, branchId: String): CashSession? =
        sessions.values.firstOrNull { it.organizationId == organizationId.trim() && it.branchId == branchId.trim() && it.status.name == "OPEN" }
    override fun update(session: CashSession) { sessions[session.id] = session }
}

private class TestCashMovementRepository : CashMovementRepository {
    val movements = mutableListOf<CashMovement>()
    override fun create(movement: CashMovement) { movements += movement }
    override fun findByCashSession(organizationId: String, cashSessionId: String): List<CashMovement> =
        movements.filter { it.organizationId == organizationId.trim() && it.cashSessionId == cashSessionId.trim() }
}

private class TestReceivableRepository : ReceivableRepository {
    val receivables = linkedMapOf<String, Receivable>()
    override fun create(receivable: Receivable) { receivables[receivable.id] = receivable }
    override fun update(receivable: Receivable) { receivables[receivable.id] = receivable }
    override fun findById(organizationId: String, receivableId: String): Receivable? =
        receivables[receivableId.trim()]?.takeIf { it.organizationId == organizationId.trim() }
    override fun findBySaleId(organizationId: String, saleId: String): Receivable? =
        receivables.values.firstOrNull { it.organizationId == organizationId.trim() && it.saleId == saleId.trim() }
}

private class TestPaymentAuditLogger : PaymentAuditLogger {
    val events = mutableListOf<PaymentAuditEvent>()
    override fun log(event: PaymentAuditEvent) { events += event }
}

private class TestPaymentsIdGenerator : PaymentsIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val normalized = prefix.trim().lowercase()
        val next = counters.getOrDefault(normalized, 0) + 1
        counters[normalized] = next
        return "${normalized}_$next"
    }
}
