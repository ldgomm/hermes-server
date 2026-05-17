package com.hermes.backend.auth

import com.hermes.application.auth.*
import com.hermes.backend.config.AppConfig
import com.hermes.infrastructure.mongo.auth.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import java.time.Clock
import java.time.Duration

object AuthModuleFactory {
    fun fromMongo(
        client: MongoClient,
        database: MongoDatabase,
        config: AppConfig,
        clock: Clock = Clock.systemUTC(),
    ): AuthModule {
        AuthMongoBootstrap.ensureIndexes(database)
        CredentialAdminMongoBootstrap.ensureIndexes(database)

        val authStore = MongoAuthStore(database = database, client = client)
        val credentialAdminStore = MongoCredentialAdminStore(database = database)
        val auditLogger = MongoCredentialAuditLogger(database)

        val idGenerator = UuidAuthIdGenerator()
        val tokenGenerator = SecureTokenGenerator()
        val passwordHasher = Pbkdf2PasswordHasher()
        val passwordPolicy = PasswordPolicy()

        val securityPolicy = AuthSecurityPolicy(
            accessTokenTtl = Duration.ofSeconds(config.auth.accessTokenTtlSeconds),
            refreshTokenTtl = Duration.ofDays(config.auth.refreshTokenTtlDays),
            sessionTtl = Duration.ofDays(config.auth.sessionTtlDays),
            maxFailedLoginAttempts = config.auth.maxFailedLoginAttempts,
            credentialLockDuration = Duration.ofMinutes(config.auth.credentialLockDurationMinutes),
        )

        val jwtTokenService = HmacJwtTokenService(
            secret = config.auth.jwtSecret,
            issuer = config.auth.jwtIssuer,
            accessTokenTtlSeconds = config.auth.accessTokenTtlSeconds,
        )

        val sessionFactory = AuthSessionFactory(
            idGenerator = idGenerator,
            tokenGenerator = tokenGenerator,
            jwtTokenService = jwtTokenService,
            policy = securityPolicy,
        )

        val authenticateRequestUseCase = AuthenticateRequestUseCase(
            repository = authStore,
            jwtTokenService = jwtTokenService,
            clock = clock,
        )

        val activeOrganizationResolverUseCase = ActiveOrganizationResolverUseCase(authStore)
        val effectivePermissionResolverUseCase = EffectivePermissionResolverUseCase(authStore)

        val registerOwnerUseCase = RegisterOwnerUseCase(
            userRepository = authStore,
            credentialRepository = authStore,
            passwordPolicy = passwordPolicy,
            passwordHasher = passwordHasher,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = clock,
        )

        val registerOwnerWorkspaceUseCase = RegisterOwnerWorkspaceUseCase(
            repository = authStore,
            passwordPolicy = passwordPolicy,
            passwordHasher = passwordHasher,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = clock,
        )

        val createOrganizationUseCase = CreateOrganizationUseCase(
            userRepository = authStore,
            organizationRepository = authStore,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = clock,
        )

        val createOwnerMembershipUseCase = CreateOwnerMembershipUseCase(
            userRepository = authStore,
            organizationRepository = authStore,
            membershipRepository = authStore,
            roleLookupRepository = authStore,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = clock,
        )

        val loginUseCase = LoginUseCase(
            userRepository = authStore,
            credentialRepository = authStore,
            sessionRepository = authStore,
            refreshTokenRepository = authStore,
            passwordHasher = passwordHasher,
            sessionFactory = sessionFactory,
            securityPolicy = securityPolicy,
            auditLogger = auditLogger,
            clock = clock,
        )

        val refreshSessionUseCase = RefreshSessionUseCase(
            userRepository = authStore,
            sessionRepository = authStore,
            refreshTokenRepository = authStore,
            jwtTokenService = jwtTokenService,
            tokenGenerator = tokenGenerator,
            idGenerator = idGenerator,
            securityPolicy = securityPolicy,
            auditLogger = auditLogger,
            clock = clock,
        )

        val revokeSessionUseCase = RevokeSessionUseCase(
            sessionRepository = authStore,
            refreshTokenRepository = authStore,
            auditLogger = auditLogger,
            clock = clock,
        )

        val credentialAdministrationModule = CredentialAdministrationModule(
            inviteUserUseCase = InviteUserUseCase(
                userRepository = authStore,
                organizationRepository = authStore,
                membershipRepository = credentialAdminStore,
                roleRepository = credentialAdminStore,
                invitationRepository = credentialAdminStore,
                idGenerator = idGenerator,
                tokenGenerator = tokenGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            acceptInvitationUseCase = AcceptInvitationUseCase(
                userRepository = authStore,
                credentialRepository = authStore,
                membershipRepository = credentialAdminStore,
                invitationRepository = credentialAdminStore,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createTemporaryUserUseCase = CreateTemporaryUserUseCase(
                userRepository = authStore,
                credentialRepository = authStore,
                organizationRepository = authStore,
                membershipRepository = credentialAdminStore,
                roleRepository = credentialAdminStore,
                idGenerator = idGenerator,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                tokenGenerator = tokenGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            changePasswordUseCase = ChangePasswordUseCase(
                userRepository = authStore,
                credentialRepository = authStore,
                sessionRepository = authStore,
                refreshTokenRepository = authStore,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                auditLogger = auditLogger,
                clock = clock,
            ),
            requestPasswordResetUseCase = RequestPasswordResetUseCase(
                userRepository = authStore,
                resetTokenRepository = credentialAdminStore,
                idGenerator = idGenerator,
                tokenGenerator = tokenGenerator,
                auditLogger = auditLogger,
                exposeTokenInResult = false,
                clock = clock,
            ),
            confirmPasswordResetUseCase = ConfirmPasswordResetUseCase(
                userRepository = authStore,
                credentialRepository = authStore,
                resetTokenRepository = credentialAdminStore,
                sessionRepository = authStore,
                refreshTokenRepository = authStore,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                auditLogger = auditLogger,
                clock = clock,
            ),
            blockUserUseCase = BlockUserUseCase(
                userRepository = authStore,
                membershipRepository = credentialAdminStore,
                sessionRepository = authStore,
                refreshTokenRepository = authStore,
                auditLogger = auditLogger,
                clock = clock,
            ),
            unblockUserUseCase = UnblockUserUseCase(
                userRepository = authStore,
                membershipRepository = credentialAdminStore,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )

        return AuthModule(
            registerOwnerUseCase = registerOwnerUseCase,
            registerOwnerWorkspaceUseCase = registerOwnerWorkspaceUseCase,
            createOrganizationUseCase = createOrganizationUseCase,
            createOwnerMembershipUseCase = createOwnerMembershipUseCase,
            loginUseCase = loginUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
            revokeSessionUseCase = revokeSessionUseCase,
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            meUseCase = MeUseCase(
                repository = authStore,
                authenticateRequestUseCase = authenticateRequestUseCase,
                activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
                effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            ),
            credentialAdministrationModule = credentialAdministrationModule,
        )
    }
}