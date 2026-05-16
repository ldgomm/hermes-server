package com.hermes.domain.payment

enum class ReceivableStatus {
    NOT_APPLICABLE,
    PENDING_RECEIVABLE,
    PARTIALLY_COLLECTED,
    SETTLED,
    OVERDUE,
    WRITTEN_OFF,
    CANCELED
}
