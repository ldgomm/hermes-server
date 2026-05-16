package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation

data class Role(
    val id: String,
    val organizationId: String?,
    val name: String,
    val permissions: Set<String>,
    val systemRole: Boolean = false,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Role id cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Role name cannot be blank.")
        if (permissions.isEmpty()) throw DomainRuleViolation("Role requires at least one permission.")
    }
}
