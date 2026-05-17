package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.backend.auth.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCredentialAdminRoutes(
    authModule: AuthModule,
) {
    routing {
        credentialAdminRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            credentialAdministrationModule = authModule.credentialAdministrationModule,
            revokeSessionUseCase = authModule.revokeSessionUseCase,
        )
    }
}

fun Route.credentialAdminRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    credentialAdministrationModule: CredentialAdministrationModule,
    revokeSessionUseCase: RevokeSessionUseCase,
) {
    route("/auth") {
        post("/invitations/accept") {
            val request = call.receive<AcceptInvitationRequest>()
            val result = credentialAdministrationModule.acceptInvitationUseCase.execute(
                AcceptInvitationCommand(
                    invitationToken = request.invitationToken,
                    password = request.password,
                    displayName = request.displayName,
                    phone = request.phone,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                ),
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/password-reset/request") {
            val request = call.receive<RequestPasswordResetRequest>()
            val result = credentialAdministrationModule.requestPasswordResetUseCase.execute(
                RequestPasswordResetCommand(
                    email = request.email,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                ),
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/password-reset/confirm") {
            val request = call.receive<ConfirmPasswordResetRequest>()
            val result = credentialAdministrationModule.confirmPasswordResetUseCase.execute(
                ConfirmPasswordResetCommand(
                    resetToken = request.resetToken,
                    newPassword = request.newPassword,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                ),
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = false,
        ) {
            post("/change-password") {
                val context = call.hermesAuthContext()
                val request = call.receive<ChangePasswordRequest>()

                val result = credentialAdministrationModule.changePasswordUseCase.execute(
                    ChangePasswordCommand(
                        userId = context.userId,
                        currentPassword = request.currentPassword,
                        newPassword = request.newPassword,
                        sessionId = context.sessionId,
                        revokeOtherSessions = request.revokeOtherSessions,
                        ipAddress = call.clientIpAddress(),
                        userAgent = call.request.header(HttpHeaders.UserAgent),
                    ),
                )

                call.respond(HttpStatusCode.OK, result.toCredentialResponse())
            }
        }
    }

    route("/organizations/{organizationId}/users") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_INVITE) {
                post("/invite") {
                    val organizationId = call.requiredOrganizationId()
                    val context = call.requireActiveOrganization(organizationId)
                    val request = call.receive<InviteUserRequest>()

                    val result = credentialAdministrationModule.inviteUserUseCase.execute(
                        InviteUserCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            email = request.email,
                            displayName = request.displayName,
                            roleIds = request.roleIds,
                            ipAddress = call.clientIpAddress(),
                            userAgent = call.request.header(HttpHeaders.UserAgent),
                        ),
                    )

                    call.respond(HttpStatusCode.Created, result.toCredentialResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_CREATE) {
                post {
                    val organizationId = call.requiredOrganizationId()
                    val context = call.requireActiveOrganization(organizationId)
                    val request = call.receive<CreateTemporaryUserRequest>()

                    val result = credentialAdministrationModule.createTemporaryUserUseCase.execute(
                        CreateTemporaryUserCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            email = request.email,
                            displayName = request.displayName,
                            roleIds = request.roleIds,
                            temporaryPassword = request.temporaryPassword,
                            phone = request.phone,
                            ipAddress = call.clientIpAddress(),
                            userAgent = call.request.header(HttpHeaders.UserAgent),
                        ),
                    )

                    call.respond(HttpStatusCode.Created, result.toCredentialResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_BLOCK) {
                post("/{userId}/block") {
                    val organizationId = call.requiredOrganizationId()
                    val context = call.requireActiveOrganization(organizationId)
                    val userId = call.requiredUserId()
                    val request = call.receive<BlockUserRequest>()

                    val result = credentialAdministrationModule.blockUserUseCase.execute(
                        BlockUserCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            targetUserId = userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            reason = request.reason,
                            ipAddress = call.clientIpAddress(),
                            userAgent = call.request.header(HttpHeaders.UserAgent),
                        ),
                    )

                    call.respond(HttpStatusCode.OK, result.toCredentialResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_USERS_UNBLOCK) {
                post("/{userId}/unblock") {
                    val organizationId = call.requiredOrganizationId()
                    val context = call.requireActiveOrganization(organizationId)
                    val userId = call.requiredUserId()
                    val request = call.receive<UnblockUserRequest>()

                    val result = credentialAdministrationModule.unblockUserUseCase.execute(
                        UnblockUserCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            targetUserId = userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            reason = request.reason,
                            ipAddress = call.clientIpAddress(),
                            userAgent = call.request.header(HttpHeaders.UserAgent),
                        ),
                    )

                    call.respond(HttpStatusCode.OK, result.toCredentialResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CREDENTIALS_SESSIONS_REVOKE) {
                post("/{userId}/sessions/revoke-all") {
                    val organizationId = call.requiredOrganizationId()
                    val context = call.requireActiveOrganization(organizationId)
                    val userId = call.requiredUserId()
                    val request = call.receive<RevokeUserSessionsByAdminRequest>()

                    val result = revokeSessionUseCase.revokeAllUserSessions(
                        RevokeAllUserSessionsCommand(
                            targetUserId = userId,
                            actorUserId = context.userId,
                            reason = request.reason,
                            organizationId = organizationId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )

                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.requiredOrganizationId(): String =
    parameters["organizationId"]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("Organization id path parameter is required.")

private fun ApplicationCall.requiredUserId(): String =
    parameters["userId"]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("User id path parameter is required.")

private fun ApplicationCall.requireActiveOrganization(expectedOrganizationId: String) =
    hermesAuthContext().also { context ->
        val activeOrganizationId = context.organizationId
            ?: throw DomainRuleViolation("Active organization is required.")

        if (activeOrganizationId != expectedOrganizationId) {
            throw DomainRuleViolation("Path organization does not match active organization.")
        }
    }

private fun ApplicationCall.clientIpAddress(): String? =
    request.header("X-Forwarded-For")
        ?.split(',')
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }