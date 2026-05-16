package com.hermes.domain.document

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DocumentStatusRulesTest {

    @Test
    fun `allows strict normal command flow`() {
        DocumentStatusRules.assertCanCreateDraft(DocumentStatus.NOT_REQUIRED)
        DocumentStatusRules.assertCanGenerate(DocumentStatus.DRAFT)
        DocumentStatusRules.assertCanValidate(DocumentStatus.GENERATED)
        DocumentStatusRules.assertCanSign(DocumentStatus.VALIDATED)
        DocumentStatusRules.assertCanSend(DocumentStatus.SIGNED)
        DocumentStatusRules.assertCanMarkReceived(DocumentStatus.SENT)
        DocumentStatusRules.assertCanAuthorize(DocumentStatus.RECEIVED)
    }

    @Test
    fun `rejects generating document that is not draft`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanGenerate(DocumentStatus.GENERATED)
        }
    }

    @Test
    fun `rejects validating document that is not generated`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanValidate(DocumentStatus.DRAFT)
        }
    }

    @Test
    fun `rejects signing document that is not validated`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanSign(DocumentStatus.GENERATED)
        }
    }

    @Test
    fun `rejects sending document that is not signed`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanSend(DocumentStatus.VALIDATED)
        }
    }

    @Test
    fun `rejects authorizing document that is not received`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanAuthorize(DocumentStatus.SENT)
        }
    }

    @Test
    fun `rejects rejected document becoming authorized without restart`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanAuthorize(DocumentStatus.REJECTED)
        }
    }

    @Test
    fun `allows rejected returned and error documents to restart as draft`() {
        DocumentStatusRules.assertCanRestartAfterFailure(DocumentStatus.REJECTED)
        DocumentStatusRules.assertCanRestartAfterFailure(DocumentStatus.RETURNED)
        DocumentStatusRules.assertCanRestartAfterFailure(DocumentStatus.ERROR)
    }

    @Test
    fun `rejects authorized document restarting as draft`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanRestartAfterFailure(DocumentStatus.AUTHORIZED)
        }
    }

    @Test
    fun `allows return only from sent or received`() {
        DocumentStatusRules.assertCanReturn(DocumentStatus.SENT)
        DocumentStatusRules.assertCanReturn(DocumentStatus.RECEIVED)
    }

    @Test
    fun `rejects return from draft`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanReturn(DocumentStatus.DRAFT)
        }
    }

    @Test
    fun `allows cancellation request only from authorized`() {
        DocumentStatusRules.assertCanRequestCancellation(DocumentStatus.AUTHORIZED)
    }

    @Test
    fun `rejects cancellation request from signed`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanRequestCancellation(DocumentStatus.SIGNED)
        }
    }

    @Test
    fun `allows cancellation flow`() {
        DocumentStatusRules.assertCanRequestCancellation(DocumentStatus.AUTHORIZED)
        DocumentStatusRules.assertCanMarkCancellationPending(DocumentStatus.CANCELLATION_REQUESTED)
        DocumentStatusRules.assertCanCancel(DocumentStatus.PENDING_CANCELLATION)
    }

    @Test
    fun `rejects canceling without pending cancellation`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanCancel(DocumentStatus.AUTHORIZED)
        }
    }

    @Test
    fun `rejects marking canceled document as error`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanMarkError(DocumentStatus.CANCELED)
        }
    }

    @Test
    fun `rejects marking authorized document as error`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanMarkError(DocumentStatus.AUTHORIZED)
        }
    }

    @Test
    fun `allows restoring authorized status after cancellation failure`() {
        DocumentStatusRules.assertCanRestoreAuthorizedAfterCancellationFailure(
            DocumentStatus.CANCELLATION_REQUESTED
        )

        DocumentStatusRules.assertCanRestoreAuthorizedAfterCancellationFailure(
            DocumentStatus.PENDING_CANCELLATION
        )
    }

    @Test
    fun `rejects restoring authorized from unrelated status`() {
        assertFailsWith<DomainRuleViolation> {
            DocumentStatusRules.assertCanRestoreAuthorizedAfterCancellationFailure(
                DocumentStatus.REJECTED
            )
        }
    }
}
