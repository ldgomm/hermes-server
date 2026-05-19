package com.hermes.backend.auth

import com.hermes.application.auth.AuthorizationPolicy
import com.hermes.backend.shared.ErrorEnvelope
import com.hermes.backend.shared.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.hermesRequiresPermission(
    permission: String,
    build: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Plugins) {
        val context = call.hermesAuthContextOrNull()
        if (context == null) {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "authentication_required",
                        message = "Authentication is required.",
                        requestId = call.request.header("X-Request-Id"),
                    ),
                ),
            )
            finish()
            return@intercept
        }

        val permissions = context.effectivePermissions?.permissions.orEmpty()
        if (!AuthorizationPolicy.canPerform(permissions, permission)) {
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "missing_permission",
                        message = "Missing required permission: $permission.",
                        requestId = call.request.header("X-Request-Id"),
                        details = permission,
                    ),
                ),
            )
            finish()
            return@intercept
        }
    }

    build()
}

fun Route.hermesRequiresAnyPermission(
    permissions: Set<String>,
    build: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Plugins) {
        val context = call.hermesAuthContextOrNull()
        if (context == null) {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "authentication_required",
                        message = "Authentication is required.",
                        requestId = call.request.header("X-Request-Id"),
                    ),
                ),
            )
            finish()
            return@intercept
        }

        val effectivePermissions = context.effectivePermissions?.permissions.orEmpty()
        if (!AuthorizationPolicy.canPerformAny(effectivePermissions, permissions)) {
            val required = permissions.sorted()

            call.respond(
                status = HttpStatusCode.Forbidden,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "missing_any_permission",
                        message = "Missing any required permission: ${required.joinToString()}.",
                        requestId = call.request.header("X-Request-Id"),
                        details = required.joinToString(separator = ","),
                    ),
                ),
            )
            finish()
            return@intercept
        }
    }

    build()
}