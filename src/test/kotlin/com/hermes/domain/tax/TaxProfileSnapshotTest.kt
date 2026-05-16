package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxProfileSnapshotTest {
    @Test
    fun `captures complete tax profile snapshot`() {
        val profile = TaxProfile.ivaFull(TaxRate.iva15())
        val snapshot = TaxProfileSnapshot.from(profile, LocalDate.of(2026, 5, 15))

        assertEquals("taxp_iva_full_current", snapshot.taxProfileId)
        assertEquals("iva_current_full", snapshot.code)
        assertEquals(TaxTreatment.IVA_FULL, snapshot.treatment)
        assertEquals(BigDecimal("15.00"), snapshot.ratePercent)
        assertEquals("2", snapshot.sriTaxCode)
        assertEquals("4", snapshot.sriRateCode)
        assertEquals("admin_tax_configuration", snapshot.source)
    }

    @Test
    fun `snapshot does not change when profile rate changes later`() {
        val originalRate = TaxRate.iva15()
        val profile = TaxProfile.ivaFull(originalRate)
        val snapshot = TaxProfileSnapshot.from(profile, LocalDate.of(2026, 5, 15))

        val changedProfile = profile.copy(
            rate = originalRate.copy(ratePercent = BigDecimal("16.00"), sriRateCode = "5"),
        )

        assertEquals(BigDecimal("16.00"), changedProfile.rate!!.ratePercent)
        assertEquals(BigDecimal("15.00"), snapshot.ratePercent)
        assertEquals("4", snapshot.sriRateCode)
    }

    @Test
    fun `rejects deprecated profile for new snapshot`() {
        assertFailsWith<DomainRuleViolation> {
            TaxProfileSnapshot.from(
                TaxProfile.ivaFull().copy(status = TaxProfileStatus.DEPRECATED),
                LocalDate.of(2026, 5, 15),
            )
        }
    }

    @Test
    fun `rejects rate outside effective range`() {
        val rate = TaxRate.iva15(
            effectiveFrom = LocalDate.of(2026, 1, 1),
        ).copy(effectiveTo = LocalDate.of(2026, 1, 31))

        assertFailsWith<DomainRuleViolation> {
            TaxProfileSnapshot.from(
                TaxProfile.ivaFull(rate),
                LocalDate.of(2026, 5, 15),
            )
        }
    }
}
