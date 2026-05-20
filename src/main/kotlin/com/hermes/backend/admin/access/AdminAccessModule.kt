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

data class AdminAccessModule(
    val listUsersUseCase: ListAdminUsersUseCase,
    val getUserUseCase: GetAdminUserUseCase,
    val updateUserUseCase: UpdateAdminUserUseCase,
    val blockUserUseCase: BlockAdminUserUseCase,
    val unblockUserUseCase: UnblockAdminUserUseCase,
    val revokeUserSessionsUseCase: RevokeAdminUserSessionsUseCase,
    val resetUserPasswordUseCase: AdminResetUserPasswordUseCase,
    val listInvitationsUseCase: ListAdminInvitationsUseCase,
    val getInvitationUseCase: GetAdminInvitationUseCase,
    val revokeInvitationUseCase: RevokeAdminInvitationUseCase,
    val resendInvitationUseCase: ResendAdminInvitationUseCase,
    val listRolesUseCase: ListAdminRolesUseCase,
    val getRoleUseCase: GetAdminRoleUseCase,
    val createRoleUseCase: CreateAdminRoleUseCase,
    val updateRoleUseCase: UpdateAdminRoleUseCase,
    val changeRoleStatusUseCase: ChangeAdminRoleStatusUseCase,
    val listPermissionsUseCase: ListAdminPermissionsUseCase,
)
