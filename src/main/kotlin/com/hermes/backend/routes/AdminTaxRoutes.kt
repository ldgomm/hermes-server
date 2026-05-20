package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.tax.TaxGetProfileCommand
import com.hermes.application.tax.TaxGetRateCommand
import com.hermes.backend.admin.tax.AdminTaxModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAdminTaxRoutes(
    authModule: AuthModule,
    adminTaxModule: AdminTaxModule,
) {
    routing { adminTaxRoutes(authModule = authModule, adminTaxModule = adminTaxModule) }
}

fun Route.adminTaxRoutes(
    authModule: AuthModule,
    adminTaxModule: AdminTaxModule,
) {
    adminTaxRoutes(
        authenticateRequestUseCase = authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
        adminTaxModule = adminTaxModule,
    )
}

fun Route.adminTaxRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    adminTaxModule: AdminTaxModule,
) {
    route("/api/v1/admin/tax") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.TAX_SETTINGS_VIEW) {
                get("/rates") {
                    val context = call.hermesAuthContext()
                    val result = adminTaxModule.searchRatesUseCase.execute(
                        adminTaxRateSearchCommand(
                            organizationId = call.adminTaxOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                            kind = call.request.queryParameters["kind"],
                            statuses = call.request.queryParameters["statuses"]
                                ?: call.request.queryParameters["status"],
                            effectiveAt = call.request.queryParameters["effectiveAt"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }

                get("/rates/{rateId}") {
                    val context = call.hermesAuthContext()
                    val result = adminTaxModule.getRateUseCase.execute(
                        TaxGetRateCommand(
                            taxRateId = call.requiredAdminTaxPath("rateId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }

                get("/profiles") {
                    val context = call.hermesAuthContext()
                    val result = adminTaxModule.searchProfilesUseCase.execute(
                        adminTaxProfileSearchCommand(
                            organizationId = call.adminTaxOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                            treatment = call.request.queryParameters["treatment"],
                            statuses = call.request.queryParameters["statuses"]
                                ?: call.request.queryParameters["status"],
                            effectiveAt = call.request.queryParameters["effectiveAt"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }

                get("/profiles/{profileId}") {
                    val context = call.hermesAuthContext()
                    val result = adminTaxModule.getProfileUseCase.execute(
                        TaxGetProfileCommand(
                            taxProfileId = call.requiredAdminTaxPath("profileId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }

                get("/readiness") {
                    val context = call.hermesAuthContext()
                    val result = adminTaxModule.readinessUseCase.execute(
                        com.hermes.application.admin.tax.GetAdminTaxReadinessCommand(
                            organizationId = call.adminTaxOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.TAX_MANAGE) {
                post("/rates") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CreateAdminTaxRateRequest>()
                    val result = adminTaxModule.createRateUseCase.execute(
                        request.toCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.Created, result.toAdminTaxResponse())
                }

                put("/rates/{rateId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<UpdateAdminTaxRateRequest>()
                    val result = adminTaxModule.updateRateUseCase.execute(
                        request.toCommand(
                            rateId = call.requiredAdminTaxPath("rateId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }

                post("/profiles") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CreateAdminTaxProfileRequest>()
                    val result = adminTaxModule.createProfileUseCase.execute(
                        request.toCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.Created, result.toAdminTaxResponse())
                }

                put("/profiles/{profileId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<UpdateAdminTaxProfileRequest>()
                    val result = adminTaxModule.updateProfileUseCase.execute(
                        request.toCommand(
                            profileId = call.requiredAdminTaxPath("profileId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse())
                }
            }
        }
    }

    route("/api/v1/admin/catalog/local/items") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(
                setOf(
                    PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE,
                ),
            ) {
                post("/{itemId}/tax-profile") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<AssignAdminCatalogItemTaxProfileRequest>()
                    val result = adminTaxModule.assignTaxProfileToCatalogItemUseCase.execute(
                        request.toCommand(
                            organizationId = call.adminTaxOrganizationId(),
                            itemId = call.requiredAdminTaxPath("itemId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toAdminTaxResponse(request.taxProfileCode))
                }
            }
        }
    }
}

private fun ApplicationCall.adminTaxOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.requiredAdminTaxPath(name: String): String =
    parameters[name]
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")
