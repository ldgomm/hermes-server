package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoginUseCaseTest {

    @Test
    fun `active user can login and receives access and refresh tokens`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val credential = UserCredential.createPasswordCredential("cred_1", user.id, "hash:VeryStrong#2026", now)
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            credentials = mutableMapOf(user.id to credential),
        )
        val useCase = state.loginUseCase()

        val result = useCase.execute(
            LoginCommand(
                email = "OWNER@Hermes.Local",
                password = "VeryStrong#2026",
                userAgent = "test-agent",
                ipAddress = "127.0.0.1",
            ),
        )

        assertEquals(user.id, result.userId)
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertEquals(1, state.sessions.size)
        assertEquals(1, state.refreshTokens.size)
        assertEquals(CredentialAuditAction.LOGIN_SUCCEEDED, state.auditLogger.events.last().action)
    }

    @Test
    fun `temporary user can login and receives must change password flag`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "cashier@hermes.local", "Cashier", now)
        val credential = UserCredential.createPasswordCredential(
            id = "cred_1",
            userId = user.id,
            passwordHash = "hash:TempStrong#2026",
            now = now,
            temporary = true,
        )
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            credentials = mutableMapOf(user.id to credential),
        )

        val result = state.loginUseCase().execute(
            LoginCommand(
                email = "cashier@hermes.local",
                password = "TempStrong#2026",
                userAgent = "test-agent",
                ipAddress = "127.0.0.1",
            ),
        )

        assertEquals(user.id, result.userId)
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertTrue(result.mustChangePassword)
        assertEquals(1, state.sessions.size)
        assertEquals(1, state.refreshTokens.size)
    }

    @Test
    fun `invalid password is rejected and failed attempt is registered`() {
        val now = java.time.Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner("usr_1", "owner@hermes.local", "Owner", now)
        val credential = UserCredential.createPasswordCredential("cred_1", user.id, "hash:VeryStrong#2026", now)
        val state = FakeAuthState(
            users = mutableMapOf(user.id to user),
            credentials = mutableMapOf(user.id to credential),
        )
        val useCase = state.loginUseCase()

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(LoginCommand(email = "owner@hermes.local", password = "Wrong#2026"))
        }

        assertEquals(1, state.credentials.getValue(user.id).failedAttempts)
        assertEquals(CredentialAuditAction.LOGIN_FAILED, state.auditLogger.events.last().action)
    }
}