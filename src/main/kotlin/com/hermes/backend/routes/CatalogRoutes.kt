package com.hermes.backend.routes

import com.hermes.application.catalog.AssignTaxProfileToCatalogItemCommand
import com.hermes.application.catalog.CatalogDisableLocalItemCommand
import com.hermes.application.catalog.CatalogSearchMasterTemplatesCommand
import com.hermes.application.catalog.CatalogSearchOrganizationItemsCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.catalog.CatalogAssignTaxProfileRequest
import com.hermes.backend.catalog.CatalogCopyFromTemplateRequest
import com.hermes.backend.catalog.CatalogCreatePlatformTemplateRequest
import com.hermes.backend.catalog.CatalogDisableLocalItemRequest
import com.hermes.backend.catalog.CatalogModule
import com.hermes.backend.catalog.CatalogRequestNewItemRequest
import com.hermes.backend.catalog.CatalogReviewRequestRequest
import com.hermes.backend.catalog.CatalogUpdateLocalItemRequest
import com.hermes.backend.catalog.toCommand
import com.hermes.backend.catalog.toResponse
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureCatalogRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    routing { catalogRoutes(authModule, catalogModule) }
}

fun Route.catalogRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    route("/admin/catalog") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                post("/templates") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogCreatePlatformTemplateRequest>()
                    val result = catalogModule.createPlatformTemplateUseCase.execute(
                        request.toCommand(context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.template.toResponse())
                }

                post("/requests/{requestId}/review") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogReviewRequestRequest>()
                    val result = catalogModule.reviewRequestUseCase.execute(
                        request.toCommand(
                            requestId = call.requiredCatalogPath("requestId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }

    route("/organizations/{organizationId}/catalog") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_VIEW) {
                get("/master/templates") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val result = catalogModule.searchMasterTemplatesUseCase.execute(
                        CatalogSearchMasterTemplatesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            query = call.request.queryParameters["query"],
                            identifier = call.request.queryParameters["identifier"],
                            type = call.request.queryParameters["type"]?.let { enumValueOf<CatalogItemType>(it.uppercase()) },
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/items") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val statuses = call.request.queryParameters["statuses"]
                        ?.split(',')
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.map { enumValueOf<CatalogItemStatus>(it.uppercase()) }
                        ?.toSet()
                        .orEmpty()
                    val result = catalogModule.searchOrganizationItemsUseCase.execute(
                        CatalogSearchOrganizationItemsCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            query = call.request.queryParameters["query"],
                            identifier = call.request.queryParameters["identifier"],
                            type = call.request.queryParameters["type"]?.let { enumValueOf<CatalogItemType>(it.uppercase()) },
                            statuses = statuses,
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER) {
                post("/items/copy-from-master") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogCopyFromTemplateRequest>()
                    val result = catalogModule.copyTemplateToOrganizationUseCase.execute(
                        request.toCommand(organizationId, context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(
                setOf(
                    PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE,
                    PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE,
                )
            ) {
                patch("/items/{catalogItemId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogUpdateLocalItemRequest>()
                    val result = catalogModule.updateLocalItemUseCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            catalogItemId = call.requiredCatalogPath("catalogItemId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY) {
                post("/items/{catalogItemId}/disable") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogDisableLocalItemRequest>()
                    val result = catalogModule.disableLocalItemUseCase.execute(
                        CatalogDisableLocalItemCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            catalogItemId = call.requiredCatalogPath("catalogItemId"),
                            reason = request.reason,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE, PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM)) {
                post("/items/{catalogItemId}/tax-profile") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogAssignTaxProfileRequest>()
                    val result = catalogModule.assignTaxProfileToCatalogItemUseCase.execute(
                        AssignTaxProfileToCatalogItemCommand(
                            organizationId = organizationId,
                            catalogItemId = call.requiredCatalogPath("catalogItemId"),
                            taxProfileCode = request.taxProfileCode,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            reason = request.reason,
                        )
                    )
                    call.respond(HttpStatusCode.OK, mapOf("assignment" to result.assignment))
                }
            }

            hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM) {
                post("/requests") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogRequestNewItemRequest>()
                    val result = catalogModule.requestNewItemUseCase.execute(
                        request.toCommand(organizationId, context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.requiredCatalogOrganizationId(): String = requiredCatalogPath("organizationId")

private fun ApplicationCall.requiredCatalogPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

private fun ApplicationCall.assertCatalogOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) throw DomainRuleViolation("Authenticated organization does not match route organization.")
}
