package com.hermes.domain.payment

enum class PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERPAID,
    REFUNDED,
    VOIDED
}
