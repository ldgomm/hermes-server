package com.hermes.application.payments

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashMovementDirection
import com.hermes.domain.cash.CashMovementType
import com.hermes.domain.cash.CashSession
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.Receivable
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.TaxTreatment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PaymentUseCasesTest {
    @Test
    fun `opens cash session and records opening movement`() {
        val fixture = fixture()
        val result = fixture.openCashSessionUseCase.execute(
            OpenCashSessionCommand(
                organizationId = ORG,
                branchId = BRANCH,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                openingBalance = Money.of("20.00"),
                openedAt = NOW,
            )
        )

        assertEquals("cash_1", result.cashSession.id)
        assertEquals(BRANCH, result.cashSession.branchId)
        assertEquals("20.00", result.cashSession.expectedCashAmount.amount.toPlainString())
        assertEquals("cmov_1", result.openingMovement?.id)
        assertTrue(fixture.audit.events.any { it.action == PaymentAuditAction.CASH_SESSION_OPENED })
    }

    @Test
    fun `rejects duplicate open cash session for the same branch`() {
        val fixture = fixture()
        fixture.cashSessions.create(CashSession.open("cash_existing", ORG, USER, Money.zero(), NOW, branchId = BRANCH))

        assertFailsWith<DomainRuleViolation> {
            fixture.openCashSessionUseCase.execute(
                OpenCashSessionCommand(ORG, BRANCH, USER, permissions(), Money.zero(), NOW)
            )
        }
    }

    @Test
    fun `registers manual cash expense and updates expected cash`() {
        val fixture = fixture()
        fixture.cashSessions.create(CashSession.open("cash_1", ORG, USER, Money.of("50.00"), NOW, branchId = BRANCH))

        val result = fixture.registerCashMovementUseCase.execute(
            RegisterCashMovementCommand(
                organizationId = ORG,
                cashSessionId = "cash_1",
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                type = CashMovementType.MANUAL_EXPENSE,
                direction = CashMovementDirection.OUT,
                amount = Money.of("5.00"),
                occurredAt = NOW,
                notes = "Compra de fundas",
            )
        )

        assertEquals("45.00", result.cashSession.expectedCashAmount.amount.toPlainString())
        assertEquals("cmov_1", result.cashMovement.id)
    }

    @Test
    fun `closes cash session with counted amount`() {
        val fixture = fixture()
        fixture.cashSessions.create(CashSession.open("cash_1", ORG, USER, Money.of("50.00"), NOW, branchId = BRANCH))

        val result = fixture.closeCashSessionUseCase.execute(
            CloseCashSessionCommand(
                organizationId = ORG,
                cashSessionId = "cash_1",
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                countedCashAmount = Money.of("49.00"),
                reason = "Cierre diario",
                closedAt = NOW.plusSeconds(3600),
            )
        )

        assertEquals("49.00", result.cashSession.countedCashAmount?.amount?.toPlainString())
        assertEquals("1.00", result.cashSession.differenceAmount?.amount?.toPlainString())
        assertTrue(fixture.audit.events.any { it.action == PaymentAuditAction.CASH_SESSION_CLOSED })
    }

    @Test
    fun `creates receivable for fully unpaid confirmed sale`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())

        val result = fixture.createReceivableForSaleUseCase.execute(
            CreateReceivableForSaleCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                dueAt = NOW.plusSeconds(86_400),
                reason = "Cliente paga mañana",
            )
        )

        assertEquals("recv_1", result.receivable.id)
        assertEquals("24.00", result.receivable.balanceDue.amount.toPlainString())
        assertTrue(fixture.audit.events.any { it.action == PaymentAuditAction.RECEIVABLE_CREATED })
    }

    @Test
    fun `registers partial cash payment and creates receivable atomically through settlement repository`() {
        val fixture = fixture()
        fixture.sales.create(confirmedSale())
        fixture.cashSessions.create(CashSession.open("cash_1", ORG, USER, Money.of("20.00"), NOW, branchId = BRANCH))

        val result = fixture.registerPaymentUseCase.execute(
            RegisterPaymentCommand(
                organizationId = ORG,
                saleId = SALE,
                actorUserId = USER,
                actorEffectivePermissions = permissions(),
                amount = Money.of("10.00"),
                method = PaymentMethod.CASH,
                paidAt = NOW,
                markRemainingAsReceivable = true,
                receivableDueAt = NOW.plusSeconds(86_400),
            )
        )

        assertEquals(SalePaymentStatus.PARTIALLY_PAID, result.sale.paymentStatus)
        assertNotNull(result.cashMovement)
        assertNotNull(result.receivable)
        assertEquals("14.00", result.receivable?.balanceDue?.amount?.toPlainString())
        assertEquals(1, fixture.payments.payments.size)
        assertEquals(1, fixture.receivables.receivables.size)
    }

    private fun fixture(): Fixture {
        val sales = InMemorySaleRepository()
        val payments = InMemoryPaymentRepository()
        val cashSessions = InMemoryCashSessionRepository()
        val cashMovements = InMemoryCashMovementRepository()
        val receivables = InMemoryReceivableRepository()
        val ids = DeterministicPaymentsIdGenerator()
        val audit = RecordingAudit()
        val settlement = DirectPaymentSettlementRepository(payments, sales, cashSessions, cashMovements, receivables)

        return Fixture(
            sales = sales,
            payments = payments,
            cashSessions = cashSessions,
            cashMovements = cashMovements,
            receivables = receivables,
            audit = audit,
            openCashSessionUseCase = OpenCashSessionUseCase(cashSessions, cashMovements, ids, audit, CLOCK),
            registerCashMovementUseCase = RegisterCashMovementUseCase(cashSessions, cashMovements, ids, audit, CLOCK),
            closeCashSessionUseCase = CloseCashSessionUseCase(cashSessions, audit, CLOCK),
            createReceivableForSaleUseCase = CreateReceivableForSaleUseCase(sales, receivables, ids, audit, CLOCK),
            registerPaymentUseCase = RegisterPaymentUseCase(
                saleRepository = sales,
                paymentRepository = payments,
                cashSessionRepository = cashSessions,
                receivableRepository = receivables,
                idGenerator = ids,
                auditLogger = audit,
                clock = CLOCK,
                cashMovementRepository = cashMovements,
                settlementRepository = settlement,
            ),
        )
    }

    private data class Fixture(
        val sales: InMemorySaleRepository,
        val payments: InMemoryPaymentRepository,
        val cashSessions: InMemoryCashSessionRepository,
        val cashMovements: InMemoryCashMovementRepository,
        val receivables: InMemoryReceivableRepository,
        val audit: RecordingAudit,
        val openCashSessionUseCase: OpenCashSessionUseCase,
        val registerCashMovementUseCase: RegisterCashMovementUseCase,
        val closeCashSessionUseCase: CloseCashSessionUseCase,
        val createReceivableForSaleUseCase: CreateReceivableForSaleUseCase,
        val registerPaymentUseCase: RegisterPaymentUseCase,
    )

    private class InMemorySaleRepository : OperationalSaleRepository {
        val sales = linkedMapOf<String, Sale>()
        override fun create(sale: Sale) { sales[sale.id] = sale }
        override fun update(sale: Sale) { sales[sale.id] = sale }
        override fun findById(organizationId: String, saleId: String): Sale? = sales[saleId]?.takeIf { it.organizationId == organizationId }
        override fun search(query: SaleSearchQuery): List<Sale> = sales.values.filter { it.organizationId == query.organizationId }
    }

    private class InMemoryPaymentRepository : PaymentRepository {
        val payments = mutableListOf<Payment>()
        override fun create(payment: Payment) { payments += payment }
        override fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment> =
            payments.filter { it.organizationId == organizationId && it.saleId == saleId && it.isEffective }
    }

    private class InMemoryCashSessionRepository : PaymentCashSessionRepository {
        val sessions = linkedMapOf<String, CashSession>()
        override fun create(session: CashSession) { sessions[session.id] = session }
        override fun findById(organizationId: String, cashSessionId: String): CashSession? = sessions[cashSessionId]?.takeIf { it.organizationId == organizationId }
        override fun findOpenByOrganization(organizationId: String): CashSession? = sessions.values.firstOrNull { it.organizationId == organizationId && it.status.name == "OPEN" }
        override fun findOpenByBranch(organizationId: String, branchId: String): CashSession? = sessions.values.firstOrNull { it.organizationId == organizationId && it.branchId == branchId && it.status.name == "OPEN" }
        override fun update(session: CashSession) { sessions[session.id] = session }
    }

    private class InMemoryCashMovementRepository : CashMovementRepository {
        val movements = mutableListOf<CashMovement>()
        override fun create(movement: CashMovement) { movements += movement }
    }

    private class InMemoryReceivableRepository : ReceivableRepository {
        val receivables = linkedMapOf<String, Receivable>()
        override fun create(receivable: Receivable) { receivables[receivable.id] = receivable }
        override fun update(receivable: Receivable) { receivables[receivable.id] = receivable }
        override fun findById(organizationId: String, receivableId: String): Receivable? = receivables[receivableId]?.takeIf { it.organizationId == organizationId }
        override fun findBySaleId(organizationId: String, saleId: String): Receivable? = receivables.values.firstOrNull { it.organizationId == organizationId && it.saleId == saleId }
    }

    private class DeterministicPaymentsIdGenerator : PaymentsIdGenerator {
        private val counters = linkedMapOf<String, Int>()
        override fun newId(prefix: String): String {
            val next = (counters[prefix] ?: 0) + 1
            counters[prefix] = next
            return "${prefix}_$next"
        }
    }

    private class RecordingAudit : PaymentAuditLogger {
        val events = mutableListOf<PaymentAuditEvent>()
        override fun log(event: PaymentAuditEvent) { events += event }
    }

    private fun confirmedSale(): Sale = Sale.createDraft(
        id = SALE,
        organizationId = ORG,
        branchId = BRANCH,
        activityId = ACTIVITY,
        saleType = SaleType.SALE,
        workflowMode = SaleWorkflowMode.QUICK_SALE,
        saleNumber = "S-1",
        customerId = CUSTOMER,
        customerSnapshot = CustomerSnapshot(CUSTOMER, "Ana", "1720000001", "cedula", "ana@example.com"),
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

    private companion object {
        const val ORG = "org_1"
        const val BRANCH = "br_1"
        const val ACTIVITY = "act_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        const val CUSTOMER = "cus_1"
        val NOW: Instant = Instant.parse("2026-05-18T20:00:00Z")
        val CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    }
}
