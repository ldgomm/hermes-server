package com.hermes.backend.routes

import com.hermes.application.catalog.CatalogListAdminRequestsCommand
import com.hermes.application.catalog.CatalogListOrganizationRequestsCommand
import com.hermes.backend.auth.*
import com.hermes.backend.catalog.*
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCatalogRequestAdvancedRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    routing { catalogRequestAdvancedRoutes(authModule, catalogModule) }
}

fun Route.catalogRequestAdvancedRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    route("/admin/catalog/requests") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                get {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.listAdminRequestsUseCase.execute(
                        CatalogListAdminRequestsCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            organizationId = call.request.queryParameters["organizationId"],
                            statuses = call.catalogRequestStatuses(),
                            requestedType = call.request.queryParameters["type"]?.let {
                                enumValueOf<CatalogItemType>(
                                    it.trim().uppercase()
                                )
                            },
                            query = call.request.queryParameters["query"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/{requestId}/approve") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogApproveRequestAsTemplateRequest>()
                    val result = catalogModule.approveRequestAsTemplateUseCase.execute(
                        request.toCommand(
                            requestId = call.requiredCatalogRequestPath("requestId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/{requestId}/reject") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogRejectRequestRequest>()
                    val result = catalogModule.rejectRequestUseCase.execute(
                        request.toCommand(
                            requestId = call.requiredCatalogRequestPath("requestId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toAdvancedResponse())
                }

                post("/{requestId}/link-existing-template") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogLinkRequestToExistingTemplateRequest>()
                    val result = catalogModule.linkRequestToExistingTemplateUseCase.execute(
                        request.toCommand(
                            requestId = call.requiredCatalogRequestPath("requestId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toAdvancedResponse())
                }

                post("/{requestId}/request-more-info") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogRequestMoreInfoRequest>()
                    val result = catalogModule.requestMoreInfoUseCase.execute(
                        request.toCommand(
                            requestId = call.requiredCatalogRequestPath("requestId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toAdvancedResponse())
                }
            }
        }
    }

    route("/organizations/{organizationId}/catalog/requests") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(
                setOf(
                    PermissionCatalog.CATALOG_LOCAL_VIEW,
                    PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM
                )
            ) {
                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogRequestOrganizationId()
                    call.assertCatalogRequestOrganizationMatchesContext(organizationId)
                    val result = catalogModule.listOrganizationRequestsUseCase.execute(
                        CatalogListOrganizationRequestsCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            statuses = call.catalogRequestStatuses(),
                            requestedType = call.request.queryParameters["type"]?.let {
                                enumValueOf<CatalogItemType>(
                                    it.trim().uppercase()
                                )
                            },
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.catalogRequestStatuses(): Set<CatalogItemRequestStatus> =
    request.queryParameters["statuses"]
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { enumValueOf<CatalogItemRequestStatus>(it.uppercase()) }
        ?.toSet()
        .orEmpty()

private fun ApplicationCall.requiredCatalogRequestOrganizationId(): String =
    requiredCatalogRequestPath("organizationId")

private fun ApplicationCall.requiredCatalogRequestPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

private fun ApplicationCall.assertCatalogRequestOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) throw DomainRuleViolation("Authenticated organization does not match route organization.")
}
