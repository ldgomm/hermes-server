package com.hermes.domain.document

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocumentStatusStateMachineTest {

    @Test
    fun `allows normal document flow`() {
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.DRAFT, DocumentStatus.GENERATED))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.GENERATED, DocumentStatus.VALIDATED))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.VALIDATED, DocumentStatus.SIGNED))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.SIGNED, DocumentStatus.SENT))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.SENT, DocumentStatus.RECEIVED))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.RECEIVED, DocumentStatus.AUTHORIZED))
    }

    @Test
    fun `rejects draft document going directly to signed`() {
        assertFalse(
            DocumentStatusStateMachine.canTransition(
                from = DocumentStatus.DRAFT,
                to = DocumentStatus.SIGNED
            )
        )

        assertFailsWith<DomainRuleViolation> {
            DocumentStatusStateMachine.assertCanTransition(
                from = DocumentStatus.DRAFT,
                to = DocumentStatus.SIGNED
            )
        }
    }

    @Test
    fun `rejects signed document going directly to authorized`() {
        assertFalse(
            DocumentStatusStateMachine.canTransition(
                from = DocumentStatus.SIGNED,
                to = DocumentStatus.AUTHORIZED
            )
        )
    }

    @Test
    fun `rejects authorized document going back to draft`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusStateMachine.assertCanTransition(
                from = DocumentStatus.AUTHORIZED,
                to = DocumentStatus.DRAFT
            )
        }
    }

    @Test
    fun `allows failed document to restart as draft`() {
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.REJECTED, DocumentStatus.DRAFT))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.RETURNED, DocumentStatus.DRAFT))
        assertTrue(DocumentStatusStateMachine.canTransition(DocumentStatus.ERROR, DocumentStatus.DRAFT))
    }

    @Test
    fun `allows authorized document to enter cancellation flow`() {
        assertTrue(
            DocumentStatusStateMachine.canTransition(
                from = DocumentStatus.AUTHORIZED,
                to = DocumentStatus.CANCELLATION_REQUESTED
            )
        )

        assertTrue(
            DocumentStatusStateMachine.canTransition(
                from = DocumentStatus.CANCELLATION_REQUESTED,
                to = DocumentStatus.PENDING_CANCELLATION
            )
        )

        assertTrue(
            DocumentStatusStateMachine.canTransition(
                from = DocumentStatus.PENDING_CANCELLATION,
                to = DocumentStatus.CANCELED
            )
        )
    }

    @Test
    fun `rejects canceled document being sent again`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusStateMachine.assertCanTransition(
                from = DocumentStatus.CANCELED,
                to = DocumentStatus.SENT
            )
        }
    }

    @Test
    fun `allows same status as idempotent transition`() {
        DocumentStatusStateMachine.assertCanTransition(
            from = DocumentStatus.SENT,
            to = DocumentStatus.SENT
        )
    }
}
