package com.hermes.infrastructure.mongo.payments

import com.hermes.application.payments.PaymentSettlement
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
import com.hermes.domain.percentage.Percentage
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.sale.*
import com.hermes.domain.tax.TaxTreatment
import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoClient
import java.time.Instant
import java.time.LocalDate
import kotlin.test.*

class MongoPaymentsStoreIntegrationTest {
    private lateinit var client: MongoClient
    private lateinit var databaseName: String

    @BeforeTest
    fun startMongo() {
        MongoIntegrationTestSupport.assumeMongoAvailable()
        client = MongoIntegrationTestSupport.client()
        databaseName = MongoIntegrationTestSupport.databaseName("hermes_phase9_mongo_payments_store_test")
    }

    @AfterTest
    fun stopMongo() {
        if (::client.isInitialized) {
            runCatching { client.getDatabase(databaseName).drop() }
            runCatching { client.close() }
        }
    }

    @Test
    fun `settlement persists payment receivable cash movement cash session and sale atomically`() {
        val database = client.getDatabase(databaseName)
        MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

        val salesStore = MongoSalesStore(database)
        val paymentsStore = MongoPaymentsStore(client = client, database = database)

        val sale = confirmedSale()
        salesStore.saleRepository.create(sale)

        val cashSession = CashSession.open(
            id = "cash_1",
            organizationId = ORG,
            branchId = BRANCH,
            openedBy = USER,
            openingBalance = Money.of("20.00"),
            openedAt = NOW,
        )

        paymentsStore.cashSessionRepository.create(cashSession)

        val payment = Payment.record(
            id = "pay_1",
            organizationId = ORG,
            saleId = SALE,
            amount = Money.of("10.00"),
            method = PaymentMethod.CASH,
            paidAt = NOW.plusSeconds(60),
            reference = null,
            notes = "Abono efectivo",
        )

        val movement = CashMovement.create(
            id = "cmov_1",
            cashSessionId = cashSession.id,
            organizationId = ORG,
            branchId = BRANCH,
            type = CashMovementType.SALE_PAYMENT,
            direction = CashMovementDirection.IN,
            amount = Money.of("10.00"),
            occurredAt = NOW.plusSeconds(60),
            referenceId = payment.id,
            notes = "Payment for sale $SALE",
        )

        val updatedCashSession = cashSession.recordMovement(movement)
        val updatedSale = sale.registerPayment(payment, NOW.plusSeconds(60))
        val receivable = Receivable.createForSale(
            id = "recv_1",
            organizationId = ORG,
            saleId = SALE,
            customerId = CUSTOMER,
            totalDue = Money.of("14.00"),
            dueAt = NOW.plusSeconds(86_400),
            createdAt = NOW.plusSeconds(60),
            branchId = BRANCH,
        )

        paymentsStore.settlementRepository.persistPaymentSettlement(
            PaymentSettlement(
                payment = payment,
                sale = updatedSale,
                cashSession = updatedCashSession,
                cashMovement = movement,
                receivable = receivable,
            )
        )

        val storedPayment = paymentsStore.paymentRepository.findEffectiveBySale(ORG, SALE).single()
        assertEquals("pay_1", storedPayment.id)
        assertEquals(Money.of("10.00"), storedPayment.amount)

        val storedReceivable = paymentsStore.receivableRepository.findBySaleId(ORG, SALE)
        assertNotNull(storedReceivable)
        assertEquals("recv_1", storedReceivable.id)
        assertEquals(Money.of("14.00"), storedReceivable.balanceDue)

        val storedCashSession = paymentsStore.cashSessionRepository.findById(ORG, "cash_1")
        assertNotNull(storedCashSession)
        assertEquals(Money.of("30.00"), storedCashSession.expectedCashAmount)
        assertEquals(
            listOf("cmov_1"),
            paymentsStore.cashMovementRepository.findByCashSession(ORG, "cash_1").map { it.id })

        val storedSale = salesStore.saleRepository.findById(ORG, SALE)
        assertNotNull(storedSale)
        assertEquals(SalePaymentStatus.PARTIALLY_PAID, storedSale.paymentStatus)
        assertEquals(Money.of("10.00"), storedSale.paidAmount)
    }

    @Test
    fun `repositories keep organization boundaries`() {
        val boundaryDatabaseName =
            MongoIntegrationTestSupport.databaseName("hermes_phase9_mongo_payments_store_boundary_test")
        val database = client.getDatabase(boundaryDatabaseName)
        MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)
        val paymentsStore = MongoPaymentsStore(client = client, database = database)

        val cashSession = CashSession.open(
            id = "cash_1",
            organizationId = ORG,
            branchId = BRANCH,
            openedBy = USER,
            openingBalance = Money.of("20.00"),
            openedAt = NOW,
        )
        paymentsStore.cashSessionRepository.create(cashSession)

        assertNotNull(paymentsStore.cashSessionRepository.findById(ORG, "cash_1"))
        assertEquals(null, paymentsStore.cashSessionRepository.findById("org_other", "cash_1"))
        assertEquals(emptyList(), paymentsStore.paymentRepository.findEffectiveBySale("org_other", SALE))
    }

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
