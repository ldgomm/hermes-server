package com.hermes.application.role

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.RoleSeedRules
import com.hermes.domain.shared.DomainRuleViolation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SeedSystemRolesUseCaseTest {
    @Test
    fun `seeds system roles when required permissions exist`() {
        runBlocking {
            val repository = FakeRoleSeedRepository(
                existingPermissionCodes = RoleSeed.requiredPermissionCodes(),
            )
            val useCase = SeedSystemRolesUseCase(repository)

            val result = useCase.execute()

            assertEquals(RoleSeed.all.size, result.requested)
            assertEquals(RoleSeed.all.size, result.inserted)
            assertEquals(SystemRoleCodeSize, repository.savedRoles.size)
        }
    }

    @Test
    fun `rejects seed when a required permission is missing`() {
        runBlocking {
            val missing = PermissionCatalog.SALES_CREATE
            val repository = FakeRoleSeedRepository(
                existingPermissionCodes = RoleSeed.requiredPermissionCodes() - missing,
            )
            val useCase = SeedSystemRolesUseCase(repository)

            assertFailsWith<DomainRuleViolation> {
                useCase.execute()
            }
        }
    }

    private class FakeRoleSeedRepository(
        private val existingPermissionCodes: Set<String>,
    ) : RoleSeedRepository {
        val savedRoles = mutableListOf<RoleDefinition>()

        override suspend fun findExistingRoleCodes(codes: Set<String>): Set<String> {
            return emptySet()
        }

        override suspend fun findExistingPermissionCodes(codes: Set<String>): Set<String> {
            return existingPermissionCodes.intersect(codes)
        }

        override suspend fun upsertSystemRoles(roles: List<RoleDefinition>): RoleSeedResult {
            RoleSeedRules.validate(roles, knownPermissionCodes = existingPermissionCodes + PermissionCatalog.ALL)
            savedRoles += roles
            return RoleSeedResult(
                requested = roles.size,
                inserted = roles.size,
                updated = 0,
            )
        }
    }

    companion object {
        private const val SystemRoleCodeSize = 8
    }
}
