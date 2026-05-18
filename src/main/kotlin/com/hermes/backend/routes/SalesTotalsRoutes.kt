package com.hermes.backend.routes

import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.sales.PreviewSaleTotalsRequest
import com.hermes.backend.sales.SalesTotalsModule
import com.hermes.backend.sales.toCommand
import com.hermes.backend.sales.toResponse
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureSalesTotalsRoutes(authModule: AuthModule, salesTotalsModule: SalesTotalsModule) {
    routing { salesTotalsRoutes(authModule, salesTotalsModule) }
}

fun Route.salesTotalsRoutes(authModule: AuthModule, salesTotalsModule: SalesTotalsModule) {
    route("/organizations/{organizationId}") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/sales/totals") {
                hermesRequiresPermission(PermissionCatalog.SALES_CREATE) {
                    post("/preview") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesTotalsOrganizationId()
                        call.assertSalesTotalsOrganizationMatchesContext(organizationId)
                        val request = call.receive<PreviewSaleTotalsRequest>()
                        val result = salesTotalsModule.previewSaleTotalsUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.requiredSalesTotalsOrganizationId(): String =
    parameters["organizationId"]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Organization id path parameter is required.")

private fun ApplicationCall.assertSalesTotalsOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}
