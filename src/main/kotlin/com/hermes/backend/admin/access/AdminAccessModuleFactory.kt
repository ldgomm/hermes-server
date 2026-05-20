package com.hermes.backend.admin.access

import com.hermes.application.admin.access.AdminResetUserPasswordUseCase
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
    ): AdminAccessModule {
        val repository = MongoAdminAccessRepository(database)
        val authStore = MongoAuthStore(database = database, client = client)
        val auditLogger = MongoCredentialAuditLogger(database)

        return AdminAccessModule(
            listUsersUseCase = ListAdminUsersUseCase(repository),
            getUserUseCase = GetAdminUserUseCase(repository),
            updateUserUseCase = UpdateAdminUserUseCase(repository, clock),
            blockUserUseCase = BlockAdminUserUseCase(repository, clock),
            unblockUserUseCase = UnblockAdminUserUseCase(repository, clock),
            revokeUserSessionsUseCase = RevokeAdminUserSessionsUseCase(repository, clock),
            resetUserPasswordUseCase = AdminResetUserPasswordUseCase(
                accessRepository = repository,
                credentialRepository = authStore,
                passwordPolicy = PasswordPolicy(),
                passwordHasher = Pbkdf2PasswordHasher(),
                tokenGenerator = SecureTokenGenerator(),
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
