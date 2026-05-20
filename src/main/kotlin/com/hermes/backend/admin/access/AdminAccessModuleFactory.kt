package com.hermes.backend.admin.access

import com.hermes.application.admin.access.AdminResetUserPasswordUseCase
import com.hermes.application.admin.access.CreateAdminTemporaryUserUseCase
import com.hermes.application.admin.access.CreateAdminInvitationUseCase
import com.hermes.application.admin.access.UnblockAdminUserUseCase
import com.hermes.application.admin.access.RevokeAdminUserSessionsUseCase
import com.hermes.application.admin.access.BlockAdminUserUseCase
import com.hermes.application.admin.access.ChangeAdminRoleStatusUseCase
import com.hermes.application.admin.access.CreateAdminRoleUseCase
import com.hermes.application.admin.access.GetAdminInvitationUseCase
import com.hermes.application.admin.access.GetAdminRoleUseCase
import com.hermes.application.admin.access.GetAdminUserUseCase
import com.hermes.application.admin.access.ListAdminInvitationsUseCase
import com.hermes.application.admin.access.ListAdminPermissionsUseCase
import com.hermes.application.admin.access.ListAdminRolesUseCase
import com.hermes.application.admin.access.ListAdminUsersUseCase
import com.hermes.application.admin.access.RevokeAdminInvitationUseCase
import com.hermes.application.admin.access.ResendAdminInvitationUseCase
import com.hermes.application.admin.access.UpdateAdminRoleUseCase
import com.hermes.application.admin.access.UpdateAdminUserUseCase
import com.hermes.application.auth.CreateTemporaryUserUseCase
import com.hermes.application.auth.InviteUserUseCase
import com.hermes.application.auth.InvitationDelivery
import com.hermes.application.auth.NoopInvitationDelivery
import com.hermes.application.auth.UuidAuthIdGenerator
import com.hermes.infrastructure.mongo.auth.MongoCredentialAdminStore
import com.hermes.application.auth.PasswordPolicy
import com.hermes.application.auth.Pbkdf2PasswordHasher
import com.hermes.application.auth.SecureTokenGenerator
import com.hermes.infrastructure.mongo.admin.access.MongoAdminAccessRepository
import com.hermes.infrastructure.mongo.auth.MongoAuthStore
import com.hermes.infrastructure.mongo.auth.MongoCredentialAuditLogger
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminAccessModuleFactory {
    fun fromMongo(
        client: MongoClient,
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
        invitationDelivery: InvitationDelivery = NoopInvitationDelivery,
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

        val inviteUserUseCase = InviteUserUseCase(
            userRepository = authStore,
            organizationRepository = authStore,
            membershipRepository = credentialAdminStore,
            roleRepository = credentialAdminStore,
            invitationRepository = credentialAdminStore,
            idGenerator = idGenerator,
            tokenGenerator = tokenGenerator,
            delivery = invitationDelivery,
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
            createInvitationUseCase = CreateAdminInvitationUseCase(
                delegate = inviteUserUseCase,
                accessRepository = repository,
                clock = clock,
            ),
            listInvitationsUseCase = ListAdminInvitationsUseCase(repository),
            getInvitationUseCase = GetAdminInvitationUseCase(repository),
            revokeInvitationUseCase = RevokeAdminInvitationUseCase(repository, clock),
            resendInvitationUseCase = ResendAdminInvitationUseCase(
                repository = repository,
                delivery = invitationDelivery,
                clock = clock,
            ),
            listRolesUseCase = ListAdminRolesUseCase(repository),
            getRoleUseCase = GetAdminRoleUseCase(repository),
            createRoleUseCase = CreateAdminRoleUseCase(repository),
            updateRoleUseCase = UpdateAdminRoleUseCase(repository),
            changeRoleStatusUseCase = ChangeAdminRoleStatusUseCase(repository),
            listPermissionsUseCase = ListAdminPermissionsUseCase(repository),
        )
    }
}
