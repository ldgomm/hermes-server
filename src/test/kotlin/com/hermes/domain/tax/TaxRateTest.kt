package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaxRateTest {
    @Test
    fun `creates active tax rate`() {
        assertTrue(TaxFixtures.iva13Rate.isEffectiveAt(TaxFixtures.now))
    }

    @Test
    fun `rejects negative tax rate`() {
        assertFailsWith<DomainRuleViolation> {
            TaxRate.of(
                id = "taxr_bad",
                code = "bad_rate",
                name = "Bad",
                rate = "-1.0000",
                legalBasis = "Test",
                effectiveFrom = TaxFixtures.now,
            )
        }
    }

    @Test
    fun `rejects inactive rate usage`() {
        val rate = TaxFixtures.iva13Rate.copy(status = TaxRateStatus.DEPRECATED)
        assertFailsWith<DomainRuleViolation> { rate.assertUsableAt(TaxFixtures.now) }
    }
}
