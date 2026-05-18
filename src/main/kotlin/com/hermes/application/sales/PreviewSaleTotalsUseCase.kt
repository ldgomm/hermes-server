package com.hermes.application.sales

import com.hermes.application.catalog.OrganizationCatalogItemRepository
import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.application.tax.TaxSaleValidationCommand
import com.hermes.application.tax.TaxSaleValidationLine
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Calculates sale totals/taxes before creating a Sale.
 *
 * This gives Business App/Admin a safe preview using the same Tax Engine that
 * later creates immutable SaleItem snapshots. It supports mixed tax profiles,
 * discounts and tax-inclusive prices according to OrganizationTaxSettings.
 */
class PreviewSaleTotalsUseCase(
    private val catalogRepository: OrganizationCatalogItemRepository,
    private val taxProfileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val taxSaleValidationUseCase: TaxSaleValidationUseCase,
) {
    fun execute(command: PreviewSaleTotalsCommand): PreviewSaleTotalsResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_CREATE)

        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (command.lines.isEmpty()) throw DomainRuleViolation("Sale totals preview requires at least one line.")

        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        val catalogItems = command.lines.map { line ->
            val catalogItemId = line.catalogItemId.trim()
            if (catalogItemId.isBlank()) throw DomainRuleViolation("Catalog item id cannot be blank.")
            catalogRepository.findById(organizationId = organizationId, catalogItemId = catalogItemId)
                ?: throw DomainRuleViolation("Organization catalog item does not exist: $catalogItemId.")
        }

        catalogItems.forEach { item ->
            if (item.status != CatalogItemStatus.ACTIVE) {
                throw DomainRuleViolation("Only active catalog items can be used in sale totals preview.")
            }
        }

        val taxLines = command.lines.zip(catalogItems).mapIndexed { index, (line, item) ->
            val profile = taxProfileRepository.findById(item.taxProfileId)
                ?: throw DomainRuleViolation("Catalog item tax profile does not exist: ${item.taxProfileId}.")
            settings.assertCanUseProfile(profile.code)

            val unitPrice = line.unitPrice ?: item.localPrice
            TaxSaleValidationLine(
                lineId = "line_${index + 1}",
                catalogItemId = item.id,
                description = item.localName,
                quantity = line.quantity,
                unitPrice = unitPrice,
                discount = line.discount ?: Money.zero(unitPrice.currency),
                taxProfileCode = profile.code,
                priceTaxMode = line.priceTaxMode,
            )
        }

        val taxResult = taxSaleValidationUseCase.execute(
            TaxSaleValidationCommand(
                organizationId = organizationId,
                actorUserId = command.actorUserId,
                actorEffectivePermissions = command.actorEffectivePermissions,
                occurredAt = command.occurredAt,
                lines = taxLines,
            )
        )

        val taxByLine = taxResult.calculation.lines.associateBy { it.lineId }
        val previewLines = command.lines.zip(catalogItems).mapIndexed { index, (line, item) ->
            val lineId = "line_${index + 1}"
            val calculated = taxByLine.getValue(lineId)
            val profileSnapshot = calculated.taxProfileSnapshot
            val unitPrice = line.unitPrice ?: item.localPrice
            PreviewSaleTotalsLineResult(
                lineId = lineId,
                catalogItemId = item.id,
                catalogItemName = item.localName,
                quantity = line.quantity,
                unitPrice = unitPrice,
                discount = line.discount ?: Money.zero(unitPrice.currency),
                priceTaxMode = line.priceTaxMode,
                grossAmount = calculated.grossAmount,
                taxableBase = calculated.taxableBase,
                zeroRateBase = calculated.zeroRateBase,
                exemptBase = calculated.exemptBase,
                notSubjectBase = calculated.notSubjectBase,
                internalNoTaxBase = calculated.internalNoTaxBase,
                taxAmount = calculated.taxAmount,
                total = calculated.total,
                taxProfileCode = profileSnapshot.profileCode,
                sriTaxCode = profileSnapshot.sriTaxCode,
                sriRateCode = profileSnapshot.sriRateCode,
                treatment = profileSnapshot.treatment.name,
            )
        }

        return PreviewSaleTotalsResult(
            organizationId = organizationId,
            occurredAt = command.occurredAt,
            lines = previewLines,
            summary = taxResult.calculation.summary,
        )
    }
}
