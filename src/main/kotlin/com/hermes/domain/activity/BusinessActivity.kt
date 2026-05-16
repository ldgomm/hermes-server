package com.hermes.domain.activity

import com.hermes.domain.shared.DomainRuleViolation

data class BusinessActivity(
    val id: String,
    val organizationId: String,
    val name: String,
    val type: BusinessActivityType,
    val status: BusinessActivityStatus,
    val workflowModes: Set<WorkflowMode>,
    val requiresCashSession: Boolean = true,
    val affectsInventory: Boolean = false,
    val allowsReservations: Boolean = false,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Business activity id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Business activity organization id cannot be blank.")
        if (name.isBlank()) throw DomainRuleViolation("Business activity name cannot be blank.")
    }
}
