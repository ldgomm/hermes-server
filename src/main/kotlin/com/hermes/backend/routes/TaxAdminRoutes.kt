package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.tax.TaxGetProfileCommand
import com.hermes.application.tax.TaxGetRateCommand
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

fun Application.configureTaxAdminRoutes(
    authModule: AuthModule,
    taxModule: TaxModule,
) {
    routing {
        taxAdminRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            taxModule = taxModule,
        )
    }
}

fun Route.taxAdminRoutes(
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
                get("/tax-rates/{taxRateId}") {
                    val context = call.hermesAuthContext()
                    val result = taxModule.getRateUseCase.execute(
                        TaxGetRateCommand(
                            taxRateId = call.requiredPathParameter("taxRateId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.rate.toResponse())
                }

                get("/tax-profiles/{taxProfileId}") {
                    val context = call.hermesAuthContext()
                    val result = taxModule.getProfileUseCase.execute(
                        TaxGetProfileCommand(
                            taxProfileId = call.requiredPathParameter("taxProfileId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.profile.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.TAX_MANAGE) {
                post("/tax-rates") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<TaxCreateRateRequest>()

                    val result = taxModule.createRateUseCase.execute(
                        request.toCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.Created, result.rate.toResponse())
                }

                patch("/tax-rates/{taxRateId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<TaxUpdateRateRequest>()

                    val result = taxModule.updateRateUseCase.execute(
                        request.toCommand(
                            taxRateId = call.requiredPathParameter("taxRateId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.rate.toResponse())
                }

                post("/tax-profiles") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<TaxCreateProfileRequest>()

                    val result = taxModule.createProfileUseCase.execute(
                        request.toCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.Created, result.profile.toResponse())
                }

                patch("/tax-profiles/{taxProfileId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<TaxUpdateProfileRequest>()

                    val result = taxModule.updateProfileUseCase.execute(
                        request.toCommand(
                            taxProfileId = call.requiredPathParameter("taxProfileId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.profile.toResponse())
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
            hermesRequiresPermission(PermissionCatalog.TAX_SETTINGS_UPDATE_ORGANIZATION_REGIME) {
                patch("/tax-settings") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredPathParameter("organizationId")
                    call.assertRouteOrganizationMatchesContext(organizationId)

                    val request = call.receive<TaxUpdateOrganizationSettingsRequest>()
                    val result = taxModule.updateOrganizationSettingsUseCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = TaxSettingsResponse(result.settings.toResponse()),
                    )
                }
            }
        }
    }
}

private fun ApplicationCall.requiredPathParameter(name: String): String =
    parameters[name]
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$name path parameter is required.")

private fun ApplicationCall.assertRouteOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext()
        .requireActiveOrganization()
        .organization
        .id

    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}