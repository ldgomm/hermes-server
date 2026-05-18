package com.hermes.application.sales

import com.hermes.application.catalog.OrganizationCatalogItemRepository
import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.application.tax.TaxSaleValidationCommand
import com.hermes.application.tax.TaxSaleValidationLine
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.money.Money
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

class SaleItemPreparationService(
    private val catalogRepository: OrganizationCatalogItemRepository,
    private val taxProfileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val taxSaleValidationUseCase: TaxSaleValidationUseCase,
    private val idGenerator: SalesIdGenerator,
) {
    fun prepare(
        organizationId: String,
        actorUserId: String,
        actorEffectivePermissions: Set<String>,
        occurredAt: Instant,
        lines: List<CreateSaleItemCommandLine>,
    ): List<SaleItem> {
        if (lines.isEmpty()) throw DomainRuleViolation("Sale requires at least one item.")
        settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        val catalogItems = lines.map { line ->
            catalogRepository.findById(organizationId, line.catalogItemId.required("Catalog item id"))
                ?: throw DomainRuleViolation("Organization catalog item does not exist: ${line.catalogItemId}.")
        }
        catalogItems.forEach(::assertSellableCatalogItem)

        val taxLines = lines.zip(catalogItems).mapIndexed { index, (line, catalogItem) ->
            val profile = taxProfileRepository.findById(catalogItem.taxProfileId)
                ?: throw DomainRuleViolation("Catalog item tax profile does not exist: ${catalogItem.taxProfileId}.")
            val unitPrice = line.unitPrice ?: catalogItem.localPrice
            TaxSaleValidationLine(
                lineId = "line_${index + 1}",
                catalogItemId = catalogItem.id,
                description = catalogItem.localName,
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
                actorUserId = actorUserId,
                actorEffectivePermissions = actorEffectivePermissions,
                occurredAt = occurredAt,
                lines = taxLines,
            )
        )
        val taxByLine = taxResult.calculation.lines.associateBy { it.lineId }

        return lines.zip(catalogItems).mapIndexed { index, (line, catalogItem) ->
            val lineId = "line_${index + 1}"
            val taxLine = taxByLine.getValue(lineId)
            val unitPrice = line.unitPrice ?: catalogItem.localPrice
            SaleItem.create(
                id = idGenerator.newId("sitem"),
                catalogItemId = catalogItem.id,
                name = catalogItem.localName,
                unitPrice = unitPrice,
                quantity = line.quantity,
                discount = line.discount ?: Money.zero(unitPrice.currency),
                catalogSnapshot = catalogItem.toCatalogSnapshot(),
                taxProfileSnapshot = taxLine.taxProfileSnapshot.toSaleSnapshot(),
                taxes = listOf(taxLine.toSaleItemTax()),
            )
        }
    }

    private fun OrganizationCatalogItem.toCatalogSnapshot(): CatalogItemSnapshot =
        CatalogItemSnapshot(
            catalogItemId = id,
            sourceTemplateId = templateId,
            globalCatalogId = globalCatalogId,
            productFamilyId = productFamilyId,
            name = localName,
            type = type,
            taxProfileId = taxProfileId,
            unitCode = "unit",
        )

    private fun assertSellableCatalogItem(item: OrganizationCatalogItem) {
        if (item.status != CatalogItemStatus.ACTIVE) {
            throw DomainRuleViolation("Only active catalog items can be sold.")
        }
    }
}

internal fun String.required(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")
