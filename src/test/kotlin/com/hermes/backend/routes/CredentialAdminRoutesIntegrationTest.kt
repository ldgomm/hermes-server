package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.backend.auth.CredentialAdministrationModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.UserSession
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CredentialAdminRoutesIntegrationTest {

    @Test
    fun `invite user rejects request without token`() = testApplication {
        val fixture = fixture(SystemRoleCode.ORGANIZATION_OWNER)

        application {
            configureSerialization()
            routing {
                credentialAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    credentialAdministrationModule = fixture.credentialAdministrationModule,
                    revokeSessionUseCase = fixture.revokeSessionUseCase,
                )
            }
        }

        val response = client.post("/organizations/org_1/users/invite") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "cashier@hermes.local",
                  "displayName": "Cashier",
                  "roleIds": ["role_operator"]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `invite user rejects actor without invite permission`() = testApplication {
        val fixture = fixture(SystemRoleCode.OPERATOR)

        application {
            configureSerialization()
            routing {
                credentialAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    credentialAdministrationModule = fixture.credentialAdministrationModule,
                    revokeSessionUseCase = fixture.revokeSessionUseCase,
                )
            }
        }

        val response = client.post("/organizations/org_1/users/invite") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "cashier@hermes.local",
                  "displayName": "Cashier",
                  "roleIds": ["role_operator"]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `invite user uses actor and permissions from authenticated context`() = testApplication {
        val fixture = fixture(SystemRoleCode.ORGANIZATION_OWNER)

        application {
            configureSerialization()
            routing {
                credentialAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    credentialAdministrationModule = fixture.credentialAdministrationModule,
                    revokeSessionUseCase = fixture.revokeSessionUseCase,
                )
            }
        }

        val response = client.post("/organizations/org_1/users/invite") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "cashier@hermes.local",
                  "displayName": "Cashier",
                  "roleIds": ["role_operator"]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(1, fixture.credentialState.invitations.size)

        val invitation = fixture.credentialState.invitations.values.single()
        assertEquals("org_1", invitation.organizationId)
        assertEquals("usr_actor", invitation.invitedByUserId)
        assertEquals("cashier@hermes.local", invitation.email)
    }

    @Test
    fun `block user rejects actor without block permission`() = testApplication {
        val fixture = fixture(SystemRoleCode.OPERATOR)
        fixture.addTargetUser()

        application {
            configureSerialization()
            routing {
                credentialAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    credentialAdministrationModule = fixture.credentialAdministrationModule,
                    revokeSessionUseCase = fixture.revokeSessionUseCase,
                )
            }
        }

        val response = client.post("/organizations/org_1/users/usr_target/block") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Security review"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `block user with permission suspends membership and revokes sessions`() = testApplication {
        val fixture = fixture(SystemRoleCode.ORGANIZATION_OWNER)
        fixture.addTargetUser()

        application {
            configureSerialization()
            routing {
                credentialAdminRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    credentialAdministrationModule = fixture.credentialAdministrationModule,
                    revokeSessionUseCase = fixture.revokeSessionUseCase,
                )
            }
        }

        val response = client.post("/organizations/org_1/users/usr_target/block") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Security review"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val revokedSession = fixture.credentialState.sessions.getValue("ses_target")

        assertEquals(
            com.hermes.domain.session.UserSessionStatus.REVOKED,
            revokedSession.status,
        )
        assertTrue(revokedSession.revokedAt != null)
    }

    private fun fixture(roleCode: SystemRoleCode): Fixture {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)

        val authRepository = FakeAuthContextRepository()
        val credentialState = CredentialAdminState()

        val actor = User.createOwner(
            id = "usr_actor",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790000000001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes Demo",
            ownerUserId = actor.id,
            now = now,
        )
        val role = RoleSeed.get(roleCode)
        val membership = OrganizationMembership.owner(
            id = "mem_actor",
            organizationId = organization.id,
            userId = actor.id,
            ownerRoleId = role.id,
            now = now,
        )
        val session = UserSession.create(
            id = "ses_actor",
            userId = actor.id,
            now = now,
            expiresAt = now.plusSeconds(3600),
        )

        authRepository.users[actor.id] = actor
        authRepository.organizations[organization.id] = organization
        authRepository.memberships[membership.id] = membership
        authRepository.roles[role.id] = role
        authRepository.sessions[session.id] = session

        credentialState.organizations[organization.id] = organization

        val jwtTokenService = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-auth-tests-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val accessToken = jwtTokenService.issueAccessToken(
            userId = actor.id,
            sessionId = session.id,
            issuedAt = now,
        ).token

        val authenticateRequestUseCase = AuthenticateRequestUseCase(
            repository = authRepository,
            jwtTokenService = jwtTokenService,
            clock = clock,
        )
        val activeOrganizationResolverUseCase = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissionResolverUseCase = EffectivePermissionResolverUseCase(authRepository)

        val idGenerator = Phase5FixedIdGenerator()
        val tokenGenerator = SecureTokenGenerator()
        val passwordHasher = TestPasswordHasher()
        val passwordPolicy = PasswordPolicy(minLength = 8)

        val revokeSessionUseCase = RevokeSessionUseCase(
            sessionRepository = credentialState,
            refreshTokenRepository = credentialState,
            clock = clock,
        )

        val credentialAdministrationModule = CredentialAdministrationModule(
            inviteUserUseCase = InviteUserUseCase(
                userRepository = credentialState,
                organizationRepository = credentialState,
                membershipRepository = credentialState,
                roleRepository = credentialState,
                invitationRepository = credentialState,
                idGenerator = idGenerator,
                tokenGenerator = tokenGenerator,
                clock = clock,
            ),
            acceptInvitationUseCase = AcceptInvitationUseCase(
                userRepository = credentialState,
                credentialRepository = credentialState,
                membershipRepository = credentialState,
                invitationRepository = credentialState,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                clock = clock,
            ),
            createTemporaryUserUseCase = CreateTemporaryUserUseCase(
                userRepository = credentialState,
                credentialRepository = credentialState,
                organizationRepository = credentialState,
                membershipRepository = credentialState,
                roleRepository = credentialState,
                idGenerator = idGenerator,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                tokenGenerator = tokenGenerator,
                clock = clock,
            ),
            changePasswordUseCase = ChangePasswordUseCase(
                userRepository = credentialState,
                credentialRepository = credentialState,
                sessionRepository = credentialState,
                refreshTokenRepository = credentialState,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                clock = clock,
            ),
            requestPasswordResetUseCase = RequestPasswordResetUseCase(
                userRepository = credentialState,
                resetTokenRepository = credentialState,
                idGenerator = idGenerator,
                tokenGenerator = tokenGenerator,
                exposeTokenInResult = true,
                clock = clock,
            ),
            confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(
                userRepository = credentialState,
                credentialRepository = credentialState,
                resetTokenRepository = credentialState,
                sessionRepository = credentialState,
                refreshTokenRepository = credentialState,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                clock = clock,
            ),
            blockUserUseCase = BlockUserUseCase(
                userRepository = credentialState,
                membershipRepository = credentialState,
                sessionRepository = credentialState,
                refreshTokenRepository = credentialState,
                clock = clock,
            ),
            unblockUserUseCase = UnblockUserUseCase(
                userRepository = credentialState,
                membershipRepository = credentialState,
                clock = clock,
            ),
        )

        return Fixture(
            credentialState = credentialState,
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            credentialAdministrationModule = credentialAdministrationModule,
            revokeSessionUseCase = revokeSessionUseCase,
            accessToken = accessToken,
            now = now,
        )
    }

    private data class Fixture(
        val credentialState: CredentialAdminState,
        val authenticateRequestUseCase: AuthenticateRequestUseCase,
        val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
        val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
        val credentialAdministrationModule: CredentialAdministrationModule,
        val revokeSessionUseCase: RevokeSessionUseCase,
        val accessToken: String,
        val now: Instant,
    ) {
        fun addTargetUser() {
            val target = User.createOwner(
                id = "usr_target",
                email = "target@hermes.local",
                displayName = "Target User",
                now = now,
            )
            val membership = OrganizationMembership.owner(
                id = "mem_target",
                organizationId = "org_1",
                userId = target.id,
                ownerRoleId = "role_operator",
                now = now,
            )
            val session = UserSession.create(
                id = "ses_target",
                userId = target.id,
                now = now,
                expiresAt = now.plusSeconds(3600),
            )

            credentialState.users[target.id] = target
            credentialState.memberships[membership.id] = membership
            credentialState.sessions[session.id] = session
        }
    }
}