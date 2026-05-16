package com.hermes.backend.auth

import com.hermes.application.auth.*
import com.hermes.backend.config.AppConfig
import com.hermes.infrastructure.mongo.auth.AuthMongoBootstrap
import com.hermes.infrastructure.mongo.auth.MongoAuthStore
import com.hermes.infrastructure.mongo.auth.MongoCredentialAuditLogger
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

        val store = MongoAuthStore(database = database, client = client)
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
            repository = store,
            jwtTokenService = jwtTokenService,
            clock = clock,
        )
        val activeOrganizationResolverUseCase = ActiveOrganizationResolverUseCase(store)
        val effectivePermissionResolverUseCase = EffectivePermissionResolverUseCase(store)

        return AuthModule(
            registerOwnerUseCase = RegisterOwnerUseCase(
                userRepository = store,
                credentialRepository = store,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            registerOwnerWorkspaceUseCase = RegisterOwnerWorkspaceUseCase(
                repository = store,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createOrganizationUseCase = CreateOrganizationUseCase(
                userRepository = store,
                organizationRepository = store,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createOwnerMembershipUseCase = CreateOwnerMembershipUseCase(
                userRepository = store,
                organizationRepository = store,
                membershipRepository = store,
                roleLookupRepository = store,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            loginUseCase = LoginUseCase(
                userRepository = store,
                credentialRepository = store,
                sessionRepository = store,
                refreshTokenRepository = store,
                passwordHasher = passwordHasher,
                sessionFactory = sessionFactory,
                securityPolicy = securityPolicy,
                auditLogger = auditLogger,
                clock = clock,
            ),
            refreshSessionUseCase = RefreshSessionUseCase(
                userRepository = store,
                sessionRepository = store,
                refreshTokenRepository = store,
                jwtTokenService = jwtTokenService,
                tokenGenerator = tokenGenerator,
                idGenerator = idGenerator,
                securityPolicy = securityPolicy,
                auditLogger = auditLogger,
                clock = clock,
            ),
            revokeSessionUseCase = RevokeSessionUseCase(
                sessionRepository = store,
                refreshTokenRepository = store,
                auditLogger = auditLogger,
                clock = clock,
            ),
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            meUseCase = MeUseCase(
                repository = store,
                authenticateRequestUseCase = authenticateRequestUseCase,
                activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
                effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            ),
        )
    }
}
