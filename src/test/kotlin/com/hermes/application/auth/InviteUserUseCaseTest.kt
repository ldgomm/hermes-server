package com.hermes.application.auth

import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InviteUserUseCaseTest {
    @Test
    fun `invites user and creates pending membership`() {
        val state = CredentialAdminState()
        val audit = RecordingAuditLogger()
        val now = phase5Clock().instant()
        state.organizations["org_1"] = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790000000001",
            legalName = "Hermes",
            commercialName = "Hermes",
            ownerUserId = "usr_owner",
            now = now,
        )

        val useCase = InviteUserUseCase(
            userRepository = state,
            organizationRepository = state,
            membershipRepository = state,
            roleRepository = state,
            invitationRepository = state,
            idGenerator = Phase5FixedIdGenerator(),
            tokenGenerator = SecureTokenGenerator(),
            auditLogger = audit,
            clock = phase5Clock(),
        )

        val result = useCase.execute(
            InviteUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                email = " Operator@Hermes.Local ",
                displayName = "Operator",
                roleIds = setOf("role_${SystemRoleCode.OPERATOR.code}"),
            )
        )

        assertEquals(UserStatus.INVITED, result.user.status)
        assertEquals(MembershipStatus.PENDING_INVITATION, result.membership.status)
        assertEquals(2, audit.events.size)
    }

    @Test
    fun `rejects invitation without permission`() {
        val state = CredentialAdminState()
        val useCase = InviteUserUseCase(
            userRepository = state,
            organizationRepository = state,
            membershipRepository = state,
            roleRepository = state,
            invitationRepository = state,
            idGenerator = Phase5FixedIdGenerator(),
            tokenGenerator = SecureTokenGenerator(),
            clock = phase5Clock(),
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                InviteUserCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = emptySet(),
                    email = "operator@hermes.local",
                    displayName = "Operator",
                    roleIds = setOf("role_${SystemRoleCode.OPERATOR.code}"),
                )
            )
        }
    }
}
