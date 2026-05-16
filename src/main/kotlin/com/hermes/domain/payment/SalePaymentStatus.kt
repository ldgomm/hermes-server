package com.hermes.domain.payment

/**
 * Aggregate payment status of a Sale.
 *
 * This answers: "Is this sale unpaid, partially paid or paid?"
 */
enum class SalePaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERPAID,
    REFUNDED,
    VOIDED
}
