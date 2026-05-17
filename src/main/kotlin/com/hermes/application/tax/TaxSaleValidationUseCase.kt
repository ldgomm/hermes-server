package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxEngine
import com.hermes.domain.tax.TaxLineInput
import java.time.Clock
import java.time.Instant

class TaxSaleValidationUseCase(
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxSaleValidationCommand): TaxSaleValidationResult {
        assertCanValidateSale(command.actorEffectivePermissions)

        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (command.lines.isEmpty()) throw DomainRuleViolation("Sale tax validation requires at least one line.")

        val settings = settingsRepository.findByOrganizationId(organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        if (!settings.allowTaxInclusivePrices && command.lines.any { it.priceTaxMode.name == "TAX_INCLUSIVE" }) {
            throw DomainRuleViolation("Organization does not allow tax-inclusive prices.")
        }

        if (!settings.allowManualLineDiscounts && command.lines.any { it.discount.amount.signum() > 0 }) {
            throw DomainRuleViolation("Organization does not allow manual tax line discounts.")
        }

        val inputs = command.lines.map { line ->
            val taxProfileCode = line.taxProfileCode.trim().lowercase()
            settings.assertCanUseProfile(taxProfileCode)

            val profile = profileRepository.findByCode(taxProfileCode)
                ?: throw DomainRuleViolation("Tax profile does not exist: $taxProfileCode.")

            TaxLineInput(
                lineId = line.lineId,
                description = line.description,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                discount = line.discount,
                taxProfileSnapshot = profile.snapshot(command.occurredAt, forEmission = false),
                priceTaxMode = line.priceTaxMode,
            )
        }

        val calculation = TaxEngine.calculate(inputs)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_SALE_VALIDATED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = null,
                after = mapOf(
                    "lineCount" to calculation.lines.size.toString(),
                    "grandTotal" to calculation.summary.grandTotal.amount.toPlainString(),
                    "currency" to calculation.summary.currency.value,
                ),
                createdAt = Instant.now(clock),
            )
        )

        return TaxSaleValidationResult(
            organizationId = organizationId,
            occurredAt = command.occurredAt,
            calculation = calculation,
        )
    }

    private fun assertCanValidateSale(effectivePermissions: Set<String>) {
        val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.SALES_CREATE) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.SALES_CREATE}, ${PermissionCatalog.TAX_SETTINGS_VIEW}."
            )
        }
    }
}
