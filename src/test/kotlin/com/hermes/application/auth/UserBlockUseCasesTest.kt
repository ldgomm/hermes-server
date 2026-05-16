package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.user.User
import com.hermes.domain.user.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class UserBlockUseCasesTest {
    @Test
    fun `blocks and unblocks user`() {
        val state = CredentialAdminState()
        val now = phase5Clock().instant()
        state.users["usr_target"] = User.createOwner(
            id = "usr_target",
            email = "target@hermes.local",
            displayName = "Target",
            now = now,
        )
        state.memberships["mem_1"] = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = "org_1",
            userId = "usr_target",
            ownerRoleId = "role_operator",
            now = now,
        )

        val block = BlockUserUseCase(
            userRepository = state,
            membershipRepository = state,
            sessionRepository = state,
            refreshTokenRepository = state,
            clock = phase5Clock(),
        )
        val blocked = block.execute(
            BlockUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                targetUserId = "usr_target",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_BLOCK),
                reason = "security review",
            )
        )
        assertEquals(UserStatus.BLOCKED, blocked.user.status)
        assertEquals(MembershipStatus.SUSPENDED, blocked.membership!!.status)

        val unblock = UnblockUserUseCase(
            userRepository = state,
            membershipRepository = state,
            clock = phase5Clock(),
        )
        val unblocked = unblock.execute(
            UnblockUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                targetUserId = "usr_target",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_UNBLOCK),
                reason = "review completed",
            )
        )
        assertEquals(UserStatus.ACTIVE, unblocked.user.status)
        assertEquals(MembershipStatus.ACTIVE, unblocked.membership!!.status)
    }
}
