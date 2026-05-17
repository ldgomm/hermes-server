package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Duration
import java.time.Instant

object SignatureCertificateRules {
    fun validityReport(
        storedStatus: ElectronicSignatureStatus,
        validFrom: Instant,
        validTo: Instant,
        now: Instant,
        warningWindow: Duration = Duration.ofDays(30),
    ): SignatureValidityReport {
        val effective = ElectronicSignatureRules.resolveEffectiveStatus(storedStatus, validFrom, validTo, now)
        val days = Duration.between(now, validTo).toDays()
        return SignatureValidityReport(
            storedStatus = storedStatus,
            effectiveStatus = effective,
            validFrom = validFrom,
            validTo = validTo,
            daysUntilExpiration = days,
            expiresSoon = effective == ElectronicSignatureStatus.VALID && !now.plus(warningWindow).isBefore(validTo),
        )
    }

    fun assertFileNameAllowed(fileName: String) {
        val normalized = fileName.trim().lowercase()
        if (normalized.isBlank()) throw DomainRuleViolation("Signature file name cannot be blank.")
        if (!normalized.endsWith(".p12") && !normalized.endsWith(".pfx")) {
            throw DomainRuleViolation("Only .p12 or .pfx signature files are allowed.")
        }
    }

    fun inferCertificateType(fileName: String): SignatureCertificateType {
        assertFileNameAllowed(fileName)
        return when {
            fileName.trim().lowercase().endsWith(".p12") -> SignatureCertificateType.P12
            else -> SignatureCertificateType.PFX
        }
    }
}
