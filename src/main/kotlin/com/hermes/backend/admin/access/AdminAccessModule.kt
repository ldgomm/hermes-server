package com.hermes.backend.admin.access

import com.hermes.application.admin.access.*

data class AdminAccessModule(
    val listUsersUseCase: ListAdminUsersUseCase,
    val getUserUseCase: GetAdminUserUseCase,
    val updateUserUseCase: UpdateAdminUserUseCase,
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
