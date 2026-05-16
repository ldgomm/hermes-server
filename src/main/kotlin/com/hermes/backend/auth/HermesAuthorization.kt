package com.hermes.backend.auth

import com.hermes.application.auth.AuthorizationPolicy
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept

fun Route.hermesRequiresPermission(
    permission: String,
    build: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Plugins) {
        val context = call.hermesAuthContextOrNull()
        if (context == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "authentication_required"))
            finish()
            return@intercept
        }

        val permissions = context.effectivePermissions?.permissions.orEmpty()
        if (!AuthorizationPolicy.canPerform(permissions, permission)) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "missing_permission", "permission" to permission))
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
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "authentication_required"))
            finish()
            return@intercept
        }

        val effectivePermissions = context.effectivePermissions?.permissions.orEmpty()
        if (!AuthorizationPolicy.canPerformAny(effectivePermissions, permissions)) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "missing_any_permission"))
            finish()
            return@intercept
        }
    }

    build()
}
