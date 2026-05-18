package com.hermes.application.tax

import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxEmissionType
import com.hermes.domain.tax.TaxFixtures
import com.hermes.domain.tax.TaxRegimeCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxDocumentEmissionValidationUseCaseTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")

    @Test
    fun `allows electronic invoice when all snapshots are SRI compatible`() {
        val fixture = fixture()

        val result = fixture.useCase.execute(
            TaxDocumentEmissionValidationCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE),
                emissionType = TaxEmissionType.ELECTRONIC_INVOICE,
                occurredAt = now,
                lines = listOf(
                    TaxSaleValidationLine(
                        lineId = "line_1",
                        description = "Producto gravado",
                        quantity = Quantity.units(1),
                        unitPrice = Money.of("10.00"),
                        taxProfileCode = "iva_current_full",
                    )
                ),
            )
        )

        assertEquals("11.30", result.calculation.summary.grandTotal.amount.toPlainString())
        assertEquals(1, fixture.audit.events.count { it.action == TaxAuditAction.TAX_DOCUMENT_EMISSION_VALIDATED })
    }

    @Test
    fun `rejects electronic invoice with internal profile`() {
        val fixture = fixture()

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                TaxDocumentEmissionValidationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE),
                    emissionType = TaxEmissionType.ELECTRONIC_INVOICE,
                    occurredAt = now,
                    lines = listOf(
                        TaxSaleValidationLine(
                            lineId = "line_1",
                            description = "Interno",
                            quantity = Quantity.units(1),
                            unitPrice = Money.of("10.00"),
                            taxProfileCode = "no_tax_internal",
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun `allows internal ticket with internal profile`() {
        val fixture = fixture()

        val result = fixture.useCase.execute(
            TaxDocumentEmissionValidationCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.DOCUMENTS_GENERATE_INTERNAL_TICKET),
                emissionType = TaxEmissionType.INTERNAL_TICKET,
                occurredAt = now,
                lines = listOf(
                    TaxSaleValidationLine(
                        lineId = "line_1",
                        description = "Interno",
                        quantity = Quantity.units(1),
                        unitPrice = Money.of("10.00"),
                        taxProfileCode = "no_tax_internal",
                    )
                ),
            )
        )

        assertEquals("10.00", result.calculation.summary.subtotalInternalNoTax.amount.toPlainString())
    }

    private fun fixture(): Fixture {
        val profileRepository = InMemoryTaxProfileRepository()
        TaxFixtures.allProfiles.forEach(profileRepository::create)

        val settingsRepository = InMemoryOrganizationTaxSettingsRepository()
        settingsRepository.create(
            OrganizationTaxSettings(
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
        )

        val audit = RecordingTaxAuditStore()
        val useCase = TaxDocumentEmissionValidationUseCase(
            profileRepository = profileRepository,
            settingsRepository = settingsRepository,
            auditLogger = audit,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        return Fixture(useCase, audit)
    }

    private data class Fixture(
        val useCase: TaxDocumentEmissionValidationUseCase,
        val audit: RecordingTaxAuditStore,
    )
}
