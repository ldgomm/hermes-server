package com.hermes.application.payments

import com.hermes.domain.cash.CashSession
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable

interface PaymentRepository {
    fun create(payment: Payment)
    fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment>
}

interface PaymentCashSessionRepository {
    fun findOpenByOrganization(organizationId: String): CashSession?
    fun update(session: CashSession)
}

interface ReceivableRepository {
    fun create(receivable: Receivable)
    fun update(receivable: Receivable)
    fun findBySaleId(organizationId: String, saleId: String): Receivable?
}
