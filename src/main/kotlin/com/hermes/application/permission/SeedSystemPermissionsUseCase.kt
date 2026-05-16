package com.hermes.application.permission

import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.permission.PermissionSeed
import com.hermes.domain.permission.PermissionSeedRules

class SeedSystemPermissionsUseCase(
    private val repository: PermissionSeedRepository,
) {
    fun execute(
        definitions: List<PermissionDefinition> = PermissionSeed.all,
    ): PermissionSeedResult {
        PermissionSeedRules.validate(definitions)

        val results = definitions.map { permission ->
            repository.upsertSystemPermission(permission)
        }

        return PermissionSeedResult(
            total = results.size,
            created = results.count { it.outcome == PermissionSeedOutcome.CREATED },
            updated = results.count { it.outcome == PermissionSeedOutcome.UPDATED },
            unchanged = results.count { it.outcome == PermissionSeedOutcome.UNCHANGED },
            skipped = results.count { it.outcome == PermissionSeedOutcome.SKIPPED },
        )
    }
}

data class PermissionSeedResult(
    val total: Int,
    val created: Int,
    val updated: Int,
    val unchanged: Int,
    val skipped: Int,
)
