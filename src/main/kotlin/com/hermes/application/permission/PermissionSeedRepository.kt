package com.hermes.application.permission

import com.hermes.domain.permission.PermissionDefinition

interface PermissionSeedRepository {
    fun upsertSystemPermission(permission: PermissionDefinition): PermissionSeedItemResult
}

data class PermissionSeedItemResult(
    val code: String,
    val outcome: PermissionSeedOutcome,
)

enum class PermissionSeedOutcome {
    CREATED,
    UPDATED,
    UNCHANGED,
    SKIPPED
}
