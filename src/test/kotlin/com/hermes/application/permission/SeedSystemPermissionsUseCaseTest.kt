package com.hermes.application.permission

import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.permission.PermissionSeed
import kotlin.test.Test
import kotlin.test.assertEquals

class SeedSystemPermissionsUseCaseTest {

    @Test
    fun `seeds system permissions idempotently`() {
        val repository = InMemoryPermissionSeedRepository()
        val useCase = SeedSystemPermissionsUseCase(repository)

        val firstRun = useCase.execute()
        val secondRun = useCase.execute()

        assertEquals(PermissionSeed.all.size, firstRun.total)
        assertEquals(PermissionSeed.all.size, firstRun.created)
        assertEquals(0, firstRun.updated)
        assertEquals(0, firstRun.unchanged)

        assertEquals(PermissionSeed.all.size, secondRun.total)
        assertEquals(0, secondRun.created)
        assertEquals(0, secondRun.updated)
        assertEquals(PermissionSeed.all.size, secondRun.unchanged)

        assertEquals(PermissionSeed.all.size, repository.items.size)
    }

    @Test
    fun `updates system managed permission when definition changes`() {
        val repository = InMemoryPermissionSeedRepository()
        val useCase = SeedSystemPermissionsUseCase(repository)

        val permission = PermissionSeed.all.first()
        useCase.execute(listOf(permission))

        val changed = permission.copy(description = permission.description + " Updated.")
        val result = useCase.execute(listOf(changed))

        assertEquals(1, result.updated)
        assertEquals(changed.description, repository.items.getValue(changed.code).description)
    }
}

private class InMemoryPermissionSeedRepository : PermissionSeedRepository {
    val items: MutableMap<String, PermissionDefinition> = linkedMapOf()

    override fun upsertSystemPermission(permission: PermissionDefinition): PermissionSeedItemResult {
        val existing = items[permission.code]

        if (existing == null) {
            items[permission.code] = permission
            return PermissionSeedItemResult(permission.code, PermissionSeedOutcome.CREATED)
        }

        if (existing == permission) {
            return PermissionSeedItemResult(permission.code, PermissionSeedOutcome.UNCHANGED)
        }

        if (!existing.systemManaged) {
            return PermissionSeedItemResult(permission.code, PermissionSeedOutcome.SKIPPED)
        }

        items[permission.code] = permission
        return PermissionSeedItemResult(permission.code, PermissionSeedOutcome.UPDATED)
    }
}
