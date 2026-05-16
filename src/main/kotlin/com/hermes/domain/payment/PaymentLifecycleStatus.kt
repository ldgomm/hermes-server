package com.hermes.domain.payment

/**
 * Lifecycle of one payment record.
 *
 * This is NOT the aggregate payment state of a Sale.
 */
enum class PaymentLifecycleStatus {
    DRAFT,
    PENDING,
    CONFIRMED,
    ALLOCATED,
    REVERSED,
    VOIDED,
    FAILED;

    val isEffective: Boolean
        get() = this == CONFIRMED || this == ALLOCATED

    val isTerminal: Boolean
        get() = this in setOf(REVERSED, VOIDED, FAILED)
}
