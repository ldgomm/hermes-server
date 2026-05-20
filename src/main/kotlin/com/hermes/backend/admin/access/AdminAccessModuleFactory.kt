package com.hermes.backend.admin.access

import com.hermes.application.admin.access.*
import com.hermes.application.auth.*
import com.hermes.infrastructure.mongo.admin.access.MongoAdminAccessRepository
import com.hermes.infrastructure.mongo.auth.MongoAuthStore
import com.hermes.infrastructure.mongo.auth.MongoCredentialAdminStore
import com.hermes.infrastructure.mongo.auth.MongoCredentialAuditLogger
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminAccessModuleFactory {
    fun fromMongo(
        client: MongoClient,
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): AdminAccessModule {
        val repository = MongoAdminAccessRepository(database)
        val authStore = MongoAuthStore(database = database, client = client)
        val credentialAdminStore = MongoCredentialAdminStore(database = database)
        val auditLogger = MongoCredentialAuditLogger(database)
        val passwordPolicy = PasswordPolicy()
        val passwordHasher = Pbkdf2PasswordHasher()
        val tokenGenerator = SecureTokenGenerator()
        val idGenerator = UuidAuthIdGenerator()

        val temporaryUserUseCase = CreateTemporaryUserUseCase(
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
        )

        return AdminAccessModule(
            createTemporaryUserUseCase = CreateAdminTemporaryUserUseCase(
                delegate = temporaryUserUseCase,
                accessRepository = repository,
                clock = clock,
            ),
            listUsersUseCase = ListAdminUsersUseCase(repository),
            getUserUseCase = GetAdminUserUseCase(repository),
            updateUserUseCase = UpdateAdminUserUseCase(repository, clock),
            blockUserUseCase = BlockAdminUserUseCase(repository, clock),
            unblockUserUseCase = UnblockAdminUserUseCase(repository, clock),
            revokeUserSessionsUseCase = RevokeAdminUserSessionsUseCase(repository, clock),
            resetUserPasswordUseCase = AdminResetUserPasswordUseCase(
                accessRepository = repository,
                credentialRepository = authStore,
                passwordPolicy = passwordPolicy,
                passwordHasher = passwordHasher,
                tokenGenerator = tokenGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            listInvitationsUseCase = ListAdminInvitationsUseCase(repository),
            getInvitationUseCase = GetAdminInvitationUseCase(repository),
            revokeInvitationUseCase = RevokeAdminInvitationUseCase(repository, clock),
            resendInvitationUseCase = ResendAdminInvitationUseCase(repository, clock = clock),
            listRolesUseCase = ListAdminRolesUseCase(repository),
            getRoleUseCase = GetAdminRoleUseCase(repository),
            createRoleUseCase = CreateAdminRoleUseCase(repository),
            updateRoleUseCase = UpdateAdminRoleUseCase(repository),
            changeRoleStatusUseCase = ChangeAdminRoleStatusUseCase(repository),
            listPermissionsUseCase = ListAdminPermissionsUseCase(repository),
        )
    }
}
