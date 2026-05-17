package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.TaxCalculationResult
import com.hermes.domain.tax.TaxEngine
import com.hermes.domain.tax.TaxLineInput
import java.time.Clock
import java.time.Instant

class TaxCalculatePreviewUseCase(
    private val profileRepository: TaxProfileRepository,
    private val settingsRepository: OrganizationTaxSettingsRepository,
    private val auditLogger: TaxAuditLogger = NoopTaxAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: TaxCalculatePreviewCommand): TaxCalculationResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.TAX_SETTINGS_VIEW)

        if (command.organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (command.lines.isEmpty()) throw DomainRuleViolation("Tax preview requires at least one line.")

        val settings = settingsRepository.findByOrganizationId(command.organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")

        val inputs = command.lines.map { line ->
            settings.assertCanUseProfile(line.taxProfileCode)
            val profile = profileRepository.findByCode(line.taxProfileCode)
                ?: throw DomainRuleViolation("Tax profile does not exist: ${line.taxProfileCode}.")
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

        val result = TaxEngine.calculate(inputs)

        auditLogger.log(
            TaxAuditEvent(
                action = TaxAuditAction.TAX_CALCULATION_PREVIEWED,
                actorUserId = command.actorUserId,
                organizationId = command.organizationId,
                targetId = null,
                createdAt = Instant.now(clock),
            )
        )

        return result
    }
}
