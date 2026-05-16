package com.hermes.domain.document

enum class DocumentStatus {
    NOT_REQUIRED,
    DRAFT,
    GENERATED,
    VALIDATED,
    SIGNED,
    SENT,
    RECEIVED,
    AUTHORIZED,
    REJECTED,
    RETURNED,
    CANCELLATION_REQUESTED,
    PENDING_CANCELLATION,
    CANCELED,
    ERROR
}
