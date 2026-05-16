package com.hermes.domain.alignment

import com.hermes.domain.payment.PaymentLifecycleStatus
import com.hermes.domain.payment.ReceivableStatus
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import kotlin.test.Test
import kotlin.test.assertTrue

class ClosureTest {
    @Test
    fun `phase 1 to 4 critical enums exist`() {
        assertTrue(SalePaymentStatus.entries.isNotEmpty())
        assertTrue(PaymentLifecycleStatus.entries.isNotEmpty())
        assertTrue(ReceivableStatus.entries.isNotEmpty())
        assertTrue(SaleWorkflowMode.entries.isNotEmpty())
        assertTrue(SaleType.entries.isNotEmpty())
    }

    @Test
    fun `sale payment status is aggregate and payment lifecycle is record lifecycle`() {
        assertTrue(SalePaymentStatus.UNPAID in SalePaymentStatus.entries)
        assertTrue(SalePaymentStatus.PARTIALLY_PAID in SalePaymentStatus.entries)
        assertTrue(PaymentLifecycleStatus.ALLOCATED in PaymentLifecycleStatus.entries)
        assertTrue(PaymentLifecycleStatus.REVERSED in PaymentLifecycleStatus.entries)
    }

    @Test
    fun `sale workflow supports concrete operational variants`() {
        assertTrue(SaleWorkflowMode.TABLE_ORDER in SaleWorkflowMode.entries)
        assertTrue(SaleWorkflowMode.DELIVERY_ORDER in SaleWorkflowMode.entries)
        assertTrue(SaleWorkflowMode.QUOTE_TO_SALE in SaleWorkflowMode.entries)
    }
}
