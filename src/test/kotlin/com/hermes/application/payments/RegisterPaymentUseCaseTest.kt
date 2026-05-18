package com.hermes.application.payments

import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.cash.CashSession
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.money.Money
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.PaymentMethod
import com.hermes.domain.payment.Receivable
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.*
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxTreatment
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.*

class RegisterPaymentUseCaseTest {
    @Test
    fun `registers cash payment and creates cash movement`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())
        fixture.cashSessionRepository.openSession = CashSession.open(
            id = "cash_1",
            organizationId = ORG,
            openedBy = USER,
            openingBalance = Money.of("20.00"),
            openedAt = NOW,
        )

        val result = fixture.useCase.execute(
            paymentCommand(
                amount = Money.of("24.00"),
                method = PaymentMethod.CASH,
                permissions = setOf(
                    PermissionCatalog.PAYMENTS_COLLECT,
                    PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW,
                ),
            )
        )

        assertEquals("pay_1", result.payment.id)
        assertEquals(SalePaymentStatus.PAID, result.sale.paymentStatus)
        assertEquals(Money.of("44.00"), result.cashSession?.expectedCashAmount)
        assertEquals("cmov_1", result.cashMovement?.id)
        assertEquals(listOf("pay_1"), fixture.paymentRepository.payments.map { it.id })
        assertTrue(fixture.auditLogger.events.any { it.action == PaymentAuditAction.PAYMENT_REGISTERED })
        assertTrue(fixture.auditLogger.events.any { it.action == PaymentAuditAction.CASH_MOVEMENT_CREATED })
    }

    @Test
    fun `rejects cash payment without open cash session`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                paymentCommand(
                    amount = Money.of("24.00"),
                    method = PaymentMethod.CASH,
                    permissions = setOf(
                        PermissionCatalog.PAYMENTS_COLLECT,
                        PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW,
                    ),
                )
            )
        }
    }

    @Test
    fun `registers bank transfer without cash movement`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        val result = fixture.useCase.execute(
            paymentCommand(
                amount = Money.of("24.00"),
                method = PaymentMethod.BANK_TRANSFER,
                reference = "TRX-001",
                permissions = setOf(PermissionCatalog.PAYMENTS_COLLECT),
            )
        )

        assertEquals(SalePaymentStatus.PAID, result.sale.paymentStatus)
        assertNull(result.cashMovement)
        assertNull(result.cashSession)
        assertEquals("TRX-001", result.payment.reference)
    }

    @Test
    fun `rejects external payment without reference`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                paymentCommand(
                    amount = Money.of("24.00"),
                    method = PaymentMethod.BANK_TRANSFER,
                    reference = null,
                    permissions = setOf(PermissionCatalog.PAYMENTS_COLLECT),
                )
            )
        }
    }

    @Test
    fun `creates receivable when partial payment leaves balance`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        val result = fixture.useCase.execute(
            paymentCommand(
                amount = Money.of("10.00"),
                method = PaymentMethod.BANK_TRANSFER,
                reference = "TRX-002",
                markRemainingAsReceivable = true,
                permissions = setOf(
                    PermissionCatalog.PAYMENTS_COLLECT,
                    PermissionCatalog.PAYMENTS_PARTIAL_COLLECT,
                    PermissionCatalog.PAYMENTS_MARK_AS_CREDIT,
                    PermissionCatalog.RECEIVABLES_CREATE,
                ),
            )
        )

        assertEquals(SalePaymentStatus.PARTIALLY_PAID, result.sale.paymentStatus)
        assertNotNull(result.receivable)
        assertEquals(Money.of("14.00"), result.receivable?.totalDue)
        assertEquals(Money.of("14.00"), result.receivable?.balanceDue)
        assertEquals("recv_1", result.receivable?.id)
        assertTrue(fixture.auditLogger.events.any { it.action == PaymentAuditAction.RECEIVABLE_CREATED })
    }

    @Test
    fun `rejects partial payment when remaining balance is not marked as receivable`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                paymentCommand(
                    amount = Money.of("10.00"),
                    method = PaymentMethod.BANK_TRANSFER,
                    reference = "TRX-003",
                    markRemainingAsReceivable = false,
                    permissions = setOf(
                        PermissionCatalog.PAYMENTS_COLLECT,
                        PermissionCatalog.PAYMENTS_PARTIAL_COLLECT,
                    ),
                )
            )
        }
    }

    @Test
    fun `rejects overpayment`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(confirmedSale())

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                paymentCommand(
                    amount = Money.of("25.00"),
                    method = PaymentMethod.BANK_TRANSFER,
                    reference = "TRX-004",
                    permissions = setOf(PermissionCatalog.PAYMENTS_COLLECT),
                )
            )
        }
    }

    @Test
    fun `rejects payment for draft sale`() {
        val fixture = paymentFixture()
        fixture.saleRepository.create(draftSaleWithItem())

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                paymentCommand(
                    amount = Money.of("24.00"),
                    method = PaymentMethod.BANK_TRANSFER,
                    reference = "TRX-005",
                    permissions = setOf(PermissionCatalog.PAYMENTS_COLLECT),
                )
            )
        }
    }

    private fun paymentFixture(): PaymentFixture {
        val saleRepository = InMemoryPaymentSaleRepository()
        val paymentRepository = InMemoryPaymentRepository()
        val cashSessionRepository = InMemoryPaymentCashSessionRepository()
        val receivableRepository = InMemoryReceivableRepository()
        val idGenerator = DeterministicPaymentsIdGenerator()
        val auditLogger = RecordingPaymentAuditLogger()

        val useCase = RegisterPaymentUseCase(
            saleRepository = saleRepository,
            paymentRepository = paymentRepository,
            cashSessionRepository = cashSessionRepository,
            receivableRepository = receivableRepository,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
        )

        return PaymentFixture(
            saleRepository = saleRepository,
            paymentRepository = paymentRepository,
            cashSessionRepository = cashSessionRepository,
            receivableRepository = receivableRepository,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            useCase = useCase,
        )
    }

    private fun paymentCommand(
        amount: Money,
        method: PaymentMethod,
        reference: String? = null,
        markRemainingAsReceivable: Boolean = false,
        permissions: Set<String>,
    ): RegisterPaymentCommand =
        RegisterPaymentCommand(
            organizationId = ORG,
            saleId = SALE,
            actorUserId = USER,
            actorEffectivePermissions = permissions,
            amount = amount,
            method = method,
            paidAt = NOW,
            reference = reference,
            markRemainingAsReceivable = markRemainingAsReceivable,
            receivableDueAt = NOW.plusSeconds(86_400),
        )

    private fun confirmedSale(): Sale =
        draftSaleWithItem().confirm(NOW)

    private fun draftSaleWithItem(): Sale =
        Sale.createDraft(
            id = SALE,
            organizationId = ORG,
            branchId = BRANCH,
            activityId = ACTIVITY,
            saleType = SaleType.SALE,
            workflowMode = SaleWorkflowMode.QUICK_SALE,
            saleNumber = "S-000001",
            customerId = "cus_1",
            customerSnapshot = CustomerSnapshot(
                customerId = "cus_1",
                displayName = "Cliente prueba",
                taxId = "9999999999999",
                taxIdType = "final_consumer",
            ),
            createdAt = NOW,
        ).addItem(sampleItem(), NOW)

    private fun sampleItem(): SaleItem =
        SaleItem.create(
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

    private data class PaymentFixture(
        val saleRepository: InMemoryPaymentSaleRepository,
        val paymentRepository: InMemoryPaymentRepository,
        val cashSessionRepository: InMemoryPaymentCashSessionRepository,
        val receivableRepository: InMemoryReceivableRepository,
        val idGenerator: DeterministicPaymentsIdGenerator,
        val auditLogger: RecordingPaymentAuditLogger,
        val useCase: RegisterPaymentUseCase,
    )

    private class InMemoryPaymentSaleRepository : OperationalSaleRepository {
        private val sales = linkedMapOf<String, Sale>()

        override fun create(sale: Sale) {
            sales[sale.id] = sale
        }

        override fun update(sale: Sale) {
            sales[sale.id] = sale
        }

        override fun findById(organizationId: String, saleId: String): Sale? =
            sales[saleId]?.takeIf { it.organizationId == organizationId }

        override fun search(query: SaleSearchQuery): List<Sale> =
            sales.values
                .filter { it.organizationId == query.organizationId }
                .filter { query.statuses.isEmpty() || it.operationalStatus in query.statuses }
                .filter { query.customerId == null || it.customerId == query.customerId }
                .filter { query.activityId == null || it.activityId == query.activityId }
                .take(query.limit)
    }

    private class InMemoryPaymentRepository : PaymentRepository {
        val payments = mutableListOf<Payment>()

        override fun create(payment: Payment) {
            payments += payment
        }

        override fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment> =
            payments.filter { it.organizationId == organizationId && it.saleId == saleId && it.isEffective }
    }

    private class InMemoryPaymentCashSessionRepository : PaymentCashSessionRepository {
        var openSession: CashSession? = null

        override fun findOpenByOrganization(organizationId: String): CashSession? =
            openSession?.takeIf { it.organizationId == organizationId }

        override fun update(session: CashSession) {
            openSession = session
        }
    }

    private class InMemoryReceivableRepository : ReceivableRepository {
        private val receivables = linkedMapOf<String, Receivable>()

        override fun create(receivable: Receivable) {
            receivables[receivable.id] = receivable
        }

        override fun update(receivable: Receivable) {
            receivables[receivable.id] = receivable
        }

        override fun findBySaleId(organizationId: String, saleId: String): Receivable? =
            receivables.values.firstOrNull { it.organizationId == organizationId && it.saleId == saleId }
    }

    private class DeterministicPaymentsIdGenerator : PaymentsIdGenerator {
        private val counters = linkedMapOf<String, Int>()

        override fun newId(prefix: String): String {
            val next = (counters[prefix] ?: 0) + 1
            counters[prefix] = next
            return "${prefix}_$next"
        }
    }

    private class RecordingPaymentAuditLogger : PaymentAuditLogger {
        val events = mutableListOf<PaymentAuditEvent>()

        override fun log(event: PaymentAuditEvent) {
            events += event
        }
    }

    private companion object {
        const val ORG = "org_1"
        const val BRANCH = "branch_1"
        const val ACTIVITY = "act_1"
        const val SALE = "sale_1"
        const val USER = "usr_1"
        val NOW: Instant = Instant.parse("2026-05-18T20:00:00Z")
    }
}
