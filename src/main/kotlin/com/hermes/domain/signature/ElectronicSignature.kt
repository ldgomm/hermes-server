package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class ElectronicSignature private constructor(
    val id: String,
    val organizationId: String,
    val storageKey: String,
    val passwordSecretRef: String,
    val subject: String,
    val issuer: String,
    val validFrom: Instant,
    val validTo: Instant,
    val status: ElectronicSignatureStatus,
    val uploadedBy: String,
    val uploadedAt: Instant,
    val lastUsedAt: Instant?
) {

    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Electronic signature id cannot be blank.")
        }

        if (organizationId.isBlank()) {
            throw DomainRuleViolation("Electronic signature organization id cannot be blank.")
        }

        if (storageKey.isBlank()) {
            throw DomainRuleViolation("Electronic signature storage key cannot be blank.")
        }

        if (passwordSecretRef.isBlank()) {
            throw DomainRuleViolation("Electronic signature password secret reference cannot be blank.")
        }

        if (subject.isBlank()) {
            throw DomainRuleViolation("Electronic signature subject cannot be blank.")
        }

        if (issuer.isBlank()) {
            throw DomainRuleViolation("Electronic signature issuer cannot be blank.")
        }

        if (uploadedBy.isBlank()) {
            throw DomainRuleViolation("Electronic signature uploadedBy cannot be blank.")
        }

        if (!validFrom.isBefore(validTo)) {
            throw DomainRuleViolation("Electronic signature validFrom must be before validTo.")
        }
    }

    fun effectiveStatus(now: Instant): ElectronicSignatureStatus {
        return ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = status,
            validFrom = validFrom,
            validTo = validTo,
            now = now
        )
    }

    fun assertUsable(now: Instant) {
        ElectronicSignatureRules.assertCanUse(
            storedStatus = status,
            validFrom = validFrom,
            validTo = validTo,
            now = now
        )
    }

    fun markValidated(now: Instant): ElectronicSignature {
        ElectronicSignatureRules.assertCanMarkValid(
            storedStatus = status,
            validFrom = validFrom,
            validTo = validTo,
            now = now
        )

        return copy(status = ElectronicSignatureStatus.VALID)
    }

    fun markUsed(usedAt: Instant): ElectronicSignature {
        assertUsable(usedAt)

        return copy(lastUsedAt = usedAt)
    }

    fun revoke(): ElectronicSignature {
        ElectronicSignatureRules.assertCanRevoke(status)

        return copy(status = ElectronicSignatureStatus.REVOKED)
    }

    fun disable(): ElectronicSignature {
        ElectronicSignatureRules.assertCanDisable(status)

        return copy(status = ElectronicSignatureStatus.DISABLED)
    }

    companion object {
        fun upload(
            id: String,
            organizationId: String,
            storageKey: String,
            passwordSecretRef: String,
            subject: String,
            issuer: String,
            validFrom: Instant,
            validTo: Instant,
            uploadedBy: String,
            uploadedAt: Instant
        ): ElectronicSignature {
            return ElectronicSignature(
                id = id,
                organizationId = organizationId,
                storageKey = storageKey,
                passwordSecretRef = passwordSecretRef,
                subject = subject,
                issuer = issuer,
                validFrom = validFrom,
                validTo = validTo,
                status = ElectronicSignatureStatus.UPLOADED,
                uploadedBy = uploadedBy,
                uploadedAt = uploadedAt,
                lastUsedAt = null
            )
        }
    }
}
