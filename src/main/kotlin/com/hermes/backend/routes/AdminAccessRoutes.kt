package com.hermes.backend.routes

import com.hermes.application.admin.access.AdminResetUserPasswordCommand
import com.hermes.application.admin.access.CreateAdminTemporaryUserCommand
import com.hermes.application.admin.access.CreateAdminInvitationCommand
import com.hermes.application.admin.access.UnblockAdminUserCommand
import com.hermes.application.admin.access.RevokeAdminUserSessionsCommand
import com.hermes.application.admin.access.BlockAdminUserCommand
import com.hermes.application.admin.access.ChangeAdminRoleStatusCommand
import com.hermes.application.admin.access.CreateAdminRoleCommand
import com.hermes.application.admin.access.GetAdminInvitationCommand
import com.hermes.application.admin.access.GetAdminRoleCommand
import com.hermes.application.admin.access.GetAdminUserCommand
import com.hermes.application.admin.access.ListAdminInvitationsCommand
import com.hermes.application.admin.access.ListAdminPermissionsCommand
import com.hermes.application.admin.access.ListAdminRolesCommand
import com.hermes.application.admin.access.ListAdminUsersCommand
import com.hermes.application.admin.access.RevokeAdminInvitationCommand
import com.hermes.application.admin.access.ResendAdminInvitationCommand
import com.hermes.application.admin.access.UpdateAdminRoleCommand
import com.hermes.application.admin.access.UpdateAdminUserCommand
import com.hermes.backend.admin.access.AdminAccessModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureAdminAccessRoutes(
    authModule: AuthModule,
    adminAccessModule: AdminAccessModule,
) {
    routing {
        adminAccessRoutes(
            authModule = authModule,
            adminAccessModule = adminAccessModule,
        )
    }
}

fun Route.adminAccessRoutes(
    authModule: AuthModule,
    adminAccessModule: AdminAccessModule,
) {
    route("/api/v1/admin") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/users") {
                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_VIEW) {
                    get {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.listUsersUseCase.execute(
                            ListAdminUsersCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                query = call.request.queryParameters["q"],
                                status = call.request.queryParameters["status"],
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    get("/{userId}") {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.getUserUseCase.execute(
                            GetAdminUserCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_CREATE) {
                    post("/temporary") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<CreateAdminTemporaryUserRequest>()
                        val result = adminAccessModule.createTemporaryUserUseCase.execute(
                            CreateAdminTemporaryUserCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                email = request.email,
                                displayName = request.displayName,
                                roleIds = request.roleIds,
                                temporaryPassword = request.temporaryPassword,
                                phone = request.phone,
                                reason = request.reason,
                                ipAddress = call.adminAccessClientIpAddress(),
                                userAgent = call.request.header(HttpHeaders.UserAgent),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }

                    put("/{userId}") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<UpdateAdminUserRequest>()
                        val result = adminAccessModule.updateUserUseCase.execute(
                            UpdateAdminUserCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                                displayName = request.displayName,
                                phone = request.phone,
                                clearPhone = request.clearPhone,
                                roleIds = request.roleIds,
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_BLOCK) {
                    post("/{userId}/block") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminUserActionRequest>()
                        val result = adminAccessModule.blockUserUseCase.execute(
                            BlockAdminUserCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_UNBLOCK) {
                    post("/{userId}/unblock") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminUserActionRequest>()
                        val result = adminAccessModule.unblockUserUseCase.execute(
                            UnblockAdminUserCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_RESET_PASSWORD) {
                    post("/{userId}/reset-password") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminResetUserPasswordRequest>()
                        val result = adminAccessModule.resetUserPasswordUseCase.execute(
                            AdminResetUserPasswordCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                                temporaryPassword = request.temporaryPassword,
                                revokeSessions = request.revokeSessions,
                                reason = request.reason,
                                ipAddress = call.adminAccessClientIpAddress(),
                                userAgent = call.request.header(HttpHeaders.UserAgent),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE) {
                    post("/{userId}/revoke-sessions") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminRevokeUserSessionsRequest>()
                        val result = adminAccessModule.revokeUserSessionsUseCase.execute(
                            RevokeAdminUserSessionsCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                userId = call.requiredAdminAccessPath("userId"),
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }

            route("/invitations") {
                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_INVITE) {
                    post {
                        val context = call.hermesAuthContext()
                        val request = call.receive<CreateAdminInvitationRequest>()
                        val result = adminAccessModule.createInvitationUseCase.execute(
                            CreateAdminInvitationCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                email = request.email,
                                displayName = request.displayName,
                                roleIds = request.roleIds,
                                reason = request.reason,
                                ipAddress = call.adminAccessClientIpAddress(),
                                userAgent = call.request.header(HttpHeaders.UserAgent),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }

                    get {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.listInvitationsUseCase.execute(
                            ListAdminInvitationsCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                status = call.request.queryParameters["status"],
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    get("/{invitationId}") {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.getInvitationUseCase.execute(
                            GetAdminInvitationCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                invitationId = call.requiredAdminAccessPath("invitationId"),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    post("/{invitationId}/resend") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminInvitationActionRequest>()
                        val result = adminAccessModule.resendInvitationUseCase.execute(
                            ResendAdminInvitationCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                invitationId = call.requiredAdminAccessPath("invitationId"),
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    post("/{invitationId}/revoke") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminInvitationActionRequest>()
                        val result = adminAccessModule.revokeInvitationUseCase.execute(
                            RevokeAdminInvitationCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                invitationId = call.requiredAdminAccessPath("invitationId"),
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }

            route("/roles") {
                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_ROLES_VIEW) {
                    get {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.listRolesUseCase.execute(
                            ListAdminRolesCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                includeSystemTemplates = call.request.queryParameters["includeSystemTemplates"] != "false",
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    get("/{roleId}") {
                        val context = call.hermesAuthContext()
                        val result = adminAccessModule.getRoleUseCase.execute(
                            GetAdminRoleCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                roleId = call.requiredAdminAccessPath("roleId"),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CREDENTIALS_ROLES_MANAGE) {
                    post {
                        val context = call.hermesAuthContext()
                        val request = call.receive<CreateAdminRoleRequest>()
                        val result = adminAccessModule.createRoleUseCase.execute(
                            CreateAdminRoleCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                code = request.code,
                                name = request.name,
                                description = request.description,
                                permissionKeys = request.permissionKeys,
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }

                    put("/{roleId}") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<UpdateAdminRoleRequest>()
                        val result = adminAccessModule.updateRoleUseCase.execute(
                            UpdateAdminRoleCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                roleId = call.requiredAdminAccessPath("roleId"),
                                name = request.name,
                                description = request.description,
                                permissionKeys = request.permissionKeys,
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    post("/{roleId}/activate") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminRoleActionRequest>()
                        val result = adminAccessModule.changeRoleStatusUseCase.activate(
                            ChangeAdminRoleStatusCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                roleId = call.requiredAdminAccessPath("roleId"),
                                targetStatus = "ACTIVE",
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    post("/{roleId}/deactivate") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminRoleActionRequest>()
                        val result = adminAccessModule.changeRoleStatusUseCase.deactivate(
                            ChangeAdminRoleStatusCommand(
                                organizationId = call.adminAccessOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                roleId = call.requiredAdminAccessPath("roleId"),
                                targetStatus = "INACTIVE",
                                reason = request.reason,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }

            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_ROLES_VIEW) {
                get("/permissions") {
                    val context = call.hermesAuthContext()
                    val result = adminAccessModule.listPermissionsUseCase.execute(
                        ListAdminPermissionsCommand(
                            organizationId = call.adminAccessOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            includeReserved = call.request.queryParameters["includeReserved"] == "true",
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.adminAccessOrganizationId(): String =
    hermesAuthContext().activeOrganization?.organization?.id
        ?: throw DomainRuleViolation("Active organization is required.")

private fun ApplicationCall.requiredAdminAccessPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

private fun ApplicationCall.adminAccessClientIpAddress(): String? =
    request.header("X-Forwarded-For")
        ?.split(',')
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }
