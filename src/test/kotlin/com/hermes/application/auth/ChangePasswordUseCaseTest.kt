package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChangePasswordUseCaseTest {
    @Test
    fun `changes temporary password and clears must change flag`() {
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
            passwordHash = TestPasswordHasher().hash("TempStrong1!".toCharArray()),
            now = now,
            temporary = true,
        )

        val useCase = ChangePasswordUseCase(
            userRepository = state,
            credentialRepository = state,
            sessionRepository = state,
            refreshTokenRepository = state,
            passwordPolicy = PasswordPolicy(minLength = 8),
            passwordHasher = TestPasswordHasher(),
            clock = phase5Clock(),
        )

        val result = useCase.execute(
            ChangePasswordCommand(
                userId = "usr_1",
                currentPassword = "TempStrong1!",
                newPassword = "NewStrong1!",
            )
        )

        assertEquals("usr_1", result.userId)
        assertFalse(state.credentials["cred_1"]!!.mustChangePassword)
    }
}
