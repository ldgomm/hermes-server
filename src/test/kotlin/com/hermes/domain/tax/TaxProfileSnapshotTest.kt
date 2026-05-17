package com.hermes.domain.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaxProfileSnapshotTest {
    @Test
    fun `snapshot keeps tax profile and rate data`() {
        val snapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now, forEmission = true)

        assertEquals("iva_current_full", snapshot.profileCode)
        assertEquals("13.0000", snapshot.rate.toPlainString())
        assertEquals("2", snapshot.sriTaxCode)
        assertEquals("4", snapshot.sriRateCode)
        assertTrue(snapshot.isElectronicEmissionCompatible)
    }
}
