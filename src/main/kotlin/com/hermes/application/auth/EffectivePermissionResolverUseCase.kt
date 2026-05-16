package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.shared.DomainRuleViolation

class EffectivePermissionResolverUseCase(
    private val repository: AuthContextRepository,
) {
    fun execute(command: ResolveEffectivePermissionsCommand): EffectivePermissionContext {
        val userId = command.userId.trim()
        val organizationId = command.organizationId.trim()

        if (userId.isBlank()) throw DomainRuleViolation("User id is required to resolve permissions.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required to resolve permissions.")

        val membership = repository.findMembershipsByUserId(userId)
            .firstOrNull { it.organizationId == organizationId && it.status == MembershipStatus.ACTIVE }
            ?: throw DomainRuleViolation("Active organization membership does not exist.")

        membership.assertCanAccessOrganization()

        val roles = repository.findRolesByIds(membership.roleIds)
            .filter { it.status == RoleStatus.ACTIVE }

        val foundRoleIds = roles.map { it.id }.toSet()
        val missingRoleIds = membership.roleIds - foundRoleIds
        if (missingRoleIds.isNotEmpty()) {
            throw DomainRuleViolation(
                "Membership references missing active roles: ${
                    missingRoleIds.sorted().joinToString()
                }."
            )
        }

        roles.forEach { role ->
            if (!role.isOrganizationRole && PermissionCatalog.ALL !in role.permissionKeys) {
                throw DomainRuleViolation("Organization membership cannot use non-organization role ${role.code}.")
            }
        }

        val unknownPermissions = roles
            .flatMap { it.permissionKeys }
            .filterNot { it in PermissionCatalog.known }
            .toSet()
        if (unknownPermissions.isNotEmpty()) {
            throw DomainRuleViolation(
                "Roles reference unknown permissions: ${
                    unknownPermissions.sorted().joinToString()
                }."
            )
        }

        val permissions = if (roles.any { PermissionCatalog.ALL in it.permissionKeys }) {
            setOf(PermissionCatalog.ALL)
        } else {
            roles.flatMap { it.permissionKeys }.toSet()
        }

        return EffectivePermissionContext(
            organizationId = organizationId,
            membership = membership,
            roles = roles.sortedBy { it.code },
            permissions = permissions,
        )
    }
}

data class ResolveEffectivePermissionsCommand(
    val userId: String,
    val organizationId: String,
)
