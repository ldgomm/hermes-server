package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.backend.auth.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureCredentialAdminRoutes(
    inviteUserUseCase: InviteUserUseCase,
    acceptInvitationUseCase: AcceptInvitationUseCase,
    createTemporaryUserUseCase: CreateTemporaryUserUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    requestPasswordResetUseCase: RequestPasswordResetUseCase,
    confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    blockUserUseCase: BlockUserUseCase,
    unblockUserUseCase: UnblockUserUseCase,
) {
    routing {
        credentialAdminRoutes(
            inviteUserUseCase = inviteUserUseCase,
            acceptInvitationUseCase = acceptInvitationUseCase,
            createTemporaryUserUseCase = createTemporaryUserUseCase,
            changePasswordUseCase = changePasswordUseCase,
            requestPasswordResetUseCase = requestPasswordResetUseCase,
            confirmPasswordResetUseCase = confirmPasswordResetUseCase,
            blockUserUseCase = blockUserUseCase,
            unblockUserUseCase = unblockUserUseCase,
        )
    }
}

fun Route.credentialAdminRoutes(
    inviteUserUseCase: InviteUserUseCase,
    acceptInvitationUseCase: AcceptInvitationUseCase,
    createTemporaryUserUseCase: CreateTemporaryUserUseCase,
    changePasswordUseCase: ChangePasswordUseCase,
    requestPasswordResetUseCase: RequestPasswordResetUseCase,
    confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    blockUserUseCase: BlockUserUseCase,
    unblockUserUseCase: UnblockUserUseCase,
) {
    route("/auth") {
        post("/invitations/accept") {
            val request = call.receive<AcceptInvitationRequest>()
            val result = acceptInvitationUseCase.execute(
                AcceptInvitationCommand(
                    invitationToken = request.invitationToken,
                    password = request.password,
                    displayName = request.displayName,
                    phone = request.phone,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/change-password") {
            val request = call.receive<ChangePasswordRequest>()
            val result = changePasswordUseCase.execute(
                ChangePasswordCommand(
                    userId = request.userId,
                    currentPassword = request.currentPassword,
                    newPassword = request.newPassword,
                    sessionId = request.sessionId,
                    revokeOtherSessions = request.revokeOtherSessions,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/password-reset/request") {
            val request = call.receive<RequestPasswordResetRequest>()
            val result = requestPasswordResetUseCase.execute(
                RequestPasswordResetCommand(
                    email = request.email,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/password-reset/confirm") {
            val request = call.receive<ConfirmPasswordResetRequest>()
            val result = confirmPasswordResetUseCase.execute(
                ConfirmPasswordResetCommand(
                    resetToken = request.resetToken,
                    newPassword = request.newPassword,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }
    }

    route("/organizations/{organizationId}/users") {
        post("/invite") {
            val organizationId = call.parameters["organizationId"] ?: error("organizationId is required")
            val request = call.receive<InviteUserRequest>()
            val result = inviteUserUseCase.execute(
                InviteUserCommand(
                    organizationId = organizationId,
                    actorUserId = request.actorUserId,
                    actorEffectivePermissions = request.actorEffectivePermissions,
                    email = request.email,
                    displayName = request.displayName,
                    roleIds = request.roleIds,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.Created, result.toCredentialResponse())
        }

        post {
            val organizationId = call.parameters["organizationId"] ?: error("organizationId is required")
            val request = call.receive<CreateTemporaryUserRequest>()
            val result = createTemporaryUserUseCase.execute(
                CreateTemporaryUserCommand(
                    organizationId = organizationId,
                    actorUserId = request.actorUserId,
                    actorEffectivePermissions = request.actorEffectivePermissions,
                    email = request.email,
                    displayName = request.displayName,
                    roleIds = request.roleIds,
                    temporaryPassword = request.temporaryPassword,
                    phone = request.phone,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.Created, result.toCredentialResponse())
        }

        post("/{userId}/block") {
            val organizationId = call.parameters["organizationId"] ?: error("organizationId is required")
            val userId = call.parameters["userId"] ?: error("userId is required")
            val request = call.receive<BlockUserRequest>()
            val result = blockUserUseCase.execute(
                BlockUserCommand(
                    organizationId = organizationId,
                    actorUserId = request.actorUserId,
                    targetUserId = userId,
                    actorEffectivePermissions = request.actorEffectivePermissions,
                    reason = request.reason,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }

        post("/{userId}/unblock") {
            val organizationId = call.parameters["organizationId"] ?: error("organizationId is required")
            val userId = call.parameters["userId"] ?: error("userId is required")
            val request = call.receive<UnblockUserRequest>()
            val result = unblockUserUseCase.execute(
                UnblockUserCommand(
                    organizationId = organizationId,
                    actorUserId = request.actorUserId,
                    targetUserId = userId,
                    actorEffectivePermissions = request.actorEffectivePermissions,
                    reason = request.reason,
                    ipAddress = call.clientIpAddress(),
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                )
            )
            call.respond(HttpStatusCode.OK, result.toCredentialResponse())
        }
    }
}

private fun ApplicationCall.clientIpAddress(): String? =
    request.header("X-Forwarded-For")
        ?.split(',')
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }
