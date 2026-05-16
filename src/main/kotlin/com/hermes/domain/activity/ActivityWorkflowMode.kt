package com.hermes.domain.activity

/**
 * Workflow modes that an activity is allowed to offer.
 *
 * Do not use this enum as the concrete workflow of a Sale.
 * A restaurant activity may allow ORDER, while a concrete sale may be TABLE_ORDER or DELIVERY_ORDER.
 */
enum class ActivityWorkflowMode {
    QUICK_SALE,
    ORDER,
    RESERVATION,
    SERVICE_ORDER,
    RENTAL
}
