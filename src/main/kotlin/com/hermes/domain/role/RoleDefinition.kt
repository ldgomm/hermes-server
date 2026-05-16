package com.hermes.domain.role

import com.hermes.domain.shared.DomainRuleViolation

/**
 * RoleDefinition is the canonical template used by the system seed.
 * Organization-specific membership assignment comes later in Fase 5.12.
 */
data class RoleDefinition(
    val id: String,
    val code: String,
    val organizationId: String?,
    val scope: RoleScope,
    val type: RoleType,
    val name: String,
    val description: String,
    val permissionKeys: Set<String>,
    val systemRole: Boolean,
    val critical: Boolean,
    val editable: Boolean,
    val status: RoleStatus = RoleStatus.ACTIVE,
    val schemaVersion: Int = 1,
) {
    init {
        if (id.isBlank()) {
            throw DomainRuleViolation("Role id cannot be blank.")
        }

        if (code.isBlank()) {
            throw DomainRuleViolation("Role code cannot be blank.")
        }

        if (code != code.trim()) {
            throw DomainRuleViolation("Role code cannot contain leading or trailing spaces.")
        }

        if (!CODE_PATTERN.matches(code)) {
            throw DomainRuleViolation("Role code has invalid format: $code.")
        }

        if (name.isBlank()) {
            throw DomainRuleViolation("Role name cannot be blank.")
        }

        if (description.isBlank()) {
            throw DomainRuleViolation("Role description cannot be blank.")
        }

        if (permissionKeys.isEmpty()) {
            throw DomainRuleViolation("Role requires at least one permission.")
        }

        if (permissionKeys.any { it.isBlank() || it != it.trim() }) {
            throw DomainRuleViolation("Role permission keys cannot be blank or contain edge spaces.")
        }

        if (scope == RoleScope.PLATFORM && organizationId != null) {
            throw DomainRuleViolation("Platform roles cannot belong to an organization.")
        }

        if (scope == RoleScope.PLATFORM && type == RoleType.CUSTOM) {
            throw DomainRuleViolation("Custom roles cannot use platform scope.")
        }

        if (type == RoleType.CUSTOM && organizationId.isNullOrBlank()) {
            throw DomainRuleViolation("Custom roles require an organization id.")
        }

        if (type != RoleType.CUSTOM && organizationId != null) {
            throw DomainRuleViolation("Seeded system role templates cannot have organization id.")
        }

        if (systemRole != (type != RoleType.CUSTOM)) {
            throw DomainRuleViolation("systemRole must be true for seeded system templates and false for custom roles.")
        }

        if (critical && editable) {
            throw DomainRuleViolation("Critical system roles cannot be editable.")
        }

        if (schemaVersion < 1) {
            throw DomainRuleViolation("Role schemaVersion must be positive.")
        }
    }

    val isPlatformRole: Boolean
        get() = scope == RoleScope.PLATFORM

    val isOrganizationRole: Boolean
        get() = scope == RoleScope.ORGANIZATION

    companion object {
        private val CODE_PATTERN = Regex("^[a-z][a-z0-9_]*$")
    }
}
