package com.hermes.backend.routes

import com.hermes.application.sales.GetSaleCommand
import com.hermes.application.sales.SearchReservationsCommand
import com.hermes.application.sales.SearchSalesCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.sales.ChangeSaleItemStatusRequest
import com.hermes.backend.sales.ChangeSaleStatusRequest
import com.hermes.backend.sales.CreateQuickSaleRequest
import com.hermes.backend.sales.CreateReservationRequest
import com.hermes.backend.sales.CreateSaleItemLineRequest
import com.hermes.backend.sales.SaleReasonRequest
import com.hermes.backend.sales.SalesModule
import com.hermes.backend.sales.toAddCommand
import com.hermes.backend.sales.toCancelCommand
import com.hermes.backend.sales.toCloseCommand
import com.hermes.backend.sales.toCommand
import com.hermes.backend.sales.toResponse
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant

fun Application.configureSalesRoutes(authModule: AuthModule, salesModule: SalesModule) {
    routing { salesRoutes(authModule, salesModule) }
}

fun Route.salesRoutes(authModule: AuthModule, salesModule: SalesModule) {
    route("/organizations/{organizationId}") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/sales") {
                hermesRequiresPermission(PermissionCatalog.SALES_VIEW) {
                    get("/{saleId}") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val result = salesModule.getSaleUseCase.execute(
                            GetSaleCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    get {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val statuses = call.request.queryParameters["statuses"]
                            ?.split(',')
                            ?.mapNotNull { raw -> raw.trim().takeIf { it.isNotBlank() } }
                            ?.map { SaleOperationalStatus.valueOf(it.uppercase()) }
                            ?.toSet()
                            .orEmpty()
                        val result = salesModule.searchSalesUseCase.execute(
                            SearchSalesCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                statuses = statuses,
                                customerId = call.request.queryParameters["customerId"],
                                activityId = call.request.queryParameters["activityId"],
                                from = call.request.queryParameters["from"]?.let(Instant::parse),
                                to = call.request.queryParameters["to"]?.let(Instant::parse),
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.SALES_CREATE) {
                    post("/quick") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<CreateQuickSaleRequest>()
                        val result = salesModule.createQuickSaleUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }

                    post("/{saleId}/items") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<CreateSaleItemLineRequest>()
                        val result = salesModule.addSaleItemUseCase.execute(
                            request.toAddCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresAnyPermission(setOf(PermissionCatalog.SALES_CONFIRM, PermissionCatalog.SALES_CLOSE)) {
                    patch("/{saleId}/status") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<ChangeSaleStatusRequest>()
                        val result = salesModule.changeSaleStatusUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                }

                hermesRequiresPermission(PermissionCatalog.SALES_CLOSE) {
                    post("/{saleId}/close") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<SaleReasonRequest>()
                        val result = salesModule.closeSaleUseCase.execute(
                            request.toCloseCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.SALES_ITEMS_CHANGE_STATUS) {
                    patch("/{saleId}/items/{saleItemId}/status") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<ChangeSaleItemStatusRequest>()
                        val result = salesModule.changeSaleItemStatusUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                saleItemId = call.requiredSalesPath("saleItemId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresAnyPermission(setOf(PermissionCatalog.SALES_CANCEL, PermissionCatalog.SALES_CANCEL_AFTER_PAYMENT)) {
                    post("/{saleId}/cancel") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<SaleReasonRequest>()
                        val result = salesModule.cancelSaleUseCase.execute(
                            request.toCancelCommand(
                                organizationId = organizationId,
                                saleId = call.requiredSalesPath("saleId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }

            route("/reservations") {
                hermesRequiresPermission(PermissionCatalog.SALES_VIEW) {
                    get {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val result = salesModule.searchReservationsUseCase.execute(
                            SearchReservationsCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                customerId = call.request.queryParameters["customerId"],
                                activityId = call.request.queryParameters["activityId"],
                                from = call.request.queryParameters["from"]?.let(Instant::parse),
                                to = call.request.queryParameters["to"]?.let(Instant::parse),
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.SALES_CREATE) {
                    post {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredSalesOrganizationId()
                        call.assertSalesOrganizationMatchesContext(organizationId)
                        val request = call.receive<CreateReservationRequest>()
                        val result = salesModule.createReservationUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.requiredSalesOrganizationId(): String =
    parameters["organizationId"]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Organization id path parameter is required.")

private fun ApplicationCall.requiredSalesPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$name path parameter is required.")

private fun ApplicationCall.assertSalesOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}
