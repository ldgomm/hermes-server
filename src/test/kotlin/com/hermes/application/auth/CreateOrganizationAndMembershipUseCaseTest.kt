package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateOrganizationAndMembershipUseCaseTest {
    @Test
    fun `creates organization for active owner user`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val state = FakeAuthState(users = mutableMapOf(user.id to user))
        val useCase = CreateOrganizationUseCase(
            userRepository = state,
            organizationRepository = state,
            idGenerator = FixedAuthIdGenerator(),
            auditLogger = state.auditLogger,
            clock = fixedClock(),
        )

        val result = useCase.execute(
            CreateOrganizationCommand(
                ownerUserId = user.id,
                legalName = "Hermes Test S.A.S.",
                commercialName = "Hermes Test",
                taxId = "1799999999001",
            ),
        )

        assertEquals(user.id, result.organization.ownerUserId)
        assertEquals("EC", result.organization.countryCode)
        assertEquals("1799999999001", result.organization.taxId)
        assertEquals(CredentialAuditAction.ORGANIZATION_CREATED, state.auditLogger.events.last().action)
    }

    @Test
    fun `creates owner membership with organization owner role`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1799999999001",
            legalName = "Hermes Test S.A.S.",
            commercialName = "Hermes Test",
            ownerUserId = user.id,
            now = now,
        )
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            organizations = mutableMapOf(organization.id to organization),
        )
        val useCase = CreateOwnerMembershipUseCase(
            userRepository = state,
            organizationRepository = state,
            membershipRepository = state,
            roleLookupRepository = state,
            idGenerator = FixedAuthIdGenerator(),
            auditLogger = state.auditLogger,
            clock = fixedClock(),
        )

        val result = useCase.execute(
            CreateOwnerMembershipCommand(
                userId = user.id,
                organizationId = organization.id,
            ),
        )

        assertEquals(organization.id, result.membership.organizationId)
        assertEquals(user.id, result.membership.userId)
        assertEquals(setOf(RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER).id), result.membership.roleIds)
    }

    @Test
    fun `rejects owner membership for non owner user`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val owner = User.createOwner("usr_owner", "owner@hermes.local", "Owner", now)
        val other = User.createOwner("usr_other", "other@hermes.local", "Other", now)
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1799999999001",
            legalName = "Hermes Test S.A.S.",
            commercialName = "Hermes Test",
            ownerUserId = owner.id,
            now = now,
        )
        val state = FakeAuthState(
            users = mutableMapOf(owner.id to owner, other.id to other),
            organizations = mutableMapOf(organization.id to organization),
        )
        val useCase = CreateOwnerMembershipUseCase(
            userRepository = state,
            organizationRepository = state,
            membershipRepository = state,
            roleLookupRepository = state,
            idGenerator = FixedAuthIdGenerator(),
            clock = fixedClock(),
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateOwnerMembershipCommand(
                    userId = other.id,
                    organizationId = organization.id,
                ),
            )
        }
    }
}
