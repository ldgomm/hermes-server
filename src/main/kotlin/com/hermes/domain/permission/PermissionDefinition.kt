package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation

data class PermissionDefinition(
    val code: String,
    val name: String,
    val description: String,
    val category: PermissionCategory,
    val scope: PermissionScope,
    val riskLevel: PermissionRiskLevel = PermissionRiskLevel.LOW,
    val status: PermissionStatus = PermissionStatus.ACTIVE,
    val systemManaged: Boolean = true,
    val requiresAudit: Boolean = false,
    val requiresReason: Boolean = false,
    val requiresStepUp: Boolean = false,
    val featureFlag: String? = null,
) {
    init {
        if (code.isBlank()) {
            throw DomainRuleViolation("Permission code cannot be blank.")
        }

        if (code != code.trim()) {
            throw DomainRuleViolation("Permission code cannot contain leading or trailing spaces.")
        }

        if (!CODE_PATTERN.matches(code)) {
            throw DomainRuleViolation("Permission code has invalid format: $code.")
        }

        if (name.isBlank()) {
            throw DomainRuleViolation("Permission name cannot be blank.")
        }

        if (description.isBlank()) {
            throw DomainRuleViolation("Permission description cannot be blank.")
        }

        if (riskLevel == PermissionRiskLevel.CRITICAL && !requiresAudit) {
            throw DomainRuleViolation("Critical permissions must require audit.")
        }

        if (requiresStepUp && riskLevel !in setOf(PermissionRiskLevel.HIGH, PermissionRiskLevel.CRITICAL)) {
            throw DomainRuleViolation("Step-up permissions must be high or critical risk.")
        }

        if (featureFlag != null && featureFlag.isBlank()) {
            throw DomainRuleViolation("Permission feature flag cannot be blank.")
        }
    }

    companion object {
        private val CODE_PATTERN = Regex("""^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$""")
    }
}
