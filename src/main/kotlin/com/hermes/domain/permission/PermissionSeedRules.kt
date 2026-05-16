package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation

object PermissionSeedRules {
    fun validate(definitions: List<PermissionDefinition>) {
        if (definitions.isEmpty()) {
            throw DomainRuleViolation("Permission seed cannot be empty.")
        }

        val duplicateCodes = definitions
            .groupBy { it.code }
            .filterValues { it.size > 1 }
            .keys

        if (duplicateCodes.isNotEmpty()) {
            throw DomainRuleViolation("Duplicated permission codes: ${duplicateCodes.sorted().joinToString()}.")
        }

        val disabledSystemPermissions = definitions.filter {
            it.systemManaged && it.status == PermissionStatus.DISABLED
        }

        if (disabledSystemPermissions.isNotEmpty()) {
            throw DomainRuleViolation(
                "System-managed permissions cannot be seeded as disabled: " +
                    disabledSystemPermissions.joinToString { it.code }
            )
        }

        val reservedWithoutFeatureFlag = definitions.filter {
            it.status == PermissionStatus.RESERVED && it.featureFlag.isNullOrBlank()
        }

        if (reservedWithoutFeatureFlag.isNotEmpty()) {
            throw DomainRuleViolation(
                "Reserved permissions require a feature flag: " +
                    reservedWithoutFeatureFlag.joinToString { it.code }
            )
        }

        val criticalWithoutAudit = definitions.filter {
            it.riskLevel == PermissionRiskLevel.CRITICAL && !it.requiresAudit
        }

        if (criticalWithoutAudit.isNotEmpty()) {
            throw DomainRuleViolation(
                "Critical permissions must require audit: " +
                    criticalWithoutAudit.joinToString { it.code }
            )
        }
    }

    fun assertRequiredPermissionsExist(definitions: List<PermissionDefinition>, requiredCodes: Set<String>) {
        val existingCodes = definitions.map { it.code }.toSet()
        val missing = requiredCodes - existingCodes

        if (missing.isNotEmpty()) {
            throw DomainRuleViolation("Missing required permissions: ${missing.sorted().joinToString()}.")
        }
    }
}
