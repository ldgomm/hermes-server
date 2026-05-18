package com.hermes.application.payments

import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashSession
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable
import com.hermes.domain.sale.Sale

data class PaymentSettlement(
    val payment: Payment,
    val sale: Sale,
    val cashSession: CashSession? = null,
    val cashMovement: CashMovement? = null,
    val receivable: Receivable? = null,
)

interface PaymentSettlementRepository {
    fun persistPaymentSettlement(settlement: PaymentSettlement)
}

interface PaymentRepository {
    fun create(payment: Payment)
    fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment>
}

interface PaymentCashSessionRepository {
    fun create(session: CashSession) {
        throw UnsupportedOperationException("Cash session create is not implemented by this repository.")
    }

    fun findById(organizationId: String, cashSessionId: String): CashSession? = null

    fun findOpenByOrganization(organizationId: String): CashSession?

    fun findOpenByBranch(organizationId: String, branchId: String): CashSession? = findOpenByOrganization(organizationId)

    fun update(session: CashSession)
}

interface CashMovementRepository {
    fun create(movement: CashMovement)
    fun findByCashSession(organizationId: String, cashSessionId: String): List<CashMovement> = emptyList()
}

interface ReceivableRepository {
    fun create(receivable: Receivable)
    fun update(receivable: Receivable)
    fun findById(organizationId: String, receivableId: String): Receivable? = null
    fun findBySaleId(organizationId: String, saleId: String): Receivable?
}

class DirectPaymentSettlementRepository(
    private val paymentRepository: PaymentRepository,
    private val saleRepository: com.hermes.application.sales.OperationalSaleRepository,
    private val cashSessionRepository: PaymentCashSessionRepository,
    private val cashMovementRepository: CashMovementRepository,
    private val receivableRepository: ReceivableRepository,
) : PaymentSettlementRepository {
    override fun persistPaymentSettlement(settlement: PaymentSettlement) {
        paymentRepository.create(settlement.payment)
        saleRepository.update(settlement.sale)
        settlement.cashMovement?.let(cashMovementRepository::create)
        settlement.cashSession?.let(cashSessionRepository::update)
        settlement.receivable?.let(receivableRepository::create)
    }
}

object NoopCashMovementRepository : CashMovementRepository {
    override fun create(movement: CashMovement) = Unit
}
