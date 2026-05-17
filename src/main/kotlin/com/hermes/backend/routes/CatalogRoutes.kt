package com.hermes.backend.routes

import com.hermes.application.catalog.AssignTaxProfileToCatalogItemCommand
import com.hermes.application.catalog.CatalogChangeTemplateStatusCommand
import com.hermes.application.catalog.CatalogDisableLocalItemCommand
import com.hermes.application.catalog.CatalogGetCategoryCommand
import com.hermes.application.catalog.CatalogGetFamilyCommand
import com.hermes.application.catalog.CatalogGetOrganizationItemCommand
import com.hermes.application.catalog.CatalogGetTemplateCommand
import com.hermes.application.catalog.CatalogLookupOrganizationItemByCodeCommand
import com.hermes.application.catalog.CatalogRemoveLocalItemCommand
import com.hermes.application.catalog.CatalogSearchMasterTemplatesCommand
import com.hermes.application.catalog.CatalogSearchOrganizationItemsCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.catalog.CatalogAssignTaxProfileRequest
import com.hermes.backend.catalog.CatalogChangeTemplateStatusRequest
import com.hermes.backend.catalog.CatalogCreateCategoryRequest
import com.hermes.backend.catalog.CatalogCreateFamilyRequest
import com.hermes.backend.catalog.CatalogCreatePlatformTemplateRequest
import com.hermes.backend.catalog.CatalogCopyFromTemplateRequest
import com.hermes.backend.catalog.CatalogDisableLocalItemRequest
import com.hermes.backend.catalog.CatalogModule
import com.hermes.backend.catalog.CatalogRemoveLocalItemRequest
import com.hermes.backend.catalog.CatalogRequestNewItemRequest
import com.hermes.backend.catalog.CatalogReviewRequestRequest
import com.hermes.backend.catalog.CatalogUpdateCategoryRequest
import com.hermes.backend.catalog.CatalogUpdateFamilyRequest
import com.hermes.backend.catalog.CatalogUpdateLocalItemRequest
import com.hermes.backend.catalog.CatalogUpdateTemplateRequest
import com.hermes.backend.catalog.categorySearchCommand
import com.hermes.backend.catalog.familySearchCommand
import com.hermes.backend.catalog.toCommand
import com.hermes.backend.catalog.toResponse
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

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
            hermesRequiresAnyPermission(setOf(PermissionCatalog.CATALOG_MANAGE_MASTER, PermissionCatalog.CATALOG_LOCAL_VIEW)) {
                get("/categories") {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.searchCategoriesUseCase.execute(
                        categorySearchCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            parentId = call.request.queryParameters["parentId"],
                            query = call.request.queryParameters["query"],
                            statuses = call.request.queryParameters["statuses"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/categories/{categoryId}") {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.getCategoryUseCase.execute(
                        CatalogGetCategoryCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            categoryId = call.requiredCatalogPath("categoryId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/families") {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.searchFamiliesUseCase.execute(
                        familySearchCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            query = call.request.queryParameters["query"],
                            categoryId = call.request.queryParameters["categoryId"],
                            type = call.request.queryParameters["type"],
                            statuses = call.request.queryParameters["statuses"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/families/{familyId}") {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.getFamilyUseCase.execute(
                        CatalogGetFamilyCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            familyId = call.requiredCatalogPath("familyId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/templates/{templateId}") {
                    val context = call.hermesAuthContext()
                    val result = catalogModule.getTemplateUseCase.execute(
                        CatalogGetTemplateCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            templateId = call.requiredCatalogPath("templateId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                post("/categories") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogCreateCategoryRequest>()
                    val result = catalogModule.createCategoryUseCase.execute(
                        request.toCommand(context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }

                patch("/categories/{categoryId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogUpdateCategoryRequest>()
                    val result = catalogModule.updateCategoryUseCase.execute(
                        request.toCommand(
                            categoryId = call.requiredCatalogPath("categoryId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/families") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogCreateFamilyRequest>()
                    val result = catalogModule.createFamilyUseCase.execute(
                        request.toCommand(context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }

                patch("/families/{familyId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogUpdateFamilyRequest>()
                    val result = catalogModule.updateFamilyUseCase.execute(
                        request.toCommand(
                            familyId = call.requiredCatalogPath("familyId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/templates") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogCreatePlatformTemplateRequest>()
                    val result = catalogModule.createPlatformTemplateUseCase.execute(
                        request.toCommand(context.userId, context.effectivePermissions?.permissions.orEmpty())
                    )
                    call.respond(HttpStatusCode.Created, result.template.toResponse())
                }

                patch("/templates/{templateId}") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogUpdateTemplateRequest>()
                    val result = catalogModule.updateTemplateUseCase.execute(
                        request.toCommand(
                            templateId = call.requiredCatalogPath("templateId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
                }

                post("/templates/{templateId}/status") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogChangeTemplateStatusRequest>()
                    val result = catalogModule.changeTemplateStatusUseCase.execute(
                        CatalogChangeTemplateStatusCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            templateId = call.requiredCatalogPath("templateId"),
                            status = enumValueOf<CatalogTemplateStatus>((request.status ?: throw DomainRuleViolation("Template status is required.")).trim().uppercase()),
                            reason = request.reason,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
                }

                post("/templates/{templateId}/publish") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogChangeTemplateStatusRequest>()
                    val result = catalogModule.changeTemplateStatusUseCase.execute(
                        CatalogChangeTemplateStatusCommand(context.userId, context.effectivePermissions?.permissions.orEmpty(), call.requiredCatalogPath("templateId"), CatalogTemplateStatus.ACTIVE, request.reason)
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
                }

                post("/templates/{templateId}/pause") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogChangeTemplateStatusRequest>()
                    val result = catalogModule.changeTemplateStatusUseCase.execute(
                        CatalogChangeTemplateStatusCommand(context.userId, context.effectivePermissions?.permissions.orEmpty(), call.requiredCatalogPath("templateId"), CatalogTemplateStatus.PAUSED, request.reason)
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
                }

                post("/templates/{templateId}/archive") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogChangeTemplateStatusRequest>()
                    val result = catalogModule.changeTemplateStatusUseCase.execute(
                        CatalogChangeTemplateStatusCommand(context.userId, context.effectivePermissions?.permissions.orEmpty(), call.requiredCatalogPath("templateId"), CatalogTemplateStatus.ARCHIVED, request.reason)
                    )
                    call.respond(HttpStatusCode.OK, result.template.toResponse())
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


                get("/items/lookup-by-code") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val result = catalogModule.lookupOrganizationItemByCodeUseCase.execute(
                        CatalogLookupOrganizationItemByCodeCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            code = call.request.queryParameters["code"]
                                ?: throw DomainRuleViolation("code query parameter is required."),
                            includeInactive = call.request.queryParameters["includeInactive"]?.toBooleanStrictOrNull() ?: false,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/items/{catalogItemId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val result = catalogModule.getOrganizationItemUseCase.execute(
                        CatalogGetOrganizationItemCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            catalogItemId = call.requiredCatalogPath("catalogItemId"),
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


            hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY) {
                post("/items/{catalogItemId}/remove") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogOrganizationId()
                    call.assertCatalogOrganizationMatchesContext(organizationId)
                    val request = call.receive<CatalogRemoveLocalItemRequest>()
                    val result = catalogModule.removeLocalItemUseCase.execute(
                        CatalogRemoveLocalItemCommand(
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
