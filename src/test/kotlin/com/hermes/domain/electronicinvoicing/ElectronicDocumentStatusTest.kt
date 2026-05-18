package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class ElectronicDocumentStatusTest {
    @Test
    fun `allows happy path transitions`() {
        assertTrue(ElectronicDocumentStatus.DRAFT.canTransitionTo(ElectronicDocumentStatus.READY_TO_ISSUE))
        assertTrue(ElectronicDocumentStatus.READY_TO_ISSUE.canTransitionTo(ElectronicDocumentStatus.ACCESS_KEY_GENERATED))
        assertTrue(ElectronicDocumentStatus.ACCESS_KEY_GENERATED.canTransitionTo(ElectronicDocumentStatus.XML_GENERATED))
        assertTrue(ElectronicDocumentStatus.XML_GENERATED.canTransitionTo(ElectronicDocumentStatus.XSD_VALIDATED))
        assertTrue(ElectronicDocumentStatus.XSD_VALIDATED.canTransitionTo(ElectronicDocumentStatus.SIGNED))
        assertTrue(ElectronicDocumentStatus.SIGNED.canTransitionTo(ElectronicDocumentStatus.SUBMITTED_TO_RECEPTION))
        assertTrue(ElectronicDocumentStatus.SUBMITTED_TO_RECEPTION.canTransitionTo(ElectronicDocumentStatus.RECEIVED_BY_SRI))
        assertTrue(ElectronicDocumentStatus.RECEIVED_BY_SRI.canTransitionTo(ElectronicDocumentStatus.AUTHORIZATION_PENDING))
        assertTrue(ElectronicDocumentStatus.AUTHORIZATION_PENDING.canTransitionTo(ElectronicDocumentStatus.AUTHORIZED))
        assertTrue(ElectronicDocumentStatus.AUTHORIZED.canTransitionTo(ElectronicDocumentStatus.DELIVERY_PENDING))
        assertTrue(ElectronicDocumentStatus.DELIVERY_PENDING.canTransitionTo(ElectronicDocumentStatus.DELIVERED))
    }

    @Test
    fun `rejects impossible transition`() {
        assertFalse(ElectronicDocumentStatus.DRAFT.canTransitionTo(ElectronicDocumentStatus.AUTHORIZED))

        assertFailsWith<DomainRuleViolation> {
            ElectronicDocumentStatus.DRAFT.assertCanTransitionTo(ElectronicDocumentStatus.AUTHORIZED)
        }
    }

    @Test
    fun `maps SRI statuses to internal status`() {
        assertEquals(
            ElectronicDocumentStatus.RECEIVED_BY_SRI,
            ElectronicDocumentStatus.fromReceptionStatus(SriReceptionStatus.RECEIVED),
        )
        assertEquals(
            ElectronicDocumentStatus.AUTHORIZATION_PENDING,
            ElectronicDocumentStatus.fromAuthorizationStatus(SriAuthorizationStatus.PROCESSING),
        )
        assertEquals(
            ElectronicDocumentStatus.AUTHORIZED,
            ElectronicDocumentStatus.fromAuthorizationStatus(SriAuthorizationStatus.AUTHORIZED),
        )
    }
}
