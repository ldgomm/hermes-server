package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SignatureCertificateRulesTest {
    @Test
    fun `infers p12 certificate type`() {
        assertEquals(SignatureCertificateType.P12, SignatureCertificateRules.inferCertificateType("firma.p12"))
    }

    @Test
    fun `rejects unsupported signature extension`() {
        assertFailsWith<DomainRuleViolation> {
            SignatureCertificateRules.assertFileNameAllowed("firma.txt")
        }
    }

    @Test
    fun `reports signature expiration warning`() {
        val now = Instant.parse("2026-05-01T00:00:00Z")
        val report = SignatureCertificateRules.validityReport(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = Instant.parse("2025-01-01T00:00:00Z"),
            validTo = Instant.parse("2026-05-20T00:00:00Z"),
            now = now,
        )

        assertEquals(ElectronicSignatureStatus.VALID, report.effectiveStatus)
        assertTrue(report.expiresSoon)
    }
}
