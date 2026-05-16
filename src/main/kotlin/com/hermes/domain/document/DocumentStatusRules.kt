package com.hermes.domain.document

import com.hermes.domain.shared.DomainRuleViolation

object DocumentStatusRules {

    fun assertCanCreateDraft(status: DocumentStatus) {
        if (status != DocumentStatus.NOT_REQUIRED) {
            throw DomainRuleViolation("Only a not-required document can become draft.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.DRAFT
        )
    }

    fun assertCanGenerate(status: DocumentStatus) {
        if (status != DocumentStatus.DRAFT) {
            throw DomainRuleViolation("Only a draft document can be generated.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.GENERATED
        )
    }

    fun assertCanValidate(status: DocumentStatus) {
        if (status != DocumentStatus.GENERATED) {
            throw DomainRuleViolation("Only a generated document can be validated.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.VALIDATED
        )
    }

    fun assertCanSign(status: DocumentStatus) {
        if (status != DocumentStatus.VALIDATED) {
            throw DomainRuleViolation("Only a validated document can be signed.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.SIGNED
        )
    }

    fun assertCanSend(status: DocumentStatus) {
        if (status != DocumentStatus.SIGNED) {
            throw DomainRuleViolation("Only a signed document can be sent.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.SENT
        )
    }

    fun assertCanMarkReceived(status: DocumentStatus) {
        if (status != DocumentStatus.SENT) {
            throw DomainRuleViolation("Only a sent document can be marked as received.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.RECEIVED
        )
    }

    fun assertCanAuthorize(status: DocumentStatus) {
        if (status != DocumentStatus.RECEIVED) {
            throw DomainRuleViolation("Only a received document can be authorized.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.AUTHORIZED
        )
    }

    fun assertCanReject(status: DocumentStatus) {
        if (status != DocumentStatus.RECEIVED) {
            throw DomainRuleViolation("Only a received document can be rejected.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.REJECTED
        )
    }

    fun assertCanReturn(status: DocumentStatus) {
        if (status !in setOf(DocumentStatus.SENT, DocumentStatus.RECEIVED)) {
            throw DomainRuleViolation("Only a sent or received document can be returned.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.RETURNED
        )
    }

    fun assertCanRestartAfterFailure(status: DocumentStatus) {
        if (status !in setOf(DocumentStatus.REJECTED, DocumentStatus.RETURNED, DocumentStatus.ERROR)) {
            throw DomainRuleViolation("Only rejected, returned or errored documents can restart as draft.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.DRAFT
        )
    }

    fun assertCanMarkError(status: DocumentStatus) {
        if (status in setOf(DocumentStatus.AUTHORIZED, DocumentStatus.CANCELED)) {
            throw DomainRuleViolation("Authorized or canceled documents cannot be marked as error.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.ERROR
        )
    }

    fun assertCanRequestCancellation(status: DocumentStatus) {
        if (status != DocumentStatus.AUTHORIZED) {
            throw DomainRuleViolation("Only an authorized document can request cancellation.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.CANCELLATION_REQUESTED
        )
    }

    fun assertCanMarkCancellationPending(status: DocumentStatus) {
        if (status != DocumentStatus.CANCELLATION_REQUESTED) {
            throw DomainRuleViolation("Only a cancellation-requested document can become pending cancellation.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.PENDING_CANCELLATION
        )
    }

    fun assertCanCancel(status: DocumentStatus) {
        if (status != DocumentStatus.PENDING_CANCELLATION) {
            throw DomainRuleViolation("Only a pending-cancellation document can be canceled.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.CANCELED
        )
    }

    fun assertCanRestoreAuthorizedAfterCancellationFailure(status: DocumentStatus) {
        if (status !in setOf(DocumentStatus.CANCELLATION_REQUESTED, DocumentStatus.PENDING_CANCELLATION)) {
            throw DomainRuleViolation("Only documents in cancellation flow can be restored to authorized.")
        }

        DocumentStatusStateMachine.assertCanTransition(
            from = status,
            to = DocumentStatus.AUTHORIZED
        )
    }
}
