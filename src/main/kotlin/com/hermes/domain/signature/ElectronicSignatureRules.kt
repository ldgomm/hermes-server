package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

object ElectronicSignatureRules {

    fun resolveEffectiveStatus(
        storedStatus: ElectronicSignatureStatus,
        validFrom: Instant,
        validTo: Instant,
        now: Instant
    ): ElectronicSignatureStatus {
        assertValidPeriod(validFrom, validTo)

        if (storedStatus in setOf(
                ElectronicSignatureStatus.REVOKED,
                ElectronicSignatureStatus.INVALID,
                ElectronicSignatureStatus.DISABLED
            )
        ) {
            return storedStatus
        }

        if (now.isBefore(validFrom)) {
            return ElectronicSignatureStatus.NOT_YET_VALID
        }

        if (!now.isBefore(validTo)) {
            return ElectronicSignatureStatus.EXPIRED
        }

        return ElectronicSignatureStatus.VALID
    }

    fun assertCanUse(
        storedStatus: ElectronicSignatureStatus,
        validFrom: Instant,
        validTo: Instant,
        now: Instant
    ) {
        val effectiveStatus = resolveEffectiveStatus(
            storedStatus = storedStatus,
            validFrom = validFrom,
            validTo = validTo,
            now = now
        )

        if (effectiveStatus != ElectronicSignatureStatus.VALID) {
            throw DomainRuleViolation("Electronic signature cannot be used with effective status $effectiveStatus.")
        }
    }

    fun assertCanMarkValid(
        storedStatus: ElectronicSignatureStatus,
        validFrom: Instant,
        validTo: Instant,
        now: Instant
    ) {
        assertValidPeriod(validFrom, validTo)

        if (storedStatus !in setOf(ElectronicSignatureStatus.UPLOADED, ElectronicSignatureStatus.INVALID)) {
            throw DomainRuleViolation("Only uploaded or invalid signatures can be marked as valid after validation.")
        }

        if (now.isBefore(validFrom)) {
            throw DomainRuleViolation("Electronic signature is not valid yet.")
        }

        if (!now.isBefore(validTo)) {
            throw DomainRuleViolation("Expired electronic signature cannot be marked as valid.")
        }
    }

    fun assertCanRevoke(status: ElectronicSignatureStatus) {
        if (status == ElectronicSignatureStatus.REVOKED) {
            throw DomainRuleViolation("Electronic signature is already revoked.")
        }
    }

    fun assertCanDisable(status: ElectronicSignatureStatus) {
        if (status in setOf(ElectronicSignatureStatus.REVOKED, ElectronicSignatureStatus.DISABLED)) {
            throw DomainRuleViolation("Electronic signature cannot be disabled from status $status.")
        }
    }

    fun assertCanReplace(status: ElectronicSignatureStatus) {
        if (status == ElectronicSignatureStatus.REVOKED) {
            throw DomainRuleViolation("Revoked electronic signature cannot be replaced in-place. Upload a new signature.")
        }
    }

    private fun assertValidPeriod(
        validFrom: Instant,
        validTo: Instant
    ) {
        if (!validFrom.isBefore(validTo)) {
            throw DomainRuleViolation("Electronic signature validFrom must be before validTo.")
        }
    }
}
