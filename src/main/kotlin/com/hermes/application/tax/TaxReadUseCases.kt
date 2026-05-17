package com.hermes.application.tax

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxRate

data class TaxQueryCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

class TaxListActiveRatesUseCase(
    private val rateRepository: TaxRateRepository,
) {
    fun execute(command: TaxQueryCommand): List<TaxRate> {
        PermissionRules.assertCanPerform(
            effectivePermissions = command.actorEffectivePermissions,
            permission = PermissionCatalog.TAX_SETTINGS_VIEW,
        )

        if (command.organizationId.isBlank()) {
            throw DomainRuleViolation("Organization id is required.")
        }

        return rateRepository.findActive()
    }
}

class TaxListActiveProfilesUseCase(
    private val profileRepository: TaxProfileRepository,
) {
    fun execute(command: TaxQueryCommand): List<TaxProfile> {
        PermissionRules.assertCanPerform(
            effectivePermissions = command.actorEffectivePermissions,
            permission = PermissionCatalog.TAX_SETTINGS_VIEW,
        )

        if (command.organizationId.isBlank()) {
            throw DomainRuleViolation("Organization id is required.")
        }

        return profileRepository.findActive()
    }
}

class TaxGetOrganizationSettingsUseCase(
    private val settingsRepository: OrganizationTaxSettingsRepository,
) {
    fun execute(command: TaxQueryCommand): OrganizationTaxSettings {
        PermissionRules.assertCanPerform(
            effectivePermissions = command.actorEffectivePermissions,
            permission = PermissionCatalog.TAX_SETTINGS_VIEW,
        )

        if (command.organizationId.isBlank()) {
            throw DomainRuleViolation("Organization id is required.")
        }

        return settingsRepository.findByOrganizationId(command.organizationId)
            ?: throw DomainRuleViolation("Organization tax settings do not exist.")
    }
}