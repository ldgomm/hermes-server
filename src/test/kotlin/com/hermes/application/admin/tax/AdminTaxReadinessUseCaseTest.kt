package com.hermes.application.admin.tax

import com.hermes.application.tax.InMemoryOrganizationTaxSettingsRepository
import com.hermes.application.tax.InMemoryTaxProfileRepository
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.tax.EcuadorTaxSeed
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxRegimeCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminTaxReadinessUseCaseTest {
    private val now: Instant = Instant.parse("2026-05-20T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `readiness is ready when settings and enabled profiles are valid`() {
        val profiles = InMemoryTaxProfileRepository()
        EcuadorTaxSeed.profiles.forEach(profiles::create)
        val settings = InMemoryOrganizationTaxSettingsRepository()
        settings.create(validSettings())
        val useCase = GetAdminTaxReadinessUseCase(settings, profiles, clock)

        val result = useCase.execute(command())

        assertTrue(result.ready)
        assertEquals(AdminTaxReadinessStatus.READY, result.status)
        assertEquals(5, result.enabledProfileCount)
        assertEquals(emptySet(), result.missingProfileCodes)
    }

    @Test
    fun `readiness is action required when tax settings are missing`() {
        val profiles = InMemoryTaxProfileRepository()
        val settings = InMemoryOrganizationTaxSettingsRepository()
        val useCase = GetAdminTaxReadinessUseCase(settings, profiles, clock)

        val result = useCase.execute(command())

        assertFalse(result.ready)
        assertEquals(AdminTaxReadinessStatus.ACTION_REQUIRED, result.status)
        assertTrue(result.checks.any { it.code == "tax_settings_present" && it.status == AdminTaxReadinessCheckStatus.FAILED })
    }

    @Test
    fun `readiness detects missing enabled profile codes`() {
        val profiles = InMemoryTaxProfileRepository()
        EcuadorTaxSeed.profiles.forEach(profiles::create)
        val settings = InMemoryOrganizationTaxSettingsRepository()
        settings.create(validSettings().copy(enabledTaxProfileCodes = setOf("iva_current_full", "missing_profile")))
        val useCase = GetAdminTaxReadinessUseCase(settings, profiles, clock)

        val result = useCase.execute(command())

        assertFalse(result.ready)
        assertEquals(setOf("missing_profile"), result.missingProfileCodes)
    }

    private fun command(): GetAdminTaxReadinessCommand = GetAdminTaxReadinessCommand(
        organizationId = "org_1",
        actorUserId = "usr_1",
        actorEffectivePermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
    )

    private fun validSettings(): OrganizationTaxSettings = OrganizationTaxSettings(
        id = "taxset_org_1",
        organizationId = "org_1",
        regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
        defaultTaxProfileCode = "iva_current_full",
        enabledTaxProfileCodes = setOf(
            "iva_current_full",
            "iva_0",
            "exempt_iva",
            "not_subject_to_iva",
            "no_tax_internal",
        ),
        allowTaxInclusivePrices = true,
        allowManualLineDiscounts = true,
        requireTaxProfileForCatalogItems = true,
        status = OrganizationTaxSettingsStatus.ACTIVE,
        createdAt = now,
        updatedAt = now,
        createdBy = "usr_1",
        updatedBy = "usr_1",
    )
}
