package com.hermes.backend.routes

import com.hermes.application.admin.business.GetAdminActivityCommand
import com.hermes.application.admin.business.GetAdminBranchCommand
import com.hermes.application.admin.business.GetAdminBusinessCommand
import com.hermes.application.admin.business.GetAdminBusinessFoundationOverviewCommand
import com.hermes.application.admin.business.GetAdminBusinessReadinessCommand
import com.hermes.application.admin.business.GetAdminEmissionPointCommand
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
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

            hermesRequiresAnyPermission(setOf(PermissionCatalog.ORGANIZATION_UPDATE)) {
                put("/business") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val request = call.receive<UpdateAdminBusinessRequest>()
                    val useCase = adminBusinessModule.updateBusinessUseCase
                        ?: throw DomainRuleViolation("Admin business update module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
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


                get("/business/overview") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val useCase = adminBusinessModule.getFoundationOverviewUseCase
                        ?: throw DomainRuleViolation("Admin business foundation overview module is not configured.")
                    val result = useCase.execute(
                        GetAdminBusinessFoundationOverviewCommand(
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

                get("/activities/{activityId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val activityId = call.parameters["activityId"] ?: throw DomainRuleViolation("Activity id is required.")
                    val useCase = adminBusinessModule.getActivityUseCase
                        ?: throw DomainRuleViolation("Admin activity detail module is not configured.")
                    val result = useCase.execute(
                        GetAdminActivityCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            activityId = activityId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.ACTIVITIES_CREATE)) {
                post("/activities") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val request = call.receive<CreateAdminActivityRequest>()
                    val useCase = adminBusinessModule.createActivityUseCase
                        ?: throw DomainRuleViolation("Admin activity creation module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.ACTIVITIES_UPDATE)) {
                put("/activities/{activityId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val activityId = call.parameters["activityId"] ?: throw DomainRuleViolation("Activity id is required.")
                    val request = call.receive<UpdateAdminActivityRequest>()
                    val useCase = adminBusinessModule.updateActivityUseCase
                        ?: throw DomainRuleViolation("Admin activity update module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            activityId = activityId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/activities/{activityId}/activate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val activityId = call.parameters["activityId"] ?: throw DomainRuleViolation("Activity id is required.")
                    val request = call.receive<ChangeAdminActivityStatusRequest>()
                    val useCase = adminBusinessModule.changeActivityStatusUseCase
                        ?: throw DomainRuleViolation("Admin activity status module is not configured.")
                    val result = useCase.activate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            activityId = activityId,
                            targetStatus = "active",
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/activities/{activityId}/deactivate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val activityId = call.parameters["activityId"] ?: throw DomainRuleViolation("Activity id is required.")
                    val request = call.receive<ChangeAdminActivityStatusRequest>()
                    val useCase = adminBusinessModule.changeActivityStatusUseCase
                        ?: throw DomainRuleViolation("Admin activity status module is not configured.")
                    val result = useCase.deactivate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            activityId = activityId,
                            targetStatus = "paused",
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

                get("/branches/{branchId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val branchId = call.parameters["branchId"] ?: throw DomainRuleViolation("Branch id is required.")
                    val useCase = adminBusinessModule.getBranchUseCase
                        ?: throw DomainRuleViolation("Admin branch detail module is not configured.")
                    val result = useCase.execute(
                        GetAdminBranchCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = branchId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.BRANCHES_CREATE, PermissionCatalog.SETTINGS_BRANCHES_MANAGE)) {
                post("/branches") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val request = call.receive<CreateAdminBranchRequest>()
                    val useCase = adminBusinessModule.createBranchUseCase
                        ?: throw DomainRuleViolation("Admin branch creation module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.BRANCHES_UPDATE, PermissionCatalog.SETTINGS_BRANCHES_MANAGE)) {
                put("/branches/{branchId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val branchId = call.parameters["branchId"] ?: throw DomainRuleViolation("Branch id is required.")
                    val request = call.receive<UpdateAdminBranchRequest>()
                    val useCase = adminBusinessModule.updateBranchUseCase
                        ?: throw DomainRuleViolation("Admin branch update module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = branchId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/branches/{branchId}/activate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val branchId = call.parameters["branchId"] ?: throw DomainRuleViolation("Branch id is required.")
                    val request = call.receive<ChangeAdminBranchStatusRequest>()
                    val useCase = adminBusinessModule.changeBranchStatusUseCase
                        ?: throw DomainRuleViolation("Admin branch status module is not configured.")
                    val result = useCase.activate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = branchId,
                            targetStatus = "active",
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/branches/{branchId}/deactivate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val branchId = call.parameters["branchId"] ?: throw DomainRuleViolation("Branch id is required.")
                    val request = call.receive<ChangeAdminBranchStatusRequest>()
                    val useCase = adminBusinessModule.changeBranchStatusUseCase
                        ?: throw DomainRuleViolation("Admin branch status module is not configured.")
                    val result = useCase.deactivate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = branchId,
                            targetStatus = "inactive",
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

                get("/emission-points/{emissionPointId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val emissionPointId = call.parameters["emissionPointId"]
                        ?: throw DomainRuleViolation("Emission point id is required.")
                    val useCase = adminBusinessModule.getEmissionPointUseCase
                        ?: throw DomainRuleViolation("Admin emission point detail module is not configured.")
                    val result = useCase.execute(
                        GetAdminEmissionPointCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            emissionPointId = emissionPointId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE)) {
                post("/emission-points") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val request = call.receive<CreateAdminEmissionPointRequest>()
                    val useCase = adminBusinessModule.createEmissionPointUseCase
                        ?: throw DomainRuleViolation("Admin emission point creation module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }

                put("/emission-points/{emissionPointId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val emissionPointId = call.parameters["emissionPointId"]
                        ?: throw DomainRuleViolation("Emission point id is required.")
                    val request = call.receive<UpdateAdminEmissionPointRequest>()
                    val useCase = adminBusinessModule.updateEmissionPointUseCase
                        ?: throw DomainRuleViolation("Admin emission point update module is not configured.")
                    val result = useCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            emissionPointId = emissionPointId,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/emission-points/{emissionPointId}/activate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val emissionPointId = call.parameters["emissionPointId"]
                        ?: throw DomainRuleViolation("Emission point id is required.")
                    val request = call.receive<ChangeAdminEmissionPointStatusRequest>()
                    val useCase = adminBusinessModule.changeEmissionPointStatusUseCase
                        ?: throw DomainRuleViolation("Admin emission point status module is not configured.")
                    val result = useCase.activate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            emissionPointId = emissionPointId,
                            targetStatus = "active",
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/emission-points/{emissionPointId}/deactivate") {
                    val context = call.hermesAuthContext()
                    val organizationId = context.requireActiveOrganization().organization.id
                    val emissionPointId = call.parameters["emissionPointId"]
                        ?: throw DomainRuleViolation("Emission point id is required.")
                    val request = call.receive<ChangeAdminEmissionPointStatusRequest>()
                    val useCase = adminBusinessModule.changeEmissionPointStatusUseCase
                        ?: throw DomainRuleViolation("Admin emission point status module is not configured.")
                    val result = useCase.deactivate(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            emissionPointId = emissionPointId,
                            targetStatus = "inactive",
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}
