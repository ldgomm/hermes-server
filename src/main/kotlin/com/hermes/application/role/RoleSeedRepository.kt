package com.hermes.application.role

import com.hermes.domain.role.RoleDefinition

data class RoleSeedResult(
    val requested: Int,
    val inserted: Int,
    val updated: Int,
    val unchanged: Int = 0,
) {
    val affected: Int
        get() = inserted + updated
}

interface RoleSeedRepository {
    suspend fun findExistingRoleCodes(codes: Set<String>): Set<String>

    suspend fun findExistingPermissionCodes(codes: Set<String>): Set<String>

    suspend fun upsertSystemRoles(roles: List<RoleDefinition>): RoleSeedResult
}
