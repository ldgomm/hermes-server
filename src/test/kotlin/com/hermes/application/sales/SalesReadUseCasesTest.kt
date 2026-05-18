package com.hermes.application.sales

import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SalesReadUseCasesTest {
    private val now = Instant.parse("2026-05-18T12:00:00Z")

    private val repository = FakeSalesReadRepository(
        sales = listOf(
            item(
                id = "sale_1",
                status = SaleOperationalStatus.CONFIRMED,
                paymentStatus = SalePaymentStatus.UNPAID,
                total = "10.00",
                paid = "0.00",
            ),
            item(
                id = "sale_2",
                status = SaleOperationalStatus.CLOSED,
                paymentStatus = SalePaymentStatus.PAID,
                total = "20.00",
                paid = "20.00",
            ),
        )
    )

    @Test
    fun `search requires sales view permission`() {
        assertFailsWith<DomainRuleViolation> {
            SearchSalesReadUseCase(repository).execute(
                SalesSearchCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = emptySet(),
                )
            )
        }
    }

    @Test
    fun `pending sales returns operationally open sales`() {
        val result = ListPendingSalesUseCase(repository).execute(
            PendingSalesCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                now = now,
            )
        )

        assertEquals(listOf("sale_1"), result.sales.map { it.id })
    }

    @Test
    fun `summary aggregates totals`() {
        val result = GetSalesDaySummaryUseCase(repository).execute(
            SalesDaySummaryCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                from = Instant.parse("2026-05-18T00:00:00Z"),
                to = Instant.parse("2026-05-19T00:00:00Z"),
            )
        )

        assertEquals(2, result.totalSalesCount)
        assertEquals("30.00", result.grandTotal.amount.toPlainString())
        assertEquals("20.00", result.paidTotal.amount.toPlainString())
        assertEquals("10.00", result.receivableTotal.amount.toPlainString())
    }

    private fun item(
        id: String,
        status: SaleOperationalStatus,
        paymentStatus: SalePaymentStatus,
        total: String,
        paid: String,
    ): SalesListItem {
        val grandTotal = usd(total)
        val paidAmount = usd(paid)
        return SalesListItem(
            id = id,
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            saleNumber = id,
            saleType = SaleType.SALE,
            customerId = null,
            customerDisplayName = "Consumidor final",
            operationalStatus = status,
            paymentStatus = paymentStatus,
            itemCount = 1,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            receivableAmount = grandTotal - paidAmount,
            dueAt = null,
            cashSessionId = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun usd(amount: String): Money =
        Money.of(BigDecimal(amount), CurrencyCode("USD"))

    private class FakeSalesReadRepository(
        private val sales: List<SalesListItem>,
    ) : SalesReadRepository {
        override fun search(command: SalesSearchCommand): List<SalesListItem> = sales

        override fun findPending(command: PendingSalesCommand): List<SalesListItem> =
            sales.filter { it.isPendingOperationally }

        override fun summarizeDay(command: SalesDaySummaryCommand): SalesDaySummaryResult {
            val currency = CurrencyCode("USD")
            val zero = Money.zero(currency)

            fun sum(selector: (SalesListItem) -> Money): Money =
                sales.fold(zero) { current, sale -> current + selector(sale) }

            return SalesDaySummaryResult(
                organizationId = command.organizationId,
                branchId = command.branchId,
                activityId = command.activityId,
                from = command.from,
                to = command.to,
                currency = currency.value,
                totalSalesCount = sales.size,
                closedSalesCount = sales.count { it.operationalStatus == SaleOperationalStatus.CLOSED },
                canceledSalesCount = sales.count { it.operationalStatus == SaleOperationalStatus.CANCELED },
                openSalesCount = sales.count { it.isPendingOperationally },
                grossTotal = sum { it.grandTotal },
                discountTotal = zero,
                taxTotal = zero,
                grandTotal = sum { it.grandTotal },
                paidTotal = sum { it.paidAmount },
                receivableTotal = sum { it.receivableAmount },
                byOperationalStatus = sales.groupingBy { it.operationalStatus }.eachCount(),
                byPaymentStatus = sales.groupingBy { it.paymentStatus }.eachCount(),
            )
        }
    }
}
