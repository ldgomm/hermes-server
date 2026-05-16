package com.hermes.backend.routes

import com.hermes.application.auth.LoginCommand
import com.hermes.application.auth.LoginUseCase
import com.hermes.application.auth.RefreshSessionCommand
import com.hermes.application.auth.RefreshSessionUseCase
import com.hermes.application.auth.RegisterOwnerCommand
import com.hermes.application.auth.RegisterOwnerUseCase
import com.hermes.application.auth.RegisterOwnerWorkspaceCommand
import com.hermes.application.auth.RegisterOwnerWorkspaceUseCase
import com.hermes.application.auth.RevokeAllUserSessionsCommand
import com.hermes.application.auth.RevokeSessionCommand
import com.hermes.application.auth.RevokeSessionUseCase
import com.hermes.backend.auth.LoginRequest
import com.hermes.backend.auth.RefreshTokenRequest
import com.hermes.backend.auth.RegisterOwnerRequest
import com.hermes.backend.auth.RegisterOwnerWorkspaceRequest
import com.hermes.backend.auth.RevokeAllUserSessionsRequest
import com.hermes.backend.auth.RevokeSessionRequest
import com.hermes.backend.auth.toResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureAuthRoutes(
    registerOwnerUseCase: RegisterOwnerUseCase,
    registerOwnerWorkspaceUseCase: RegisterOwnerWorkspaceUseCase,
    loginUseCase: LoginUseCase,
    refreshSessionUseCase: RefreshSessionUseCase,
    revokeSessionUseCase: RevokeSessionUseCase,
) {
    routing {
        authRoutes(
            registerOwnerUseCase = registerOwnerUseCase,
            registerOwnerWorkspaceUseCase = registerOwnerWorkspaceUseCase,
            loginUseCase = loginUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
            revokeSessionUseCase = revokeSessionUseCase,
        )
    }
}

fun io.ktor.server.routing.Route.authRoutes(
    registerOwnerUseCase: RegisterOwnerUseCase,
    registerOwnerWorkspaceUseCase: RegisterOwnerWorkspaceUseCase,
    loginUseCase: LoginUseCase,
    refreshSessionUseCase: RefreshSessionUseCase,
    revokeSessionUseCase: RevokeSessionUseCase,
) {
    route("/auth") {
        post("/owners") {
            val request = call.receive<RegisterOwnerRequest>()
            val result = registerOwnerUseCase.execute(
                RegisterOwnerCommand(
                    email = request.email,
                    displayName = request.displayName,
                    password = request.password,
                    phone = request.phone,
                ),
            )
            call.respond(HttpStatusCode.Created, result.toResponse())
        }

        post("/owner-workspaces") {
            val request = call.receive<RegisterOwnerWorkspaceRequest>()
            val result = registerOwnerWorkspaceUseCase.execute(
                RegisterOwnerWorkspaceCommand(
                    ownerEmail = request.ownerEmail,
                    ownerDisplayName = request.ownerDisplayName,
                    ownerPassword = request.ownerPassword,
                    ownerPhone = request.ownerPhone,
                    organizationLegalName = request.organizationLegalName,
                    organizationCommercialName = request.organizationCommercialName,
                    organizationTaxId = request.organizationTaxId,
                    organizationCountryCode = request.organizationCountryCode,
                ),
            )
            call.respond(HttpStatusCode.Created, result.toResponse())
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = loginUseCase.execute(
                LoginCommand(
                    email = request.email,
                    password = request.password,
                    userAgent = call.request.header(HttpHeaders.UserAgent),
                    ipAddress = call.clientIpAddress(),
                ),
            )
            call.respond(HttpStatusCode.OK, result.toResponse())
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val result = refreshSessionUseCase.execute(
                RefreshSessionCommand(
                    refreshToken = request.refreshToken,
                ),
            )
            call.respond(HttpStatusCode.OK, result.toResponse())
        }

        post("/sessions/revoke") {
            val request = call.receive<RevokeSessionRequest>()
            val result = revokeSessionUseCase.revokeSession(
                RevokeSessionCommand(
                    sessionId = request.sessionId,
                    actorUserId = request.actorUserId,
                    reason = request.reason,
                ),
            )
            call.respond(HttpStatusCode.OK, result.toResponse())
        }

        post("/sessions/revoke-all") {
            val request = call.receive<RevokeAllUserSessionsRequest>()
            val result = revokeSessionUseCase.revokeAllUserSessions(
                RevokeAllUserSessionsCommand(
                    targetUserId = request.targetUserId,
                    actorUserId = request.actorUserId,
                    reason = request.reason,
                    organizationId = request.organizationId,
                    actorEffectivePermissions = request.actorEffectivePermissions,
                ),
            )
            call.respond(HttpStatusCode.OK, result.toResponse())
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.clientIpAddress(): String? =
    request.header("X-Forwarded-For")
        ?.split(',')
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }
