package com.hermes.backend.routes

import com.hermes.application.admin.business.GetAdminBusinessCommand
import com.hermes.application.admin.business.GetAdminBusinessReadinessCommand
import com.hermes.application.admin.business.ListAdminActivitiesCommand
import com.hermes.application.admin.business.ListAdminBranchesCommand
import com.hermes.application.admin.business.ListAdminEmissionPointsCommand
import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.admin.business.AdminBusinessModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.domain.permission.PermissionCatalog
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureAdminBusinessRoutes(
    authModule: AuthModule,
    adminBusinessModule: AdminBusinessModule,
) {
    routing { adminBusinessRoutes(authModule = authModule, adminBusinessModule = adminBusinessModule) }
}

fun Route.adminBusinessRoutes(
    authModule: AuthModule,
    adminBusinessModule: AdminBusinessModule,
) {
    adminBusinessRoutes(
        authenticateRequestUseCase = authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
        adminBusinessModule = adminBusinessModule,
    )
}

fun Route.adminBusinessRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    adminBusinessModule: AdminBusinessModule,
) {
    route("/api/v1/admin") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(setOf(PermissionCatalog.ORGANIZATION_VIEW, PermissionCatalog.ORGANIZATION_UPDATE)) {
                get("/business") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val result = adminBusinessModule.getBusinessUseCase.execute(
                        GetAdminBusinessCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(
                setOf(PermissionCatalog.ORGANIZATION_VIEW, PermissionCatalog.ORGANIZATION_UPDATE, PermissionCatalog.AUDIT_VIEW)
            ) {
                get("/business/readiness") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val result = adminBusinessModule.getReadinessUseCase.execute(
                        GetAdminBusinessReadinessCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.ACTIVITIES_VIEW)) {
                get("/activities") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val result = adminBusinessModule.listActivitiesUseCase.execute(
                        ListAdminActivitiesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.BRANCHES_VIEW, PermissionCatalog.SETTINGS_BRANCHES_VIEW)) {
                get("/branches") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val result = adminBusinessModule.listBranchesUseCase.execute(
                        ListAdminBranchesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW)) {
                get("/emission-points") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val result = adminBusinessModule.listEmissionPointsUseCase.execute(
                        ListAdminEmissionPointsCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}
