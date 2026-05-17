package com.hermes.backend.auth

import com.hermes.application.auth.*

data class AuthModule(
    val registerOwnerUseCase: RegisterOwnerUseCase,
    val registerOwnerWorkspaceUseCase: RegisterOwnerWorkspaceUseCase,
    val createOrganizationUseCase: CreateOrganizationUseCase,
    val createOwnerMembershipUseCase: CreateOwnerMembershipUseCase,
    val loginUseCase: LoginUseCase,
    val refreshSessionUseCase: RefreshSessionUseCase,
    val revokeSessionUseCase: RevokeSessionUseCase,
    val authenticateRequestUseCase: AuthenticateRequestUseCase,
    val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    val meUseCase: MeUseCase,
    val credentialAdministrationModule: CredentialAdministrationModule,
)