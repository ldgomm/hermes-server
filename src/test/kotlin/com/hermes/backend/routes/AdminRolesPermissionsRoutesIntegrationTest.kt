package com.hermes.backend.routes

import com.hermes.application.admin.access.*
import com.hermes.application.auth.*
import com.hermes.backend.admin.access.AdminAccessModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.CredentialAdministrationModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.credential.PasswordResetToken
import com.hermes.domain.credential.UserCredential
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.session.UserSession
import com.hermes.domain.session.UserSessionStatus
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminRolesPermissionsRoutesIntegrationTest {
    @Test
    fun `PUT admin roles updates custom role safely`() = testApplication {
        val fixture = fixture13B6()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.put("/api/v1/admin/roles/role_cashier") {
            auth13B6(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "Caja actualizada",
                  "description": "Puede ver ventas y cobrar",
                  "permissionKeys": ["${PermissionCatalog.SALES_VIEW}", "${PermissionCatalog.PAYMENTS_COLLECT}"],
                  "reason": "Actualizar permisos operativos"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Caja actualizada"), body)
        assertTrue(body.contains(PermissionCatalog.PAYMENTS_COLLECT), body)
        assertEquals("Caja actualizada", fixture.repository.roles.getValue("role_cashier").name)
    }

    @Test
    fun `POST admin roles deactivate custom role`() = testApplication {
        val fixture = fixture13B6()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.post("/api/v1/admin/roles/role_cashier/deactivate") {
            auth13B6(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{ "reason": "Rol ya no se usa" }""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"INACTIVE\""), body)
        assertEquals(RoleStatus.INACTIVE, fixture.repository.roles.getValue("role_cashier").status)
    }

    @Test
    fun `POST admin roles activate custom role`() = testApplication {
        val fixture = fixture13B6().also {
            it.repository.roles["role_cashier"] =
                it.repository.roles.getValue("role_cashier").copy(status = RoleStatus.INACTIVE)
        }

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.post("/api/v1/admin/roles/role_cashier/activate") {
            auth13B6(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{ "reason": "Reactivar rol de caja" }""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"status\":\"ACTIVE\""), body)
        assertEquals(RoleStatus.ACTIVE, fixture.repository.roles.getValue("role_cashier").status)
    }

    @Test
    fun `PUT admin roles rejects removing last role manager`() = testApplication {
        val fixture = fixture13B6()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.put("/api/v1/admin/roles/role_owner") {
            auth13B6(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "permissionKeys": ["${PermissionCatalog.CREDENTIALS_USERS_CREATE}"],
                  "reason": "No debe quedar sin gestor de roles"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertTrue(response.bodyAsText().contains("last active role manager"))
    }

    @Test
    fun `PUT admin roles rejects actor without manage permission`() = testApplication {
        val fixture = fixture13B6(permissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW))

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.put("/api/v1/admin/roles/role_cashier") {
            auth13B6(fixture.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "No permitido",
                  "reason": "Sin permiso"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `GET admin permissions lists public active permission catalog`() = testApplication {
        val fixture = fixture13B6(permissions = setOf(PermissionCatalog.CREDENTIALS_ROLES_VIEW))

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.get("/api/v1/admin/permissions") {
            auth13B6(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(PermissionCatalog.CREDENTIALS_USERS_VIEW), body)
        assertTrue(body.contains("requiresAudit"), body)
    }

    @Test
    fun `GET admin permissions rejects actor without roles view permission`() = testApplication {
        val fixture = fixture13B6(permissions = setOf(PermissionCatalog.SALES_VIEW))

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminAccessRoutes(
                    authModule = fixture.authModule,
                    adminAccessModule = fixture.adminAccessModule,
                )
            }
        }

        val response = client.get("/api/v1/admin/permissions") {
            auth13B6(fixture.accessToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun HttpRequestBuilder.auth13B6(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header("X-Organization-Id", "org_1")
    }

    private fun fixture13B6(
        permissions: Set<String> = setOf(
            PermissionCatalog.CREDENTIALS_ROLES_VIEW,
            PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
        ),
    ): Fixture13B6 {
        val now = Instant.parse("2026-05-19T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val authStore = RouteAuthStore13B6()
        val repository = RouteAdminAccessRepository13B6(now)

        val owner = User.createOwner(
            id = "usr_owner",
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
            ownerUserId = owner.id,
            now = now,
        )
        val actorRole = repository.roles.getValue("role_owner").copy(permissionKeys = permissions)
        val membership = OrganizationMembership.owner(
            id = "mem_owner",
            organizationId = organization.id,
            userId = owner.id,
            ownerRoleId = actorRole.id,
            now = now,
        )
        val session = UserSession.create(
            id = "ses_owner",
            userId = owner.id,
            now = now,
            expiresAt = now.plusSeconds(3600),
        )

        authStore.users[owner.id] = owner
        authStore.organizations[organization.id] = organization
        authStore.roles[actorRole.id] = actorRole
        authStore.memberships[membership.id] = membership
        authStore.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-admin-roles-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val accessToken = jwt.issueAccessToken(owner.id, session.id, now).token

        return Fixture13B6(
            authModule = authStore.authModule(jwt, clock),
            adminAccessModule = repository.adminAccessModule(authStore, clock),
            repository = repository,
            accessToken = accessToken,
        )
    }

    private data class Fixture13B6(
        val authModule: AuthModule,
        val adminAccessModule: AdminAccessModule,
        val repository: RouteAdminAccessRepository13B6,
        val accessToken: String,
    )
}

private class RouteAdminAccessRepository13B6(
    private val now: Instant,
) : AdminAccessRepository {
    val users: MutableMap<String, User> = linkedMapOf(
        "usr_owner" to User.createOwner(
            id = "usr_owner",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
    )

    val memberships: MutableMap<String, OrganizationMembership> = linkedMapOf(
        "mem_owner" to OrganizationMembership.owner(
            id = "mem_owner",
            organizationId = "org_1",
            userId = "usr_owner",
            ownerRoleId = "role_owner",
            now = now,
        )
    )

    val roles: MutableMap<String, RoleDefinition> = linkedMapOf(
        "role_owner" to routeRole13B6(
            id = "role_owner",
            code = "owner",
            name = "Owner",
            description = "Owner role",
            permissionKeys = setOf(
                PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
                PermissionCatalog.CREDENTIALS_USERS_CREATE,
            ),
        ),
        "role_cashier" to routeRole13B6(
            id = "role_cashier",
            code = "cashier",
            name = "Cashier",
            description = "Cashier role",
            permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
        ),
    )

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> = memberships.values
        .filter { it.organizationId == organizationId }
        .filter { status.isNullOrBlank() || it.status.name.equals(status, ignoreCase = true) }
        .take(limit.coerceIn(1, 250))
        .mapNotNull { membership ->
            val user = users[membership.userId] ?: return@mapNotNull null
            AdminUserAccessRecord(
                user = user,
                membership = membership,
                roles = findRolesByIds(membership.roleIds),
                activeSessionCount = 0,
            )
        }

    override fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord? =
        listUserAccess(organizationId, null, null, 250).firstOrNull { it.user.id == userId }

    override fun findUserById(userId: String): User? = users[userId]

    override fun updateUser(user: User) {
        users[user.id] = user
    }

    override fun findMembership(organizationId: String, userId: String): OrganizationMembership? =
        memberships.values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun updateMembership(membership: OrganizationMembership) {
        memberships[membership.id] = membership
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> =
        roleIds.mapNotNull { roles[it] }

    override fun listRoles(organizationId: String, includeSystemTemplates: Boolean): List<RoleDefinition> =
        roles.values.filter { it.organizationId == organizationId && it.status != RoleStatus.ARCHIVED }

    override fun findRole(organizationId: String, roleId: String): RoleDefinition? =
        roles[roleId]?.takeIf { it.organizationId == organizationId }

    override fun existsRoleCode(organizationId: String, code: String, excludeRoleId: String?): Boolean =
        roles.values.any { it.organizationId == organizationId && it.code == code && it.id != excludeRoleId }

    override fun createRole(role: RoleDefinition) {
        roles[role.id] = role
    }

    override fun updateRole(role: RoleDefinition) {
        roles[role.id] = role
    }

    override fun listInvitations(organizationId: String, status: String?, limit: Int): List<Invitation> = emptyList()
    override fun findInvitation(organizationId: String, invitationId: String): Invitation? = null
    override fun updateInvitation(invitation: Invitation) = Unit

    override fun listPermissionDefinitions(includeReserved: Boolean): List<PermissionDefinition> =
        if (includeReserved) PermissionCatalog.definitions else PermissionCatalog.active

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> = emptyList()
    override fun updateSession(session: UserSession) = Unit
    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int = 0

    override fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String?,
        adminPermissionKeys: Set<String>,
    ): Int = memberships.values
        .filter { it.organizationId == organizationId && it.status == MembershipStatus.ACTIVE && it.userId != excludingUserId }
        .count { membership ->
            findRolesByIds(membership.roleIds).any { role ->
                role.status == RoleStatus.ACTIVE && role.permissionKeys.any { it in adminPermissionKeys }
            }
        }
}

private fun RouteAdminAccessRepository13B6.adminAccessModule(
    authStore: RouteAuthStore13B6,
    clock: Clock,
): AdminAccessModule {
    val idGenerator = RouteAuthIdGenerator13B6()
    val passwordPolicy = PasswordPolicy(minLength = 8)
    val passwordHasher = RoutePasswordHasher13B6
    val tokenGenerator = SecureTokenGenerator()

    val temporaryUserUseCase = CreateTemporaryUserUseCase(
        userRepository = authStore,
        credentialRepository = authStore,
        organizationRepository = authStore,
        membershipRepository = authStore,
        roleRepository = authStore,
        idGenerator = idGenerator,
        passwordPolicy = passwordPolicy,
        passwordHasher = passwordHasher,
        tokenGenerator = tokenGenerator,
        clock = clock,
    )

    val inviteUserUseCase = InviteUserUseCase(
        userRepository = authStore,
        organizationRepository = authStore,
        membershipRepository = authStore,
        roleRepository = authStore,
        invitationRepository = authStore,
        idGenerator = idGenerator,
        tokenGenerator = tokenGenerator,
        clock = clock,
    )

    return AdminAccessModule(
        createTemporaryUserUseCase = CreateAdminTemporaryUserUseCase(
            delegate = temporaryUserUseCase,
            accessRepository = this,
            clock = clock,
        ),
        listUsersUseCase = ListAdminUsersUseCase(this),
        getUserUseCase = GetAdminUserUseCase(this),
        updateUserUseCase = UpdateAdminUserUseCase(this, clock),
        blockUserUseCase = BlockAdminUserUseCase(this, clock),
        unblockUserUseCase = UnblockAdminUserUseCase(this, clock),
        revokeUserSessionsUseCase = RevokeAdminUserSessionsUseCase(this, clock),
        resetUserPasswordUseCase = AdminResetUserPasswordUseCase(
            accessRepository = this,
            credentialRepository = authStore,
            passwordPolicy = passwordPolicy,
            passwordHasher = passwordHasher,
            tokenGenerator = tokenGenerator,
            clock = clock,
        ),
        createInvitationUseCase = CreateAdminInvitationUseCase(
            delegate = inviteUserUseCase,
            accessRepository = this,
            clock = clock,
        ),
        listInvitationsUseCase = ListAdminInvitationsUseCase(this),
        getInvitationUseCase = GetAdminInvitationUseCase(this),
        revokeInvitationUseCase = RevokeAdminInvitationUseCase(this, clock),
        resendInvitationUseCase = ResendAdminInvitationUseCase(this, clock = clock),
        listRolesUseCase = ListAdminRolesUseCase(this),
        getRoleUseCase = GetAdminRoleUseCase(this),
        createRoleUseCase = CreateAdminRoleUseCase(this, clock = clock),
        updateRoleUseCase = UpdateAdminRoleUseCase(this, clock = clock),
        changeRoleStatusUseCase = ChangeAdminRoleStatusUseCase(this, clock = clock),
        listPermissionsUseCase = ListAdminPermissionsUseCase(this),
    )
}

private class RouteAuthStore13B6 :
    AuthContextRepository,
    UserRepository,
    UserCredentialRepository,
    OrganizationRepository,
    OrganizationMembershipRepository,
    AuthRoleLookupRepository,
    UserSessionRepository,
    RefreshTokenRepository,
    OwnerWorkspaceRepository,
    MembershipMutationRepository,
    RoleQueryRepository,
    InvitationRepository,
    PasswordResetTokenRepository {
    val users: MutableMap<String, User> = linkedMapOf()
    val credentials: MutableMap<String, UserCredential> = linkedMapOf()
    val organizations: MutableMap<String, Organization> = linkedMapOf()
    val memberships: MutableMap<String, OrganizationMembership> = linkedMapOf()
    val roles: MutableMap<String, RoleDefinition> = linkedMapOf()
    val sessions: MutableMap<String, UserSession> = linkedMapOf()
    val refreshTokens: MutableMap<String, RefreshToken> = linkedMapOf()
    val invitations: MutableMap<String, Invitation> = linkedMapOf()
    val resetTokens: MutableMap<String, PasswordResetToken> = linkedMapOf()

    override fun findUserById(userId: String): User? = users[userId]
    override fun findSessionById(sessionId: String): UserSession? = sessions[sessionId]
    override fun findMembershipsByUserId(userId: String): List<OrganizationMembership> =
        memberships.values.filter { it.userId == userId }

    override fun findOrganizationById(organizationId: String): Organization? = organizations[organizationId]
    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> = roleIds.mapNotNull { roles[it] }

    override fun existsUserByEmail(email: String): Boolean = users.values.any { it.email == email }
    override fun findUserByEmail(email: String): User? = users.values.firstOrNull { it.email == email }
    override fun create(user: User) {
        users[user.id] = user
    }

    override fun update(user: User) {
        users[user.id] = user
    }

    override fun findByUserId(userId: String): UserCredential? = credentials[userId]
    override fun create(credential: UserCredential) {
        credentials[credential.userId] = credential
    }

    override fun update(credential: UserCredential) {
        credentials[credential.userId] = credential
    }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.values.any { it.countryCode == countryCode && it.taxId == taxId }

    override fun create(organization: Organization) {
        organizations[organization.id] = organization
    }

    override fun existsByOrganizationIdAndUserId(organizationId: String, userId: String): Boolean =
        memberships.values.any { it.organizationId == organizationId && it.userId == userId }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        memberships.values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun create(membership: OrganizationMembership) {
        memberships[membership.id] = membership
    }

    override fun update(membership: OrganizationMembership) {
        memberships[membership.id] = membership
    }

    override fun findSystemRoleByCode(code: String): RoleDefinition? =
        roles.values.firstOrNull { it.code == code }

    override fun findRoleById(roleId: String): RoleDefinition? = roles[roleId]

    override fun create(session: UserSession) {
        sessions[session.id] = session
    }

    override fun findActiveByUserId(userId: String): List<UserSession> =
        sessions.values.filter { it.userId == userId && it.status == UserSessionStatus.ACTIVE }

    override fun update(session: UserSession) {
        sessions[session.id] = session
    }

    override fun create(refreshToken: RefreshToken) {
        refreshTokens[refreshToken.id] = refreshToken
    }

    override fun findRefreshTokenByHash(tokenHash: String): RefreshToken? =
        refreshTokens.values.firstOrNull { it.tokenHash == tokenHash }

    override fun findActiveBySessionId(sessionId: String): List<RefreshToken> =
        refreshTokens.values.filter { it.sessionId == sessionId && !it.isRevoked && !it.isUsed }

    override fun update(refreshToken: RefreshToken) {
        refreshTokens[refreshToken.id] = refreshToken
    }

    override fun revokeActiveBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int {
        val active = refreshTokens.values.filter { it.sessionId in sessionIds && !it.isRevoked }
        active.forEach { refreshTokens[it.id] = it.copy(revokedAt = revokedAt, version = it.version + 1) }
        return active.size
    }

    override fun rotate(oldToken: RefreshToken, newToken: RefreshToken) {
        refreshTokens[oldToken.id] = oldToken
        refreshTokens[newToken.id] = newToken
    }

    override fun emailExists(email: String): Boolean = existsUserByEmail(email)
    override fun organizationTaxIdExists(countryCode: String, taxId: String): Boolean =
        existsByTaxId(countryCode, taxId)

    override fun createOwnerWorkspace(
        user: User,
        credential: UserCredential,
        organization: Organization,
        membership: OrganizationMembership,
    ) {
        create(user)
        create(credential)
        create(organization)
        create(membership)
    }

    override fun create(invitation: Invitation) {
        invitations[invitation.id] = invitation
    }

    override fun findInvitationByTokenHash(tokenHash: String): Invitation? =
        invitations.values.firstOrNull { it.tokenHash == tokenHash }

    override fun findPendingByOrganizationAndEmail(organizationId: String, email: String): Invitation? =
        invitations.values.firstOrNull {
            it.organizationId == organizationId && it.email == email && it.status == InvitationStatus.PENDING
        }

    override fun update(invitation: Invitation) {
        invitations[invitation.id] = invitation
    }

    override fun create(token: PasswordResetToken) {
        resetTokens[token.id] = token
    }

    override fun findPasswordResetTokenByHash(tokenHash: String): PasswordResetToken? =
        resetTokens.values.firstOrNull { it.tokenHash == tokenHash }

    override fun revokeActiveForUser(userId: String, revokedAt: Instant): Int {
        val active = resetTokens.values.filter { it.userId == userId && !it.isRevoked && !it.isUsed }
        active.forEach { resetTokens[it.id] = it.copy(revokedAt = revokedAt, version = it.version + 1) }
        return active.size
    }

    override fun update(token: PasswordResetToken) {
        resetTokens[token.id] = token
    }

    fun authModule(jwt: HmacJwtTokenService, clock: Clock): AuthModule {
        val idGenerator = RouteAuthIdGenerator13B6()
        val passwordPolicy = PasswordPolicy(minLength = 8)
        val passwordHasher = RoutePasswordHasher13B6
        val tokenGenerator = SecureTokenGenerator()
        val securityPolicy = AuthSecurityPolicy()
        val sessionFactory = AuthSessionFactory(
            idGenerator = idGenerator,
            tokenGenerator = tokenGenerator,
            jwtTokenService = jwt,
            policy = securityPolicy,
        )
        val authenticateRequestUseCase = AuthenticateRequestUseCase(this, jwt, clock)
        val activeOrganizationResolverUseCase = ActiveOrganizationResolverUseCase(this)
        val effectivePermissionResolverUseCase = EffectivePermissionResolverUseCase(this)

        return AuthModule(
            registerOwnerUseCase = RegisterOwnerUseCase(
                userRepository = this,
                credentialRepository = this,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                clock = clock,
            ),
            registerOwnerWorkspaceUseCase = RegisterOwnerWorkspaceUseCase(
                repository = this,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                clock = clock,
            ),
            createOrganizationUseCase = CreateOrganizationUseCase(
                userRepository = this,
                organizationRepository = this,
                idGenerator = idGenerator,
                clock = clock,
            ),
            createOwnerMembershipUseCase = CreateOwnerMembershipUseCase(
                userRepository = this,
                organizationRepository = this,
                membershipRepository = this,
                roleLookupRepository = this,
                idGenerator = idGenerator,
                clock = clock,
            ),
            loginUseCase = LoginUseCase(
                userRepository = this,
                credentialRepository = this,
                sessionRepository = this,
                refreshTokenRepository = this,
                passwordHasher = passwordHasher,
                sessionFactory = sessionFactory,
                securityPolicy = securityPolicy,
                clock = clock,
            ),
            refreshSessionUseCase = RefreshSessionUseCase(
                userRepository = this,
                sessionRepository = this,
                refreshTokenRepository = this,
                jwtTokenService = jwt,
                tokenGenerator = tokenGenerator,
                idGenerator = idGenerator,
                securityPolicy = securityPolicy,
                clock = clock,
            ),
            revokeSessionUseCase = RevokeSessionUseCase(
                sessionRepository = this,
                refreshTokenRepository = this,
                clock = clock,
            ),
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            meUseCase = MeUseCase(
                repository = this,
                authenticateRequestUseCase = authenticateRequestUseCase,
                activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
                effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            ),
            credentialAdministrationModule = CredentialAdministrationModule(
                inviteUserUseCase = InviteUserUseCase(
                    userRepository = this,
                    organizationRepository = this,
                    membershipRepository = this,
                    roleRepository = this,
                    invitationRepository = this,
                    idGenerator = idGenerator,
                    tokenGenerator = tokenGenerator,
                    clock = clock,
                ),
                acceptInvitationUseCase = AcceptInvitationUseCase(
                    userRepository = this,
                    credentialRepository = this,
                    membershipRepository = this,
                    invitationRepository = this,
                    passwordPolicy = passwordPolicy,
                    passwordHasher = passwordHasher,
                    idGenerator = idGenerator,
                    clock = clock,
                ),
                createTemporaryUserUseCase = CreateTemporaryUserUseCase(
                    userRepository = this,
                    credentialRepository = this,
                    organizationRepository = this,
                    membershipRepository = this,
                    roleRepository = this,
                    idGenerator = idGenerator,
                    passwordPolicy = passwordPolicy,
                    passwordHasher = passwordHasher,
                    tokenGenerator = tokenGenerator,
                    clock = clock,
                ),
                changePasswordUseCase = ChangePasswordUseCase(
                    userRepository = this,
                    credentialRepository = this,
                    sessionRepository = this,
                    refreshTokenRepository = this,
                    passwordPolicy = passwordPolicy,
                    passwordHasher = passwordHasher,
                    clock = clock,
                ),
                requestPasswordResetUseCase = RequestPasswordResetUseCase(
                    userRepository = this,
                    resetTokenRepository = this,
                    idGenerator = idGenerator,
                    tokenGenerator = tokenGenerator,
                    clock = clock,
                ),
                confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(
                    userRepository = this,
                    credentialRepository = this,
                    resetTokenRepository = this,
                    sessionRepository = this,
                    refreshTokenRepository = this,
                    passwordPolicy = passwordPolicy,
                    passwordHasher = passwordHasher,
                    clock = clock,
                ),
                blockUserUseCase = com.hermes.application.auth.BlockUserUseCase(
                    userRepository = this,
                    membershipRepository = this,
                    sessionRepository = this,
                    refreshTokenRepository = this,
                    clock = clock,
                ),
                unblockUserUseCase = com.hermes.application.auth.UnblockUserUseCase(
                    userRepository = this,
                    membershipRepository = this,
                    clock = clock,
                ),
            ),
        )
    }
}

private object RoutePasswordHasher13B6 : PasswordHasher {
    override fun hash(password: CharArray): String = "hash:${String(password)}"
    override fun verify(password: CharArray, encodedHash: String): Boolean = encodedHash == hash(password)
}

private class RouteAuthIdGenerator13B6 : AuthIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "${prefix}_$next"
    }
}

private fun routeRole13B6(
    id: String,
    code: String,
    name: String,
    description: String,
    permissionKeys: Set<String>,
    status: RoleStatus = RoleStatus.ACTIVE,
): RoleDefinition = RoleDefinition(
    id = id,
    code = code,
    organizationId = "org_1",
    scope = RoleScope.ORGANIZATION,
    type = RoleType.CUSTOM,
    name = name,
    description = description,
    permissionKeys = permissionKeys,
    systemRole = false,
    critical = false,
    editable = true,
    status = status,
)
