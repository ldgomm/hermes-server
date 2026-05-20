package com.hermes.backend.routes

import com.hermes.application.admin.support.GetAdminAuditTimelineCommand
import com.hermes.application.admin.support.GetAdminSupportDiagnosticsCommand
import com.hermes.application.admin.support.GetAdminSupportModulesCommand
import com.hermes.application.admin.support.GetAdminSupportPermissionsCommand
import com.hermes.application.admin.support.SearchAdminAuditLogsCommand
import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.admin.support.AdminSupportModule
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant

fun Application.configureAdminSupportRoutes(
    authModule: AuthModule,
    adminSupportModule: AdminSupportModule,
) {
    routing { adminSupportRoutes(authModule = authModule, adminSupportModule = adminSupportModule) }
}

fun Route.adminSupportRoutes(
    authModule: AuthModule,
    adminSupportModule: AdminSupportModule,
) {
    adminSupportRoutes(
        authenticateRequestUseCase = authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
        adminSupportModule = adminSupportModule,
    )
}

fun Route.adminSupportRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    adminSupportModule: AdminSupportModule,
) {
    route("/api/v1/admin") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(setOf(PermissionCatalog.AUDIT_VIEW)) {
                get("/audit/logs") {
                    val context = call.hermesAuthContext()
                    val result = adminSupportModule.searchAuditLogsUseCase.execute(
                        SearchAdminAuditLogsCommand(
                            organizationId = call.adminSupportOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            sources = call.queryCsv("sources", "source"),
                            surfaces = call.queryCsv("surfaces", "surface"),
                            actions = call.queryCsv("actions", "action"),
                            severities = call.queryCsv("severities", "severity"),
                            auditedActorUserId = call.query("actorUserId") ?: call.query("auditedActorUserId"),
                            targetType = call.query("targetType"),
                            targetId = call.query("targetId"),
                            from = call.queryInstant("from"),
                            to = call.queryInstant("to"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/audit/timeline") {
                    val context = call.hermesAuthContext()
                    val result = adminSupportModule.auditTimelineUseCase.execute(
                        GetAdminAuditTimelineCommand(
                            organizationId = call.adminSupportOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            auditedActorUserId = call.query("actorUserId") ?: call.query("auditedActorUserId"),
                            targetType = call.query("targetType"),
                            targetId = call.query("targetId"),
                            from = call.queryInstant("from"),
                            to = call.queryInstant("to"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.ORGANIZATION_VIEW)) {
                get("/support/diagnostics") {
                    val context = call.hermesAuthContext()
                    val result = adminSupportModule.supportDiagnosticsUseCase.execute(
                        GetAdminSupportDiagnosticsCommand(
                            organizationId = call.adminSupportOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/support/modules") {
                    val context = call.hermesAuthContext()
                    val result = adminSupportModule.supportModulesUseCase.execute(
                        GetAdminSupportModulesCommand(
                            organizationId = call.adminSupportOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.CREDENTIALS_ROLES_VIEW)) {
                get("/support/permissions") {
                    val context = call.hermesAuthContext()
                    val result = adminSupportModule.supportPermissionsUseCase.execute(
                        GetAdminSupportPermissionsCommand(
                            organizationId = call.adminSupportOrganizationId(),
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

private fun ApplicationCall.adminSupportOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.query(name: String): String? =
    request.queryParameters[name]?.trim()?.takeIf { it.isNotBlank() }

private fun ApplicationCall.queryInt(name: String): Int? = query(name)?.toIntOrNull()

private fun ApplicationCall.queryInstant(name: String): Instant? = query(name)?.let { raw ->
    runCatching { Instant.parse(raw) }
        .getOrElse { throw DomainRuleViolation("Query parameter '$name' must be an ISO-8601 instant.") }
}

private fun ApplicationCall.queryCsv(vararg names: String): Set<String> = names
    .asSequence()
    .mapNotNull { query(it) }
    .flatMap { it.split(',').asSequence() }
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .toSet()
