package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.shared.DomainRuleViolation

class ActiveOrganizationResolverUseCase(
    private val repository: AuthContextRepository,
) {
    fun execute(command: ResolveActiveOrganizationCommand): ActiveOrganizationContext? {
        val userId = command.userId.trim()
        if (userId.isBlank()) {
            throw DomainRuleViolation("User id is required to resolve active organization.")
        }

        val activeMemberships = repository.findMembershipsByUserId(userId)
            .filter { it.status == MembershipStatus.ACTIVE }

        if (activeMemberships.isEmpty()) {
            if (command.required) {
                throw DomainRuleViolation("User has no active organization memberships.")
            }
            return null
        }

        val selectedMembership = when {
            !command.requestedOrganizationId.isNullOrBlank() -> {
                val requested = command.requestedOrganizationId.trim()
                activeMemberships.firstOrNull { it.organizationId == requested }
                    ?: throw DomainRuleViolation("User does not have active membership in requested organization.")
            }

            activeMemberships.size == 1 -> activeMemberships.first()
            command.required -> throw DomainRuleViolation("Active organization must be selected explicitly.")
            else -> return null
        }

        selectedMembership.assertCanAccessOrganization()

        val organization = repository.findOrganizationById(selectedMembership.organizationId)
            ?: throw DomainRuleViolation("Active organization does not exist.")
        organization.assertCanOperate()

        return ActiveOrganizationContext(
            organization = organization,
            membership = selectedMembership,
        )
    }
}

data class ResolveActiveOrganizationCommand(
    val userId: String,
    val requestedOrganizationId: String? = null,
    val required: Boolean = true,
)
