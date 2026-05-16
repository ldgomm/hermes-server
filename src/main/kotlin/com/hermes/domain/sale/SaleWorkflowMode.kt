package com.hermes.domain.sale

/**
 * Concrete workflow used by a specific Sale.
 */
enum class SaleWorkflowMode {
    QUICK_SALE,
    COUNTER_ORDER,
    TABLE_ORDER,
    DELIVERY_ORDER,
    RESERVATION,
    APPOINTMENT,
    SERVICE_ORDER,
    RENTAL,
    QUOTE_TO_SALE
}
