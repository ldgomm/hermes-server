package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.tax.TaxQueryCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.tax.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureTaxRoutes(
    authModule: AuthModule,
    taxModule: TaxModule,
) {
    routing {
        taxRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            taxModule = taxModule,
        )
    }
}

fun Route.taxRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    taxModule: TaxModule,
) {
    route("/admin") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.TAX_SETTINGS_VIEW) {
                get("/tax-rates") {
                    val command = call.taxQueryCommandFromContext()
                    val result = taxModule.listActiveRatesUseCase.execute(command)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = TaxRatesResponse(result.map { it.toResponse() }),
                    )
                }

                get("/tax-profiles") {
                    val command = call.taxQueryCommandFromContext()
                    val result = taxModule.listActiveProfilesUseCase.execute(command)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = TaxProfilesResponse(result.map { it.toResponse() }),
                    )
                }
            }
        }
    }

    route("/organizations/{organizationId}") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.TAX_SETTINGS_VIEW) {
                get("/tax-settings") {
                    val command = call.taxQueryCommandFromPathAndContext()
                    val result = taxModule.getOrganizationSettingsUseCase.execute(command)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = TaxSettingsResponse(result.toResponse()),
                    )
                }

                route("/tax") {
                    get("/settings") {
                        val command = call.taxQueryCommandFromPathAndContext()
                        val result = taxModule.getOrganizationSettingsUseCase.execute(command)

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = TaxSettingsResponse(result.toResponse()),
                        )
                    }

                    get("/rates") {
                        val command = call.taxQueryCommandFromPathAndContext()
                        val result = taxModule.listActiveRatesUseCase.execute(command)

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = TaxRatesResponse(result.map { it.toResponse() }),
                        )
                    }

                    get("/profiles") {
                        val command = call.taxQueryCommandFromPathAndContext()
                        val result = taxModule.listActiveProfilesUseCase.execute(command)

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = TaxProfilesResponse(result.map { it.toResponse() }),
                        )
                    }

                    post("/calculate-preview") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredOrganizationIdFromPath()
                        call.assertRouteOrganizationMatchesContext(organizationId)

                        val request = call.receive<TaxCalculatePreviewRequest>()
                        val result = taxModule.calculatePreviewUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            ),
                        )

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = result.toResponse(),
                        )
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.taxQueryCommandFromContext(): TaxQueryCommand {
    val context = hermesAuthContext()
    val organizationId = context.requireActiveOrganization().organization.id

    return TaxQueryCommand(
        organizationId = organizationId,
        actorUserId = context.userId,
        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
    )
}

private fun ApplicationCall.taxQueryCommandFromPathAndContext(): TaxQueryCommand {
    val context = hermesAuthContext()
    val organizationId = requiredOrganizationIdFromPath()
    assertRouteOrganizationMatchesContext(organizationId)

    return TaxQueryCommand(
        organizationId = organizationId,
        actorUserId = context.userId,
        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
    )
}

private fun ApplicationCall.requiredOrganizationIdFromPath(): String =
    parameters["organizationId"]
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Organization id path parameter is required.")

private fun ApplicationCall.assertRouteOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext()
        .requireActiveOrganization()
        .organization
        .id

    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}