package com.hermes.application.auth

import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegisterOwnerWorkspaceUseCaseTest {
    @Test
    fun `creates owner user credential organization and owner membership`() {
        val repository = FakeOwnerWorkspaceRepository()
        val useCase = RegisterOwnerWorkspaceUseCase(
            repository = repository,
            passwordPolicy = PasswordPolicy(),
            passwordHasher = FakePasswordHasher(),
            idGenerator = FixedAuthIdGenerator(),
            auditLogger = repository.auditLogger,
            clock = fixedClock(),
        )

        val result = useCase.execute(
            RegisterOwnerWorkspaceCommand(
                ownerEmail = " OWNER@Hermes.Local ",
                ownerDisplayName = "José Owner",
                ownerPassword = "VeryStrong#2026",
                organizationLegalName = "Hermes Test S.A.S.",
                organizationCommercialName = "Hermes Test",
                organizationTaxId = "1799999999001",
            ),
        )

        assertEquals("owner@hermes.local", result.user.email)
        assertEquals(result.user.id, result.organization.ownerUserId)
        assertEquals(result.organization.id, result.membership.organizationId)
        assertEquals(result.user.id, result.membership.userId)
        assertEquals(setOf(RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER).id), result.membership.roleIds)
        assertEquals(5, repository.auditLogger.events.size)
        assertTrue(repository.users.containsKey(result.user.id))
    }

    @Test
    fun `rejects duplicated owner email`() {
        val repository = FakeOwnerWorkspaceRepository(existingEmails = mutableSetOf("owner@hermes.local"))
        val useCase = RegisterOwnerWorkspaceUseCase(
            repository = repository,
            passwordPolicy = PasswordPolicy(),
            passwordHasher = FakePasswordHasher(),
            idGenerator = FixedAuthIdGenerator(),
            clock = fixedClock(),
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                RegisterOwnerWorkspaceCommand(
                    ownerEmail = "owner@hermes.local",
                    ownerDisplayName = "Owner",
                    ownerPassword = "VeryStrong#2026",
                    organizationLegalName = "Hermes Test S.A.S.",
                    organizationCommercialName = "Hermes Test",
                    organizationTaxId = "1799999999001",
                ),
            )
        }
    }
}
