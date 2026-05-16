package com.hermes.domain.shared

class StateTransitionValidator<T : Enum<T>>(
    private val entityName: String,
    private val transitions: Map<T, Set<T>>
) {

    fun canTransition(from: T, to: T): Boolean {
        if (from == to) return true
        return transitions[from]?.contains(to) == true
    }

    fun assertCanTransition(from: T, to: T) {
        if (!canTransition(from, to)) {
            throw DomainRuleViolation(
                "Invalid $entityName state transition from ${from.name} to ${to.name}."
            )
        }
    }
}
