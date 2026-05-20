package com.hermes.application.admin.operations

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdminOperationsUseCasesTest {
    @Test
    fun `search sales delegates when actor has sales permission`() {
        val repository = FakeAdminOperationsRepository()
        val useCase = SearchAdminSalesUseCase(repository)

        val result = useCase.execute(
            SearchAdminSalesCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
            ),
        )

        assertEquals(1, result.sales.size)
        assertEquals("sale_1", result.sales.first().id)
    }

    @Test
    fun `search sales rejects actor without permission`() {
        val useCase = SearchAdminSalesUseCase(FakeAdminOperationsRepository())

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                SearchAdminSalesCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = emptySet(),
                ),
            )
        }
    }

    @Test
    fun `operational today returns dashboard sections`() {
        val repository = FakeAdminOperationsRepository()
        val useCase = GetAdminOperationalTodayReportUseCase(repository)
        val from = Instant.parse("2026-05-20T00:00:00Z")
        val to = Instant.parse("2026-05-21T00:00:00Z")

        val result = useCase.execute(
            GetAdminOperationalTodayReportCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.REPORTS_DASHBOARD_VIEW),
                businessDate = LocalDate.parse("2026-05-20"),
                from = from,
                to = to,
            ),
        )

        assertEquals(1, result.sales.saleCount)
        assertEquals("10.00", result.sales.grandTotal.amount.toPlainString())
        assertTrue(result.alerts.isNotEmpty())
    }

    @Test
    fun `summary rejects invalid date range`() {
        val useCase = GetAdminSalesSummaryReportUseCase(FakeAdminOperationsRepository())

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                GetAdminSalesSummaryReportCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.REPORTS_SALES_VIEW),
                    from = Instant.parse("2026-05-21T00:00:00Z"),
                    to = Instant.parse("2026-05-20T00:00:00Z"),
                ),
            )
        }
    }
}

private class FakeAdminOperationsRepository : AdminOperationsQueryRepository {
    private val now = Instant.parse("2026-05-20T12:00:00Z")
    private val money = AdminMoneyAmount(BigDecimal("10.00"), "USD")
    private val sale = AdminSaleListItem(
        id = "sale_1",
        organizationId = "org_1",
        branchId = "br_1",
        activityId = "act_1",
        saleNumber = "S-1",
        saleType = "standard_sale",
        customerId = null,
        customerDisplayName = "Consumidor final",
        operationalStatus = "closed",
        paymentStatus = "paid",
        documentStatus = "authorized",
        itemCount = 1,
        subtotal = money,
        discountTotal = AdminMoneyAmount.zero(),
        taxTotal = AdminMoneyAmount.zero(),
        grandTotal = money,
        paidAmount = money,
        receivableAmount = AdminMoneyAmount.zero(),
        dueAt = null,
        cashSessionId = "cash_1",
        createdAt = now,
        updatedAt = now,
    )

    override fun searchSales(command: SearchAdminSalesCommand): List<AdminSaleListItem> = listOf(sale)

    override fun findSale(command: GetAdminSaleCommand): AdminSaleDetail? = AdminSaleDetail(
        summary = sale,
        lines = emptyList(),
        payments = emptyList(),
        documents = emptyList(),
    )

    override fun searchCashSessions(command: SearchAdminCashSessionsCommand): List<AdminCashSessionReadModel> =
        emptyList()

    override fun findCurrentCashSession(command: GetCurrentAdminCashSessionCommand): AdminCashSessionReadModel? = null
    override fun findCashSession(command: GetAdminCashSessionCommand): AdminCashSessionReadModel? = null
    override fun searchPayments(command: SearchAdminPaymentsCommand): List<AdminPaymentReadModel> = emptyList()
    override fun searchReceivables(command: SearchAdminReceivablesCommand): List<AdminReceivableReadModel> = emptyList()

    override fun operationalToday(command: GetAdminOperationalTodayReportCommand): AdminOperationalTodayReport =
        AdminOperationalTodayReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            businessDate = command.businessDate,
            from = command.from,
            to = command.to,
            sales = salesSummary(
                GetAdminSalesSummaryReportCommand(
                    organizationId = command.organizationId,
                    actorUserId = command.actorUserId,
                    actorEffectivePermissions = command.actorEffectivePermissions,
                    branchId = command.branchId,
                    activityId = command.activityId,
                    from = command.from,
                    to = command.to,
                ),
            ),
            cash = cashSummary(
                GetAdminCashSummaryReportCommand(
                    organizationId = command.organizationId,
                    actorUserId = command.actorUserId,
                    actorEffectivePermissions = command.actorEffectivePermissions,
                    branchId = command.branchId,
                    from = command.from,
                    to = command.to,
                ),
            ),
            tax = taxSummary(
                GetAdminTaxSummaryReportCommand(
                    organizationId = command.organizationId,
                    actorUserId = command.actorUserId,
                    actorEffectivePermissions = command.actorEffectivePermissions,
                    branchId = command.branchId,
                    activityId = command.activityId,
                    from = command.from,
                    to = command.to,
                ),
            ),
            currentCashSession = null,
            pendingReceivables = AdminMoneyAmount.zero(),
            topItems = emptyList(),
            alerts = listOf(AdminOperationalAlert("cash_session_not_open", "warning", "No hay caja abierta")),
        )

    override fun salesSummary(command: GetAdminSalesSummaryReportCommand): AdminSalesSummaryReport =
        AdminSalesSummaryReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            activityId = command.activityId,
            from = command.from,
            to = command.to,
            saleCount = 1,
            closedSaleCount = 1,
            canceledSaleCount = 0,
            openSaleCount = 0,
            itemCount = 1,
            subtotal = money,
            discountTotal = AdminMoneyAmount.zero(),
            taxTotal = AdminMoneyAmount.zero(),
            grandTotal = money,
            paidTotal = money,
            receivableTotal = AdminMoneyAmount.zero(),
            byOperationalStatus = listOf(AdminStatusCount("closed", 1)),
            byPaymentStatus = listOf(AdminStatusCount("paid", 1)),
            byDocumentStatus = listOf(AdminStatusCount("authorized", 1)),
            topItems = emptyList(),
        )

    override fun cashSummary(command: GetAdminCashSummaryReportCommand): AdminCashSummaryReport =
        AdminCashSummaryReport(
            organizationId = command.organizationId,
            branchId = command.branchId,
            from = command.from,
            to = command.to,
            openSessionCount = 0,
            closedSessionCount = 0,
            movementCount = 0,
            cashInTotal = AdminMoneyAmount.zero(),
            cashOutTotal = AdminMoneyAmount.zero(),
            netCashMovement = AdminMoneyAmount.zero(),
            expectedOpenCashTotal = AdminMoneyAmount.zero(),
            countedClosedCashTotal = AdminMoneyAmount.zero(),
            differenceClosedCashTotal = AdminMoneyAmount.zero(),
            byMovementType = emptyList(),
        )

    override fun taxSummary(command: GetAdminTaxSummaryReportCommand): AdminTaxSummaryReport = AdminTaxSummaryReport(
        organizationId = command.organizationId,
        branchId = command.branchId,
        activityId = command.activityId,
        from = command.from,
        to = command.to,
        documentCount = 1,
        authorizedDocumentCount = 1,
        documentGrandTotal = money,
        taxTotal = AdminMoneyAmount.zero(),
        byTaxRate = emptyList(),
    )
}
