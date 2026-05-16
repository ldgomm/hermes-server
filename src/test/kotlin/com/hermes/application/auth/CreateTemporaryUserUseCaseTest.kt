package com.hermes.application.auth

import com.hermes.domain.organization.Organization
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.SystemRoleCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTemporaryUserUseCaseTest {
    @Test
    fun `creates user with temporary password and active membership`() {
        val state = CredentialAdminState()
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

        val useCase = CreateTemporaryUserUseCase(
            userRepository = state,
            credentialRepository = state,
            organizationRepository = state,
            membershipRepository = state,
            roleRepository = state,
            idGenerator = Phase5FixedIdGenerator(),
            passwordPolicy = PasswordPolicy(minLength = 8),
            passwordHasher = TestPasswordHasher(),
            tokenGenerator = SecureTokenGenerator(),
            clock = phase5Clock(),
        )

        val result = useCase.execute(
            CreateTemporaryUserCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_CREATE),
                email = "cashier@hermes.local",
                displayName = "Cashier",
                roleIds = setOf("role_${SystemRoleCode.OPERATOR.code}"),
                temporaryPassword = "TempStrong1!",
            )
        )

        assertTrue(result.credential.temporaryPassword)
        assertTrue(result.credential.mustChangePassword)
        assertEquals("TempStrong1!", result.temporaryPassword)
    }
}
