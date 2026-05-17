package com.hermes.backend.auth

import com.hermes.application.auth.*

data class CredentialAdministrationModule(
    val inviteUserUseCase: InviteUserUseCase,
    val acceptInvitationUseCase: AcceptInvitationUseCase,
    val createTemporaryUserUseCase: CreateTemporaryUserUseCase,
    val changePasswordUseCase: ChangePasswordUseCase,
    val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    val blockUserUseCase: BlockUserUseCase,
    val unblockUserUseCase: UnblockUserUseCase,
)