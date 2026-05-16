package com.hermes.domain.payment

enum class CollectionStatus {
    NOT_REQUIRED,
    PENDING,
    PARTIALLY_COLLECTED,
    COLLECTED,
    OVERDUE,
    WRITTEN_OFF,
    VOIDED
}
