package com.hermes.domain.organization

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrganizationMembershipTest {

    @Test
    fun `owner membership is active`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")

        val membership = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = "org_1",
            userId = "usr_1",
            ownerRoleId = "role_owner",
            now = now,
        )

        assertEquals(MembershipStatus.ACTIVE, membership.status)
        assertEquals(setOf("role_owner"), membership.roleIds)
    }

    @Test
    fun `pending membership cannot access organization`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val membership = OrganizationMembership(
            id = "mem_1",
            organizationId = "org_1",
            userId = "usr_1",
            roleIds = setOf("role_operator"),
            status = MembershipStatus.PENDING_INVITATION,
            createdAt = now,
            updatedAt = now,
            invitedBy = "usr_owner",
        )

        assertFailsWith<DomainRuleViolation> {
            membership.assertCanAccessOrganization()
        }
    }
}
