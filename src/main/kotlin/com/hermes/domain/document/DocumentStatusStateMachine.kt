package com.hermes.domain.document

import com.hermes.domain.shared.StateTransitionValidator

object DocumentStatusStateMachine {

    private val validator = StateTransitionValidator(
        entityName = "document",
        transitions = mapOf(
            DocumentStatus.NOT_REQUIRED to setOf(
                DocumentStatus.DRAFT
            ),
            DocumentStatus.DRAFT to setOf(
                DocumentStatus.GENERATED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.GENERATED to setOf(
                DocumentStatus.VALIDATED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.VALIDATED to setOf(
                DocumentStatus.SIGNED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.SIGNED to setOf(
                DocumentStatus.SENT,
                DocumentStatus.ERROR
            ),
            DocumentStatus.SENT to setOf(
                DocumentStatus.RECEIVED,
                DocumentStatus.RETURNED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.RECEIVED to setOf(
                DocumentStatus.AUTHORIZED,
                DocumentStatus.REJECTED,
                DocumentStatus.RETURNED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.AUTHORIZED to setOf(
                DocumentStatus.CANCELLATION_REQUESTED
            ),
            DocumentStatus.REJECTED to setOf(
                DocumentStatus.DRAFT
            ),
            DocumentStatus.RETURNED to setOf(
                DocumentStatus.DRAFT
            ),
            DocumentStatus.ERROR to setOf(
                DocumentStatus.DRAFT
            ),
            DocumentStatus.CANCELLATION_REQUESTED to setOf(
                DocumentStatus.PENDING_CANCELLATION,
                DocumentStatus.AUTHORIZED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.PENDING_CANCELLATION to setOf(
                DocumentStatus.CANCELED,
                DocumentStatus.AUTHORIZED,
                DocumentStatus.ERROR
            ),
            DocumentStatus.CANCELED to emptySet()
        )
    )

    fun canTransition(
        from: DocumentStatus,
        to: DocumentStatus
    ): Boolean {
        return validator.canTransition(from, to)
    }

    fun assertCanTransition(
        from: DocumentStatus,
        to: DocumentStatus
    ) {
        validator.assertCanTransition(from, to)
    }
}
