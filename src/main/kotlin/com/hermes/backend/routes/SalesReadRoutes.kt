package com.hermes.backend.routes

import com.hermes.application.sales.*
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.sales.toResponse
import com.hermes.domain.payment.SalePaymentStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

fun Application.configureSalesReadRoutes(
    authModule: AuthModule,
    salesReadUseCases: SalesReadUseCases,
) {
    configureSalesReadRoutes(
        authModule = authModule,
        searchSalesReadUseCase = salesReadUseCases.searchSalesReadUseCase,
        listPendingSalesUseCase = salesReadUseCases.listPendingSalesUseCase,
        getSalesDaySummaryUseCase = salesReadUseCases.getSalesDaySummaryUseCase,
    )
}

fun Application.configureSalesReadRoutes(
    authModule: AuthModule,
    searchSalesReadUseCase: SearchSalesReadUseCase,
    listPendingSalesUseCase: ListPendingSalesUseCase,
    getSalesDaySummaryUseCase: GetSalesDaySummaryUseCase,
) {
    routing {
        salesReadRoutes(
            authModule = authModule,
            searchSalesReadUseCase = searchSalesReadUseCase,
            listPendingSalesUseCase = listPendingSalesUseCase,
            getSalesDaySummaryUseCase = getSalesDaySummaryUseCase,
        )
    }
}

fun Route.salesReadRoutes(
    authModule: AuthModule,
    searchSalesReadUseCase: SearchSalesReadUseCase,
    listPendingSalesUseCase: ListPendingSalesUseCase,
    getSalesDaySummaryUseCase: GetSalesDaySummaryUseCase,
) {
    route("/organizations/{organizationId}/sales") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.SALES_VIEW) {
                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredSalesReadPath("organizationId")
                    call.assertSalesReadOrganizationMatchesContext(organizationId)

                    val result = searchSalesReadUseCase.execute(
                        SalesSearchCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.request.queryParameters["branchId"],
                            activityId = call.request.queryParameters["activityId"],
                            customerId = call.request.queryParameters["customerId"],
                            operationalStatuses = call.enumSetQuery<SaleOperationalStatus>("operationalStatus"),
                            paymentStatuses = call.enumSetQuery<SalePaymentStatus>("paymentStatus"),
                            saleTypes = call.enumSetQuery<SaleType>("saleType"),
                            from = call.optionalInstantQuery("from"),
                            to = call.optionalInstantQuery("to"),
                            query = call.request.queryParameters["q"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/pending") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredSalesReadPath("organizationId")
                    call.assertSalesReadOrganizationMatchesContext(organizationId)

                    val result = listPendingSalesUseCase.execute(
                        PendingSalesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.request.queryParameters["branchId"],
                            activityId = call.request.queryParameters["activityId"],
                            now = Instant.now(),
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/summary/day") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredSalesReadPath("organizationId")
                    call.assertSalesReadOrganizationMatchesContext(organizationId)

                    val from = call.optionalInstantQuery("from")
                        ?: throw DomainRuleViolation("Query parameter 'from' is required.")
                    val to = call.optionalInstantQuery("to")
                        ?: throw DomainRuleViolation("Query parameter 'to' is required.")

                    val result = getSalesDaySummaryUseCase.execute(
                        SalesDaySummaryCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            branchId = call.request.queryParameters["branchId"],
                            activityId = call.request.queryParameters["activityId"],
                            from = from,
                            to = to,
                        )
                    )

                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.requiredSalesReadPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("Path parameter '$name' is required.")

private fun ApplicationCall.assertSalesReadOrganizationMatchesContext(organizationId: String) {
    val contextOrganizationId = hermesAuthContext().organizationId
    if (contextOrganizationId != organizationId) {
        throw DomainRuleViolation("Path organization does not match active organization.")
    }
}

private fun ApplicationCall.optionalInstantQuery(name: String): Instant? =
    request.queryParameters[name]?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse)

private inline fun <reified T : Enum<T>> ApplicationCall.enumSetQuery(name: String): Set<T> =
    request.queryParameters.getAll(name)
        .orEmpty()
        .flatMap { it.split(",") }
        .mapNotNull { raw -> raw.trim().takeIf { it.isNotBlank() } }
        .map { enumValueOf<T>(it.uppercase()) }
        .toSet()
