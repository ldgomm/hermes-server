package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation

enum class ElectronicDocumentStatus {
    DRAFT,
    READY_TO_ISSUE,
    ACCESS_KEY_GENERATED,
    XML_GENERATED,
    XSD_VALIDATED,
    XSD_INVALID,
    SIGNED,
    SIGNATURE_FAILED,
    SUBMITTED_TO_RECEPTION,
    RECEIVED_BY_SRI,
    RETURNED_BY_SRI,
    AUTHORIZATION_PENDING,
    AUTHORIZED,
    NOT_AUTHORIZED,
    DELIVERY_PENDING,
    DELIVERED,
    DELIVERY_FAILED,
    ERROR,
    CANCELLATION_REQUESTED,
    CANCELED;

    val isTerminal: Boolean
        get() = this in setOf(DELIVERED, NOT_AUTHORIZED, RETURNED_BY_SRI, CANCELED, ERROR)

    val isSriFinal: Boolean
        get() = this in setOf(AUTHORIZED, NOT_AUTHORIZED)

    fun canTransitionTo(target: ElectronicDocumentStatus): Boolean {
        if (this == target) return true
        if (this.isTerminal && target !in recoveryTargets) return false
        return target in allowedTransitions.getValue(this)
    }

    fun assertCanTransitionTo(target: ElectronicDocumentStatus) {
        if (!canTransitionTo(target)) {
            throw DomainRuleViolation("Electronic document cannot transition from $this to $target.")
        }
    }

    companion object {
        private val recoveryTargets = setOf(READY_TO_ISSUE, DELIVERY_PENDING)

        private val allowedTransitions: Map<ElectronicDocumentStatus, Set<ElectronicDocumentStatus>> = mapOf(
            DRAFT to setOf(READY_TO_ISSUE, ERROR),
            READY_TO_ISSUE to setOf(ACCESS_KEY_GENERATED, ERROR),
            ACCESS_KEY_GENERATED to setOf(XML_GENERATED, ERROR),
            XML_GENERATED to setOf(XSD_VALIDATED, XSD_INVALID, ERROR),
            XSD_INVALID to setOf(XML_GENERATED, ERROR),
            XSD_VALIDATED to setOf(SIGNED, SIGNATURE_FAILED, ERROR),
            SIGNATURE_FAILED to setOf(XSD_VALIDATED, ERROR),
            SIGNED to setOf(SUBMITTED_TO_RECEPTION, ERROR),
            SUBMITTED_TO_RECEPTION to setOf(RECEIVED_BY_SRI, RETURNED_BY_SRI, ERROR),
            RECEIVED_BY_SRI to setOf(AUTHORIZATION_PENDING, AUTHORIZED, NOT_AUTHORIZED, ERROR),
            AUTHORIZATION_PENDING to setOf(AUTHORIZED, NOT_AUTHORIZED, AUTHORIZATION_PENDING, ERROR),
            AUTHORIZED to setOf(DELIVERY_PENDING, CANCELLATION_REQUESTED, ERROR),
            NOT_AUTHORIZED to setOf(READY_TO_ISSUE, ERROR),
            RETURNED_BY_SRI to setOf(READY_TO_ISSUE, ERROR),
            DELIVERY_PENDING to setOf(DELIVERED, DELIVERY_FAILED, ERROR),
            DELIVERY_FAILED to setOf(DELIVERY_PENDING, ERROR),
            DELIVERED to setOf(CANCELLATION_REQUESTED),
            CANCELLATION_REQUESTED to setOf(CANCELED, AUTHORIZED, ERROR),
            CANCELED to emptySet(),
            ERROR to setOf(READY_TO_ISSUE, DELIVERY_PENDING),
        )

        fun fromReceptionStatus(status: SriReceptionStatus): ElectronicDocumentStatus =
            when (status) {
                SriReceptionStatus.RECEIVED -> RECEIVED_BY_SRI
                SriReceptionStatus.RETURNED -> RETURNED_BY_SRI
            }

        fun fromAuthorizationStatus(status: SriAuthorizationStatus): ElectronicDocumentStatus =
            when (status) {
                SriAuthorizationStatus.AUTHORIZED -> AUTHORIZED
                SriAuthorizationStatus.NOT_AUTHORIZED -> NOT_AUTHORIZED
                SriAuthorizationStatus.PROCESSING -> AUTHORIZATION_PENDING
            }
    }
}
