package com.hermes.backend.routes

import com.hermes.application.auth.*
import com.hermes.backend.auth.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAuthRoutes(
    registerOwnerUseCase: RegisterOwnerUseCase,
    registerOwnerWorkspaceUseCase: RegisterOwnerWorkspaceUseCase,
    loginUseCase: LoginUseCase,
    refreshSessionUseCase: RefreshSessionUseCase,
    revokeSessionUseCase: RevokeSessionUseCase,
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
) {
    routing {
        authRoutes(
            registerOwnerUseCase = registerOwnerUseCase,
            registerOwnerWorkspaceUseCase = registerOwnerWorkspaceUseCase,
            loginUseCase = loginUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
            revokeSessionUseCase = revokeSessionUseCase,
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
        )
    }
}

fun Route.authRoutes(
    registerOwnerUseCase: RegisterOwnerUseCase,
    registerOwnerWorkspaceUseCase: RegisterOwnerWorkspaceUseCase,
    loginUseCase: LoginUseCase,
    refreshSessionUseCase: RefreshSessionUseCase,
    revokeSessionUseCase: RevokeSessionUseCase,
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
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

        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = false,
        ) {
            post("/sessions/revoke") {
                val context = call.hermesAuthContext()
                val request = call.receive<RevokeSessionRequest>()

                val result = revokeSessionUseCase.revokeSession(
                    RevokeSessionCommand(
                        sessionId = request.sessionId?.trim()?.takeIf { it.isNotBlank() } ?: context.sessionId,
                        actorUserId = context.userId,
                        reason = request.reason,
                    ),
                )

                call.respond(HttpStatusCode.OK, result.toResponse())
            }

            post("/sessions/revoke-all") {
                val context = call.hermesAuthContext()
                val request = call.receive<RevokeAllUserSessionsRequest>()
                val targetUserId = request.targetUserId?.trim()?.takeIf { it.isNotBlank() } ?: context.userId

                if (targetUserId != context.userId) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf(
                            "error" to "admin_session_revocation_requires_organization_route",
                            "message" to "Use /organizations/{organizationId}/users/{userId}/sessions/revoke-all.",
                        ),
                    )
                    return@post
                }

                val result = revokeSessionUseCase.revokeAllUserSessions(
                    RevokeAllUserSessionsCommand(
                        targetUserId = context.userId,
                        actorUserId = context.userId,
                        reason = request.reason,
                        organizationId = null,
                        actorEffectivePermissions = emptySet(),
                    ),
                )

                call.respond(HttpStatusCode.OK, result.toResponse())
            }
        }
    }
}

private fun ApplicationCall.clientIpAddress(): String? =
    request.header("X-Forwarded-For")?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }