package com.hermes.domain.signature

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ElectronicSignatureRulesTest {

    private val validFrom = Instant.parse("2026-01-01T00:00:00Z")
    private val validTo = Instant.parse("2028-01-01T00:00:00Z")
    private val validNow = Instant.parse("2026-05-15T20:30:00Z")

    @Test
    fun `resolves valid signature inside valid period`() {
        val status = ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = validFrom,
            validTo = validTo,
            now = validNow
        )

        assertEquals(ElectronicSignatureStatus.VALID, status)
    }

    @Test
    fun `resolves uploaded signature as valid inside valid period`() {
        val status = ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.UPLOADED,
            validFrom = validFrom,
            validTo = validTo,
            now = validNow
        )

        assertEquals(ElectronicSignatureStatus.VALID, status)
    }

    @Test
    fun `resolves not yet valid before validFrom`() {
        val status = ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = validFrom,
            validTo = validTo,
            now = Instant.parse("2025-12-31T23:59:59Z")
        )

        assertEquals(ElectronicSignatureStatus.NOT_YET_VALID, status)
    }

    @Test
    fun `resolves expired when now is equal to validTo`() {
        val status = ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = validFrom,
            validTo = validTo,
            now = validTo
        )

        assertEquals(ElectronicSignatureStatus.EXPIRED, status)
    }

    @Test
    fun `keeps revoked status even inside valid period`() {
        val status = ElectronicSignatureRules.resolveEffectiveStatus(
            storedStatus = ElectronicSignatureStatus.REVOKED,
            validFrom = validFrom,
            validTo = validTo,
            now = validNow
        )

        assertEquals(ElectronicSignatureStatus.REVOKED, status)
    }

    @Test
    fun `allows valid signature to be used`() {
        ElectronicSignatureRules.assertCanUse(
            storedStatus = ElectronicSignatureStatus.VALID,
            validFrom = validFrom,
            validTo = validTo,
            now = validNow
        )
    }

    @Test
    fun `rejects expired signature usage`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignatureRules.assertCanUse(
                storedStatus = ElectronicSignatureStatus.VALID,
                validFrom = validFrom,
                validTo = validTo,
                now = Instant.parse("2028-01-01T00:00:00Z")
            )
        }
    }

    @Test
    fun `rejects revoked signature usage`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignatureRules.assertCanUse(
                storedStatus = ElectronicSignatureStatus.REVOKED,
                validFrom = validFrom,
                validTo = validTo,
                now = validNow
            )
        }
    }

    @Test
    fun `rejects invalid period`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignatureRules.resolveEffectiveStatus(
                storedStatus = ElectronicSignatureStatus.VALID,
                validFrom = validTo,
                validTo = validFrom,
                now = validNow
            )
        }
    }

    @Test
    fun `allows uploaded signature to be marked valid inside valid period`() {
        ElectronicSignatureRules.assertCanMarkValid(
            storedStatus = ElectronicSignatureStatus.UPLOADED,
            validFrom = validFrom,
            validTo = validTo,
            now = validNow
        )
    }

    @Test
    fun `rejects expired signature being marked valid`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignatureRules.assertCanMarkValid(
                storedStatus = ElectronicSignatureStatus.UPLOADED,
                validFrom = validFrom,
                validTo = validTo,
                now = Instant.parse("2028-01-01T00:00:00Z")
            )
        }
    }

    @Test
    fun `rejects already revoked signature being revoked again`() {
        assertFailsWith<DomainRuleViolation> {
            ElectronicSignatureRules.assertCanRevoke(ElectronicSignatureStatus.REVOKED)
        }
    }
}
