package com.hermes.application.role

import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.RoleSeedRules
import com.hermes.domain.shared.DomainRuleViolation

class SeedSystemRolesUseCase(
    private val repository: RoleSeedRepository,
) {
    suspend fun execute(roles: List<RoleDefinition> = RoleSeed.all): RoleSeedResult {
        RoleSeedRules.validate(roles)

        val requiredPermissionCodes = RoleSeedRules.requiredPermissions(roles)
        val existingPermissionCodes = repository.findExistingPermissionCodes(requiredPermissionCodes)
        val missingPermissionCodes = requiredPermissionCodes - existingPermissionCodes

        if (missingPermissionCodes.isNotEmpty()) {
            throw DomainRuleViolation(
                "Cannot seed roles because permissions are missing: " +
                        missingPermissionCodes.sorted().joinToString()
            )
        }

        val roleCodes = roles.map { it.code }.toSet()
        repository.findExistingRoleCodes(roleCodes)

        return repository.upsertSystemRoles(roles)
    }
}
