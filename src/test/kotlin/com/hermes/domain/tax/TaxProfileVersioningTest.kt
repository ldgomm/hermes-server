package com.hermes.domain.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TaxProfileVersioningTest {
    @Test
    fun `snapshot freezes profile and rate versions`() {
        val snapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now)
        val updatedProfile = TaxFixtures.iva13Profile.copy(
            name = "IVA changed after sale",
            version = TaxFixtures.iva13Profile.version + 1,
        )

        assertEquals(1, snapshot.profileVersion)
        assertEquals(1, snapshot.rateVersion)
        assertNotEquals(updatedProfile.name, snapshot.profileName)
        assertEquals("IVA current full", snapshot.profileName)
    }

    @Test
    fun `profile update produces next version and keeps code stable`() {
        val updatedProfile = TaxFixtures.iva13Profile.copy(
            name = "IVA tarifa vigente general actualizada",
            version = TaxFixtures.iva13Profile.version + 1,
        )

        assertEquals("iva_current_full", updatedProfile.code)
        assertEquals(2, updatedProfile.version)
    }
}
