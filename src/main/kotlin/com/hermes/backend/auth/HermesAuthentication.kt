package com.hermes.backend.auth

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.AuthenticatedRequestContext
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.auth.ResolveActiveOrganizationCommand
import com.hermes.application.auth.ResolveEffectivePermissionsCommand
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept

fun Route.hermesAuthenticated(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    requireOrganization: Boolean = false,
    build: Route.() -> Unit,
) {
    intercept(ApplicationCallPipeline.Plugins) {
        val token = call.bearerTokenOrNull()
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "missing_bearer_token"))
            finish()
            return@intercept
        }

        val context = runCatching {
            val principal = authenticateRequestUseCase.execute(token)
            val activeOrganization = activeOrganizationResolverUseCase.execute(
                ResolveActiveOrganizationCommand(
                    userId = principal.user.id,
                    requestedOrganizationId = call.requestedOrganizationId(),
                    required = requireOrganization,
                ),
            )
            val effectivePermissions = activeOrganization?.let { organizationContext ->
                effectivePermissionResolverUseCase.execute(
                    ResolveEffectivePermissionsCommand(
                        userId = principal.user.id,
                        organizationId = organizationContext.organization.id,
                    ),
                )
            }

            AuthenticatedRequestContext(
                principal = principal,
                activeOrganization = activeOrganization,
                effectivePermissions = effectivePermissions,
            )
        }.getOrElse { error ->
            val status = if (error is DomainRuleViolation) HttpStatusCode.Unauthorized else HttpStatusCode.InternalServerError
            call.respond(status, mapOf("error" to (error.message ?: "authentication_failed")))
            finish()
            return@intercept
        }

        call.attributes.put(HermesAuthAttributes.Context, context)
    }

    build()
}

fun ApplicationCall.bearerTokenOrNull(): String? {
    val authorization = request.header(HttpHeaders.Authorization)
        ?: request.header(HermesHeaders.AUTHORIZATION)
        ?: return null

    val parts = authorization.trim().split(Regex("\\s+"), limit = 2)
    if (parts.size != 2) return null
    if (!parts[0].equals("Bearer", ignoreCase = true)) return null

    return parts[1].trim().takeIf { it.isNotBlank() }
}

fun ApplicationCall.requestedOrganizationId(): String? =
    request.header(HermesHeaders.ORGANIZATION_ID)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: request.queryParameters["organizationId"]?.trim()?.takeIf { it.isNotBlank() }
