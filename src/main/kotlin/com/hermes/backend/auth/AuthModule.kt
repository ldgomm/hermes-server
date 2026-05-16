package com.hermes.backend.auth

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.CreateOrganizationUseCase
import com.hermes.application.auth.CreateOwnerMembershipUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.auth.LoginUseCase
import com.hermes.application.auth.MeUseCase
import com.hermes.application.auth.RefreshSessionUseCase
import com.hermes.application.auth.RegisterOwnerUseCase
import com.hermes.application.auth.RegisterOwnerWorkspaceUseCase
import com.hermes.application.auth.RevokeSessionUseCase

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
)
