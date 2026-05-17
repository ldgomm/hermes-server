package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

enum class SignatureCertificateType {
    P12,
    PFX,
}

data class SignatureCertificateMetadata(
    val certificateType: SignatureCertificateType,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: Instant,
    val validTo: Instant,
    val sha256Fingerprint: String,
) {
    init {
        if (subject.isBlank()) throw DomainRuleViolation("Signature certificate subject cannot be blank.")
        if (issuer.isBlank()) throw DomainRuleViolation("Signature certificate issuer cannot be blank.")
        if (serialNumber.isBlank()) throw DomainRuleViolation("Signature certificate serial number cannot be blank.")
        if (!validFrom.isBefore(validTo)) throw DomainRuleViolation("Signature certificate validFrom must be before validTo.")
        if (!FINGERPRINT_PATTERN.matches(sha256Fingerprint)) {
            throw DomainRuleViolation("Signature certificate SHA-256 fingerprint is invalid.")
        }
    }

    fun effectiveStatus(now: Instant): ElectronicSignatureStatus =
        ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = validFrom,
            validTo = validTo,
            now = now,
        )

    companion object {
        private val FINGERPRINT_PATTERN = Regex("^[A-Fa-f0-9]{64}$")
    }
}

data class SignatureValidityReport(
    val storedStatus: ElectronicSignatureStatus,
    val effectiveStatus: ElectronicSignatureStatus,
    val validFrom: Instant,
    val validTo: Instant,
    val daysUntilExpiration: Long,
    val expiresSoon: Boolean,
) {
    val usable: Boolean get() = effectiveStatus == ElectronicSignatureStatus.VALID
}
