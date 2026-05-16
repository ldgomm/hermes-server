package com.hermes.backend.auth

import com.hermes.application.auth.AcceptInvitationUseCase
import com.hermes.application.auth.BlockUserUseCase
import com.hermes.application.auth.ChangePasswordUseCase
import com.hermes.application.auth.ConfirmPasswordResetUseCase
import com.hermes.application.auth.CreateTemporaryUserUseCase
import com.hermes.application.auth.InviteUserUseCase
import com.hermes.application.auth.RequestPasswordResetUseCase
import com.hermes.application.auth.UnblockUserUseCase

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
