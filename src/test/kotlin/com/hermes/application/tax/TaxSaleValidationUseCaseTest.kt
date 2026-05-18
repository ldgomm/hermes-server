package com.hermes.application.tax

import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.PriceTaxMode
import com.hermes.domain.tax.TaxFixtures
import com.hermes.domain.tax.TaxRegimeCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxSaleValidationUseCaseTest {
    private val now = Instant.parse("2026-05-17T12:00:00Z")

    @Test
    fun `validates sale tax lines using organization settings and profiles`() {
        val fixture = fixture()

        val result = fixture.useCase.execute(
            TaxSaleValidationCommand(
                organizationId = "org_1",
                actorUserId = "usr_1",
                actorEffectivePermissions = setOf(PermissionCatalog.SALES_CREATE),
                occurredAt = now,
                lines = listOf(
                    TaxSaleValidationLine(
                        lineId = "line_1",
                        catalogItemId = "cat_1",
                        description = "Producto gravado",
                        quantity = Quantity.units(2),
                        unitPrice = Money.of("10.00"),
                        discount = Money.of("0.00"),
                        taxProfileCode = "iva_current_full",
                    )
                ),
            )
        )

        assertEquals("22.60", result.calculation.summary.grandTotal.amount.toPlainString())
        assertEquals(1, fixture.audit.events.count { it.action == TaxAuditAction.TAX_SALE_VALIDATED })
    }

    @Test
    fun `rejects sale with disabled tax profile`() {
        val fixture = fixture(enabledProfiles = setOf("iva_current_full"))

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                TaxSaleValidationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.SALES_CREATE),
                    occurredAt = now,
                    lines = listOf(
                        TaxSaleValidationLine(
                            lineId = "line_1",
                            description = "Producto no habilitado",
                            quantity = Quantity.units(1),
                            unitPrice = Money.of("10.00"),
                            taxProfileCode = "iva_0",
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun `rejects tax inclusive sale when organization does not allow it`() {
        val fixture = fixture(allowTaxInclusivePrices = false)

        assertFailsWith<DomainRuleViolation> {
            fixture.useCase.execute(
                TaxSaleValidationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_1",
                    actorEffectivePermissions = setOf(PermissionCatalog.SALES_CREATE),
                    occurredAt = now,
                    lines = listOf(
                        TaxSaleValidationLine(
                            lineId = "line_1",
                            description = "Producto inclusivo",
                            quantity = Quantity.units(1),
                            unitPrice = Money.of("11.30"),
                            taxProfileCode = "iva_current_full",
                            priceTaxMode = PriceTaxMode.TAX_INCLUSIVE,
                        )
                    ),
                )
            )
        }
    }

    private fun fixture(
        enabledProfiles: Set<String> = setOf(
            "iva_current_full",
            "iva_0",
            "exempt_iva",
            "not_subject_to_iva",
            "no_tax_internal",
        ),
        allowTaxInclusivePrices: Boolean = true,
    ): Fixture {
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
                allowTaxInclusivePrices = allowTaxInclusivePrices,
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
        val useCase = TaxSaleValidationUseCase(
            profileRepository = profileRepository,
            settingsRepository = settingsRepository,
            auditLogger = audit,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

        return Fixture(useCase, audit)
    }

    private data class Fixture(
        val useCase: TaxSaleValidationUseCase,
        val audit: RecordingTaxAuditStore,
    )
}
