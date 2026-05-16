package com.hermes.application.auth

import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.user.User
import com.hermes.domain.user.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AcceptInvitationUseCaseTest {
    @Test
    fun `accepts invitation activates user membership and creates credential`() {
        val state = CredentialAdminState()
        val now = phase5Clock().instant()
        val rawToken = "raw_invitation_token"
        state.users["usr_1"] = User(
            id = "usr_1",
            email = "operator@hermes.local",
            displayName = "Operator",
            status = UserStatus.INVITED,
            createdAt = now,
            updatedAt = now,
        )
        state.memberships["mem_1"] = OrganizationMembership(
            id = "mem_1",
            organizationId = "org_1",
            userId = "usr_1",
            roleIds = setOf("role_operator"),
            status = MembershipStatus.PENDING_INVITATION,
            invitedBy = "usr_owner",
            createdAt = now,
            updatedAt = now,
        )
        state.invitations["inv_1"] = Invitation(
            id = "inv_1",
            organizationId = "org_1",
            email = "operator@hermes.local",
            invitedByUserId = "usr_owner",
            roleIds = setOf("role_operator"),
            tokenHash = TokenHasher.sha256(rawToken),
            status = InvitationStatus.PENDING,
            createdAt = now,
            expiresAt = now.plusSeconds(3600),
        )

        val useCase = AcceptInvitationUseCase(
            userRepository = state,
            credentialRepository = state,
            membershipRepository = state,
            invitationRepository = state,
            passwordPolicy = PasswordPolicy(minLength = 8),
            passwordHasher = TestPasswordHasher(),
            idGenerator = Phase5FixedIdGenerator(),
            clock = phase5Clock(),
        )

        val result = useCase.execute(
            AcceptInvitationCommand(
                invitationToken = rawToken,
                password = "NewStrong1!",
            )
        )

        assertEquals(UserStatus.ACTIVE, result.user.status)
        assertEquals(MembershipStatus.ACTIVE, result.membership.status)
        assertEquals(InvitationStatus.ACCEPTED, result.invitation.status)
    }
}
