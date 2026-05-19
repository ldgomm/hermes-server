package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.electronicinvoicing.ElectronicInvoicingModule
import com.hermes.backend.electronicinvoicing.electronicInvoiceSearchCommand
import com.hermes.backend.electronicinvoicing.toDetailResponse
import com.hermes.backend.electronicinvoicing.toResponse
import com.hermes.domain.permission.PermissionCatalog
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureElectronicInvoiceRoutes(
    authModule: AuthModule,
    electronicInvoicingModule: ElectronicInvoicingModule,
) {
    routing {
        electronicInvoiceRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            electronicInvoicingModule = electronicInvoicingModule,
        )
    }
}

fun Route.electronicInvoiceRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    electronicInvoicingModule: ElectronicInvoicingModule,
) {
    route("/api/v1/electronic-invoices") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_LIST) {
                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.listElectronicInvoicesUseCase.execute(
                        electronicInvoiceSearchCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            saleId = call.request.queryParameters["saleId"],
                            statuses = call.request.queryParameters["statuses"],
                            environment = call.request.queryParameters["environment"],
                            from = call.request.queryParameters["from"],
                            to = call.request.queryParameters["to"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW) {
                get("/{documentId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getElectronicInvoiceUseCase.execute(
                        GetElectronicInvoiceCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toDetailResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.requiredElectronicInvoiceOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.requiredElectronicInvoicePath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$name path parameter is required.")
