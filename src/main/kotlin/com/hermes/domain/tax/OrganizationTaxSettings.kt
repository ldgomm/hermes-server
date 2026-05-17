package com.hermes.domain.tax

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class OrganizationTaxSettings(
    val id: String,
    val organizationId: String,
    val regime: TaxRegimeCode,
    val defaultTaxProfileCode: String,
    val enabledTaxProfileCodes: Set<String>,
    val allowTaxInclusivePrices: Boolean,
    val allowManualLineDiscounts: Boolean,
    val requireTaxProfileForCatalogItems: Boolean,
    val status: OrganizationTaxSettingsStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val createdBy: String,
    val updatedBy: String,
    val version: Long = 1,
    val schemaVersion: Int = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Organization tax settings id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (defaultTaxProfileCode.isBlank()) throw DomainRuleViolation("Default tax profile code cannot be blank.")
        if (enabledTaxProfileCodes.isEmpty()) throw DomainRuleViolation("At least one tax profile must be enabled.")
        if (defaultTaxProfileCode !in enabledTaxProfileCodes) {
            throw DomainRuleViolation("Default tax profile must be enabled for the organization.")
        }
        if (enabledTaxProfileCodes.any { it.isBlank() || it != it.trim() }) {
            throw DomainRuleViolation("Enabled tax profile codes cannot be blank or contain edge spaces.")
        }
        if (createdBy.isBlank()) throw DomainRuleViolation("Organization tax settings createdBy cannot be blank.")
        if (updatedBy.isBlank()) throw DomainRuleViolation("Organization tax settings updatedBy cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Organization tax settings version must be positive.")
        if (schemaVersion < 1) throw DomainRuleViolation("Organization tax settings schemaVersion must be positive.")
    }

    fun assertCanUseProfile(profileCode: String) {
        if (status != OrganizationTaxSettingsStatus.ACTIVE) {
            throw DomainRuleViolation("Organization tax settings cannot be used from status $status.")
        }
        if (profileCode !in enabledTaxProfileCodes) {
            throw DomainRuleViolation("Tax profile $profileCode is not enabled for this organization.")
        }
    }
}

enum class OrganizationTaxSettingsStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED,
}
