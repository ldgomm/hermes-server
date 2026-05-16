package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ElectronicSignatureTest {

    private val validFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val validTo = Instant.parse("2028-01-01T00:00:00Z")
    private val now = Instant.parse("2026-05-15T20:30:00Z")

    private fun signature(): ElectronicSignature {
        return ElectronicSignature.upload(
            id = "sig_1",
            organizationId = "org_1",
            storageKey = "encrypted/signatures/org_1/sig_1.p12",
            passwordSecretRef = "secret_ref_1",
            subject = "ALTOS DEL MURCO",
            issuer = "TEST ISSUER",
            validFrom = validFrom,
            validTo = validTo,
            uploadedBy = "usr_1",
            uploadedAt = now
        )
    }

    @Test
    fun `uploads signature as uploaded`() {
        val signature = signature()

        assertEquals(ElectronicSignatureStatus.UPLOADED, signature.status)
    }

    @Test
    fun `marks uploaded signature as validated`() {
        val validated = signature().markValidated(now)

        assertEquals(ElectronicSignatureStatus.VALID, validated.status)
    }

    @Test
    fun `marks valid signature as used`() {
        val used = signature()
            .markValidated(now)
            .markUsed(now)

        assertEquals(now, used.lastUsedAt)
    }

    @Test
    fun `rejects use after expiration`() {
        val expiredNow = Instant.parse("2028-01-01T00:00:00Z")
        val validated = signature().markValidated(now)

        assertFailsWith<DomainRuleViolation> {
            validated.markUsed(expiredNow)
        }
    }

    @Test
    fun `revoked signature cannot be used`() {
        val revoked = signature()
            .markValidated(now)
            .revoke()

        assertFailsWith<DomainRuleViolation> {
            revoked.markUsed(now)
        }
    }

    @Test
    fun `rejects blank storage key`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignature.upload(
                id = "sig_1",
                organizationId = "org_1",
                storageKey = "",
                passwordSecretRef = "secret_ref_1",
                subject = "ALTOS DEL MURCO",
                issuer = "TEST ISSUER",
                validFrom = validFrom,
                validTo = validTo,
                uploadedBy = "usr_1",
                uploadedAt = now
            )
        }
    }

    @Test
    fun `rejects invalid validity range`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignature.upload(
                id = "sig_1",
                organizationId = "org_1",
                storageKey = "encrypted/signatures/org_1/sig_1.p12",
                passwordSecretRef = "secret_ref_1",
                subject = "ALTOS DEL MURCO",
                issuer = "TEST ISSUER",
                validFrom = validTo,
                validTo = validFrom,
                uploadedBy = "usr_1",
                uploadedAt = now
            )
        }
    }
}
