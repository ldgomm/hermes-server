package com.hermes.backend.routes

import com.hermes.application.admin.operations.GetAdminCashSessionCommand
import com.hermes.application.admin.operations.GetAdminCashSummaryReportCommand
import com.hermes.application.admin.operations.GetAdminOperationalTodayReportCommand
import com.hermes.application.admin.operations.GetAdminSaleCommand
import com.hermes.application.admin.operations.GetAdminSalesSummaryReportCommand
import com.hermes.application.admin.operations.GetAdminTaxSummaryReportCommand
import com.hermes.application.admin.operations.GetCurrentAdminCashSessionCommand
import com.hermes.application.admin.operations.SearchAdminCashSessionsCommand
import com.hermes.application.admin.operations.SearchAdminPaymentsCommand
import com.hermes.application.admin.operations.SearchAdminReceivablesCommand
import com.hermes.application.admin.operations.SearchAdminSalesCommand
import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.admin.operations.AdminOperationsModule
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
import java.time.LocalDate
import java.time.ZoneId

fun Application.configureAdminOperationsRoutes(
    authModule: AuthModule,
    adminOperationsModule: AdminOperationsModule,
) {
    routing { adminOperationsRoutes(authModule = authModule, adminOperationsModule = adminOperationsModule) }
}

fun Route.adminOperationsRoutes(
    authModule: AuthModule,
    adminOperationsModule: AdminOperationsModule,
) {
    adminOperationsRoutes(
        authenticateRequestUseCase = authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
        adminOperationsModule = adminOperationsModule,
    )
}

fun Route.adminOperationsRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    adminOperationsModule: AdminOperationsModule,
) {
    route("/api/v1/admin") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW)) {
                get("/sales") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.searchSalesUseCase.execute(
                        SearchAdminSalesCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            activityId = call.query("activityId"),
                            customerId = call.query("customerId"),
                            operationalStatuses = call.queryCsv("operationalStatuses", "operationalStatus", "status"),
                            paymentStatuses = call.queryCsv("paymentStatuses", "paymentStatus"),
                            saleTypes = call.queryCsv("saleTypes", "saleType"),
                            from = call.queryInstant("from"),
                            to = call.queryInstant("to"),
                            query = call.query("q") ?: call.query("query"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/sales/{saleId}") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.getSaleUseCase.execute(
                        GetAdminSaleCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            saleId = call.requiredAdminOperationsPath("saleId"),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(
                setOf(PermissionCatalog.CASH_VIEW, PermissionCatalog.CASH_SESSION_VIEW_CURRENT, PermissionCatalog.REPORTS_CASH_VIEW),
            ) {
                get("/cash-sessions/current") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.getCurrentCashSessionUseCase.execute(
                        GetCurrentAdminCashSessionCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(
                setOf(PermissionCatalog.CASH_VIEW, PermissionCatalog.CASH_SESSION_VIEW_HISTORY, PermissionCatalog.REPORTS_CASH_VIEW),
            ) {
                get("/cash-sessions") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.searchCashSessionsUseCase.execute(
                        SearchAdminCashSessionsCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            statuses = call.queryCsv("statuses", "status"),
                            from = call.queryInstant("from"),
                            to = call.queryInstant("to"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/cash-sessions/{cashSessionId}") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.getCashSessionUseCase.execute(
                        GetAdminCashSessionCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            cashSessionId = call.requiredAdminOperationsPath("cashSessionId"),
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.PAYMENTS_VIEW, PermissionCatalog.REPORTS_CASH_VIEW)) {
                get("/payments") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.searchPaymentsUseCase.execute(
                        SearchAdminPaymentsCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            saleId = call.query("saleId"),
                            customerId = call.query("customerId"),
                            cashSessionId = call.query("cashSessionId"),
                            methods = call.queryCsv("methods", "method"),
                            statuses = call.queryCsv("statuses", "status"),
                            from = call.queryInstant("from"),
                            to = call.queryInstant("to"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.RECEIVABLES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW)) {
                get("/receivables") {
                    val context = call.hermesAuthContext()
                    val result = adminOperationsModule.searchReceivablesUseCase.execute(
                        SearchAdminReceivablesCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            customerId = call.query("customerId"),
                            statuses = call.queryCsv("statuses", "status"),
                            dueFrom = call.queryInstant("dueFrom"),
                            dueTo = call.queryInstant("dueTo"),
                            limit = call.queryInt("limit") ?: 100,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.REPORTS_DASHBOARD_VIEW, PermissionCatalog.REPORTS_SALES_VIEW)) {
                get("/reports/operational-today") {
                    val context = call.hermesAuthContext()
                    val range = call.adminOperationsDateRange()
                    val result = adminOperationsModule.operationalTodayReportUseCase.execute(
                        GetAdminOperationalTodayReportCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            activityId = call.query("activityId"),
                            businessDate = range.date,
                            from = range.from,
                            to = range.to,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.REPORTS_SALES_VIEW)) {
                get("/reports/sales-summary") {
                    val context = call.hermesAuthContext()
                    val range = call.adminOperationsDateRange()
                    val result = adminOperationsModule.salesSummaryReportUseCase.execute(
                        GetAdminSalesSummaryReportCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            activityId = call.query("activityId"),
                            from = range.from,
                            to = range.to,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.REPORTS_CASH_VIEW)) {
                get("/reports/cash-summary") {
                    val context = call.hermesAuthContext()
                    val range = call.adminOperationsDateRange()
                    val result = adminOperationsModule.cashSummaryReportUseCase.execute(
                        GetAdminCashSummaryReportCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            from = range.from,
                            to = range.to,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.REPORTS_TAX_VIEW)) {
                get("/reports/tax-summary") {
                    val context = call.hermesAuthContext()
                    val range = call.adminOperationsDateRange()
                    val result = adminOperationsModule.taxSummaryReportUseCase.execute(
                        GetAdminTaxSummaryReportCommand(
                            organizationId = call.adminOperationsOrganizationId(),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.query("branchId"),
                            activityId = call.query("activityId"),
                            from = range.from,
                            to = range.to,
                        ),
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private data class AdminOperationsRange(
    val date: LocalDate,
    val from: Instant,
    val to: Instant,
)

private fun ApplicationCall.adminOperationsOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.requiredAdminOperationsPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

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

private fun ApplicationCall.adminOperationsDateRange(): AdminOperationsRange {
    val timezone = query("timezone")?.let { ZoneId.of(it) } ?: ZoneId.of("UTC")
    val explicitFrom = queryInstant("from")
    val explicitTo = queryInstant("to")
    if (explicitFrom != null && explicitTo != null) {
        val date = explicitFrom.atZone(timezone).toLocalDate()
        return AdminOperationsRange(date = date, from = explicitFrom, to = explicitTo)
    }

    val date = query("date")?.let { LocalDate.parse(it) } ?: LocalDate.now(timezone)
    val from = date.atStartOfDay(timezone).toInstant()
    val to = date.plusDays(1).atStartOfDay(timezone).toInstant()
    return AdminOperationsRange(date = date, from = from, to = to)
}
