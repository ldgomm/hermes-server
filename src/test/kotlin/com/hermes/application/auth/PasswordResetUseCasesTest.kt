package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.user.User
import kotlin.test.*

class PasswordResetUseCasesTest {
    @Test
    fun `password reset request is neutral for unknown emails`() {
        val state = CredentialAdminState()
        val useCase = RequestPasswordResetUseCase(
            userRepository = state,
            resetTokenRepository = state,
            idGenerator = Phase5FixedIdGenerator(),
            tokenGenerator = SecureTokenGenerator(),
            exposeTokenInResult = true,
            clock = phase5Clock(),
        )

        val result = useCase.execute(RequestPasswordResetCommand(email = "missing@hermes.local"))

        assertTrue(result.accepted)
        assertNull(result.rawResetToken)
    }

    @Test
    fun `confirms password reset and revokes active reset token`() {
        val state = CredentialAdminState()
        val now = phase5Clock().instant()
        state.users["usr_1"] = User.createOwner(
            id = "usr_1",
            email = "user@hermes.local",
            displayName = "User",
            now = now,
        )
        state.credentials["cred_1"] = UserCredential.createPasswordCredential(
            id = "cred_1",
            userId = "usr_1",
            passwordHash = TestPasswordHasher().hash("OldStrong1!".toCharArray()),
            now = now,
        )

        val request = RequestPasswordResetUseCase(
            userRepository = state,
            resetTokenRepository = state,
            idGenerator = Phase5FixedIdGenerator(),
            tokenGenerator = SecureTokenGenerator(),
            exposeTokenInResult = true,
            clock = phase5Clock(),
        )
        val requested = request.execute(RequestPasswordResetCommand(email = "user@hermes.local"))
        assertNotNull(requested.rawResetToken)

        val confirm = ConfirmPasswordResetUseCase(
            userRepository = state,
            credentialRepository = state,
            resetTokenRepository = state,
            sessionRepository = state,
            refreshTokenRepository = state,
            passwordPolicy = PasswordPolicy(minLength = 8),
            passwordHasher = TestPasswordHasher(),
            clock = phase5Clock(),
        )

        val result = confirm.execute(
            ConfirmPasswordResetCommand(
                resetToken = requested.rawResetToken!!,
                newPassword = "NewStrong1!",
            )
        )

        assertEquals("usr_1", result.userId)
        assertTrue(result.resetToken.isUsed)
    }
}
