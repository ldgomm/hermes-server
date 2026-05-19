package com.hermes.backend.routes

import com.hermes.application.admin.business.*
import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.admin.business.AdminBusinessModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.shared.ErrorEnvelope
import com.hermes.backend.shared.ErrorResponse
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            get("/business") {
                if (!call.requireAnyAdminPermission(organizationViewPermissions)) return@get

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

            put("/business") {
                if (!call.requireAnyAdminPermission(organizationUpdatePermissions)) return@put

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

            get("/business/readiness") {
                if (!call.requireAnyAdminPermission(readinessViewPermissions)) return@get

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

            get("/activities") {
                if (!call.requireAnyAdminPermission(activityViewPermissions)) return@get

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
                if (!call.requireAnyAdminPermission(activityViewPermissions)) return@get

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

            post("/activities") {
                if (!call.requireAnyAdminPermission(activityCreatePermissions)) return@post

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

            put("/activities/{activityId}") {
                if (!call.requireAnyAdminPermission(activityUpdatePermissions)) return@put

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
                if (!call.requireAnyAdminPermission(activityUpdatePermissions)) return@post

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
                if (!call.requireAnyAdminPermission(activityUpdatePermissions)) return@post

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

            get("/branches") {
                if (!call.requireAnyAdminPermission(branchViewPermissions)) return@get

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
                if (!call.requireAnyAdminPermission(branchViewPermissions)) return@get

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

            post("/branches") {
                if (!call.requireAnyAdminPermission(branchCreatePermissions)) return@post

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

            put("/branches/{branchId}") {
                if (!call.requireAnyAdminPermission(branchUpdatePermissions)) return@put

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
                if (!call.requireAnyAdminPermission(branchUpdatePermissions)) return@post

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
                if (!call.requireAnyAdminPermission(branchUpdatePermissions)) return@post

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

            get("/emission-points") {
                if (!call.requireAnyAdminPermission(emissionPointViewPermissions)) return@get

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

private val organizationViewPermissions = setOf(
    PermissionCatalog.ORGANIZATION_VIEW,
    PermissionCatalog.ORGANIZATION_UPDATE,
)

private val organizationUpdatePermissions = setOf(
    PermissionCatalog.ORGANIZATION_UPDATE,
)

private val readinessViewPermissions = setOf(
    PermissionCatalog.ORGANIZATION_VIEW,
    PermissionCatalog.ORGANIZATION_UPDATE,
    PermissionCatalog.AUDIT_VIEW,
)

private val activityViewPermissions = setOf(
    PermissionCatalog.ACTIVITIES_VIEW,
)

private val activityCreatePermissions = setOf(
    PermissionCatalog.ACTIVITIES_CREATE,
)

private val activityUpdatePermissions = setOf(
    PermissionCatalog.ACTIVITIES_UPDATE,
)

private val branchViewPermissions = setOf(
    PermissionCatalog.BRANCHES_VIEW,
    PermissionCatalog.SETTINGS_BRANCHES_VIEW,
)

private val branchCreatePermissions = setOf(
    PermissionCatalog.BRANCHES_CREATE,
    PermissionCatalog.SETTINGS_BRANCHES_MANAGE,
)

private val branchUpdatePermissions = setOf(
    PermissionCatalog.BRANCHES_UPDATE,
    PermissionCatalog.SETTINGS_BRANCHES_MANAGE,
)

private val emissionPointViewPermissions = setOf(
    PermissionCatalog.SETTINGS_EMISSION_POINTS_VIEW,
)

private suspend fun ApplicationCall.requireAnyAdminPermission(requiredPermissions: Set<String>): Boolean {
    val effectivePermissions = hermesAuthContext().effectivePermissions?.permissions.orEmpty()
    if (PermissionCatalog.ALL in effectivePermissions || requiredPermissions.any { it in effectivePermissions }) {
        return true
    }

    respond(
        status = HttpStatusCode.Forbidden,
        message = ErrorEnvelope(
            error = ErrorResponse(
                code = "missing_any_permission",
                message = "Missing any required permission: ${requiredPermissions.sorted().joinToString()}.",
                requestId = request.header("X-Request-Id"),
                details = requiredPermissions.sorted().joinToString(separator = ","),
            ),
        ),
    )
    return false
}