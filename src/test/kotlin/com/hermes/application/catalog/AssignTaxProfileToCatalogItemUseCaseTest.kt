package com.hermes.application.catalog

import com.hermes.application.tax.InMemoryOrganizationTaxSettingsRepository
import com.hermes.application.tax.InMemoryTaxProfileRepository
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxFixtures
import com.hermes.domain.tax.TaxRegimeCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AssignTaxProfileToCatalogItemUseCaseTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")

    @Test
    fun `assigns enabled active tax profile to catalog item`() {
        val fixture = fixture()

        val result = fixture.useCase.execute(
            AssignTaxProfileToCatalogItemCommand(
                organizationId = "org_1",
                catalogItemId = "cat_1",
                taxProfileCode = "iva_0",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM),
                reason = "Producto con tarifa cero",
            )
        )

        assertEquals("taxp_iva0", result.assignment.taxProfileId)
        assertEquals("taxp_iva13", result.assignment.previousTaxProfileId)
        assertEquals(now, result.assignment.updatedAt)
        assertEquals(
            1,
            fixture.audit.events.count { it.action == CatalogAuditAction.LOCAL_ITEM_TAX_PROFILE_ASSIGNED },
        )
    }

    @Test
    fun `rejects assignment with disabled profile`() {
        val fixture = fixture(enabledProfiles = setOf("iva_current_full"))

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                AssignTaxProfileToCatalogItemCommand(
                    organizationId = "org_1",
                    catalogItemId = "cat_1",
                    taxProfileCode = "iva_0",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM),
                    reason = "Intento inválido",
                )
            )
        }
    }

    private fun fixture(
        enabledProfiles: Set<String> = setOf("iva_current_full", "iva_0"),
    ): Fixture {
        val catalogRepository = InMemoryCatalogTaxProfileRepository()
        catalogRepository.items["cat_1"] = CatalogTaxProfileAssignmentRecord(
            organizationId = "org_1",
            catalogItemId = "cat_1",
            previousTaxProfileId = null,
            taxProfileId = "taxp_iva13",
            updatedAt = now,
        )

        val profileRepository = InMemoryTaxProfileRepository()
        TaxFixtures.allProfiles.forEach(profileRepository::create)

        val settingsRepository = InMemoryOrganizationTaxSettingsRepository()
        settingsRepository.create(
            OrganizationTaxSettings(
                id = "taxset_org_1",
                organizationId = "org_1",
                regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
                defaultTaxProfileCode = "iva_current_full",
                enabledTaxProfileCodes = enabledProfiles,
                allowTaxInclusivePrices = true,
                allowManualLineDiscounts = true,
                requireTaxProfileForCatalogItems = true,
                status = OrganizationTaxSettingsStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                createdBy = "usr_1",
                updatedBy = "usr_1",
            )
        )

        val audit = RecordingCatalogAuditLogger()
        val useCase = AssignTaxProfileToCatalogItemUseCase(
            catalogRepository = catalogRepository,
            profileRepository = profileRepository,
            settingsRepository = settingsRepository,
            auditLogger = audit,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        return Fixture(useCase, audit)
    }

    private data class Fixture(
        val useCase: AssignTaxProfileToCatalogItemUseCase,
        val audit: RecordingCatalogAuditLogger,
    )

    private class RecordingCatalogAuditLogger : CatalogAuditLogger {
        val events = mutableListOf<CatalogAuditEvent>()

        override fun log(event: CatalogAuditEvent) {
            events += event
        }
    }

    private class InMemoryCatalogTaxProfileRepository : OrganizationCatalogTaxProfileRepository {
        val items = linkedMapOf<String, CatalogTaxProfileAssignmentRecord>()

        override fun assignTaxProfile(
            organizationId: String,
            catalogItemId: String,
            taxProfileId: String,
            updatedAt: Instant,
        ): CatalogTaxProfileAssignmentRecord {
            val current = items[catalogItemId]
                ?: throw DomainRuleViolation("Catalog item does not exist for this organization.")

            if (current.organizationId != organizationId) {
                throw DomainRuleViolation("Catalog item does not exist for this organization.")
            }

            val updated = CatalogTaxProfileAssignmentRecord(
                organizationId = organizationId,
                catalogItemId = catalogItemId,
                previousTaxProfileId = current.taxProfileId,
                taxProfileId = taxProfileId,
                updatedAt = updatedAt,
            )
            items[catalogItemId] = updated
            return updated
        }
    }
}
