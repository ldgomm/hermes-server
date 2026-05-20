package com.hermes.backend.routes

import com.hermes.application.admin.catalog.ChangeAdminCatalogLocalItemStatusCommand
import com.hermes.application.admin.catalog.GetAdminCatalogRequestCommand
import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.catalog.CatalogGetOrganizationItemCommand
import com.hermes.application.catalog.CatalogGetTemplateCommand
import com.hermes.backend.admin.catalog.AdminCatalogModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureAdminCatalogRoutes(
    authModule: com.hermes.backend.auth.AuthModule,
    adminCatalogModule: AdminCatalogModule,
) {
    routing { adminCatalogRoutes(authModule = authModule, adminCatalogModule = adminCatalogModule) }
}

fun Route.adminCatalogRoutes(
    authModule: com.hermes.backend.auth.AuthModule,
    adminCatalogModule: AdminCatalogModule,
) {
    adminCatalogRoutes(
        authenticateRequestUseCase = authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
        adminCatalogModule = adminCatalogModule,
    )
}

fun Route.adminCatalogRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    adminCatalogModule: AdminCatalogModule,
) {
    route("/api/v1/admin/catalog") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/master") {
                route("/templates") {
                    hermesRequiresAnyPermission(setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER)) {
                        get {
                            val context = call.hermesAuthContext()
                            val result = adminCatalogModule.searchMasterTemplatesUseCase.execute(
                                adminCatalogMasterTemplateSearchCommand(
                                    organizationId = call.adminCatalogOrganizationId(),
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                                    identifier = call.request.queryParameters["identifier"],
                                    type = call.request.queryParameters["type"],
                                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                                ),
                            )
                            call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                        }

                        get("/{templateId}") {
                            val context = call.hermesAuthContext()
                            val result = adminCatalogModule.getTemplateUseCase.execute(
                                CatalogGetTemplateCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    templateId = call.requiredAdminCatalogPath("templateId"),
                                ),
                            )
                            call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                        }
                    }

                    hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                        post {
                            val context = call.hermesAuthContext()
                            val request = call.receive<CreateAdminCatalogMasterTemplateRequest>()
                            val result = adminCatalogModule.createPlatformTemplateUseCase.execute(
                                request.toCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                ),
                            )
                            call.respond(HttpStatusCode.Created, result.toAdminCatalogResponse())
                        }
                    }
                }

                route("/categories") {
                    hermesRequiresAnyPermission(setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER)) {
                        get {
                            val context = call.hermesAuthContext()
                            val result = adminCatalogModule.searchCategoriesUseCase.execute(
                                adminCatalogCategorySearchCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    parentId = call.request.queryParameters["parentId"],
                                    query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                                    statuses = call.request.queryParameters["statuses"],
                                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                                ),
                            )
                            call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                        }
                    }

                    hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                        post {
                            val context = call.hermesAuthContext()
                            val request = call.receive<CreateAdminCatalogCategoryRequest>()
                            val result = adminCatalogModule.createCategoryUseCase.execute(
                                request.toCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                ),
                            )
                            call.respond(HttpStatusCode.Created, result.toAdminCatalogResponse())
                        }
                    }
                }

                route("/families") {
                    hermesRequiresAnyPermission(setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER)) {
                        get {
                            val context = call.hermesAuthContext()
                            val result = adminCatalogModule.searchFamiliesUseCase.execute(
                                adminCatalogFamilySearchCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                                    categoryId = call.request.queryParameters["categoryId"],
                                    type = call.request.queryParameters["type"],
                                    statuses = call.request.queryParameters["statuses"],
                                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                                ),
                            )
                            call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                        }
                    }

                    hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                        post {
                            val context = call.hermesAuthContext()
                            val request = call.receive<CreateAdminCatalogFamilyRequest>()
                            val result = adminCatalogModule.createFamilyUseCase.execute(
                                request.toCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                ),
                            )
                            call.respond(HttpStatusCode.Created, result.toAdminCatalogResponse())
                        }
                    }
                }
            }

            route("/local/items") {
                hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_VIEW) {
                    get {
                        val context = call.hermesAuthContext()
                        val result = adminCatalogModule.searchOrganizationItemsUseCase.execute(
                            adminCatalogLocalItemSearchCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                                identifier = call.request.queryParameters["identifier"],
                                type = call.request.queryParameters["type"],
                                statuses = call.request.queryParameters["statuses"],
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }

                    get("/{itemId}") {
                        val context = call.hermesAuthContext()
                        val result = adminCatalogModule.getOrganizationItemUseCase.execute(
                            CatalogGetOrganizationItemCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                catalogItemId = call.requiredAdminCatalogPath("itemId"),
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER) {
                    post("/copy-from-template") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<CopyAdminCatalogItemFromTemplateRequest>()
                        val result = adminCatalogModule.copyTemplateToOrganizationUseCase.execute(
                            request.toCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            ),
                        )
                        call.respond(HttpStatusCode.Created, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresAnyPermission(
                    setOf(
                        PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY,
                        PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE,
                        PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE,
                    ),
                ) {
                    put("/{itemId}") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<UpdateAdminCatalogLocalItemRequest>()
                        val result = adminCatalogModule.updateLocalItemUseCase.execute(
                            request.toCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                itemId = call.requiredAdminCatalogPath("itemId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY) {
                    post("/{itemId}/activate") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminCatalogLocalItemActionRequest>()
                        val result = adminCatalogModule.changeLocalItemStatusUseCase.execute(
                            ChangeAdminCatalogLocalItemStatusCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                catalogItemId = call.requiredAdminCatalogPath("itemId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                targetStatus = CatalogItemStatus.ACTIVE,
                                reason = request.reason,
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY) {
                    post("/{itemId}/deactivate") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminCatalogLocalItemActionRequest>()
                        val result = adminCatalogModule.changeLocalItemStatusUseCase.execute(
                            ChangeAdminCatalogLocalItemStatusCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                catalogItemId = call.requiredAdminCatalogPath("itemId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                targetStatus = CatalogItemStatus.PAUSED,
                                reason = request.reason,
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }

                    post("/{itemId}/remove") {
                        val context = call.hermesAuthContext()
                        val request = call.receive<AdminCatalogLocalItemActionRequest>()
                        val result = adminCatalogModule.removeLocalItemUseCase.execute(
                            request.toRemoveCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                itemId = call.requiredAdminCatalogPath("itemId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }
                }
            }

            route("/requests") {
                hermesRequiresAnyPermission(
                    setOf(
                        PermissionCatalog.CATALOG_LOCAL_VIEW,
                        PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM,
                        PermissionCatalog.CATALOG_MANAGE_MASTER,
                    ),
                ) {
                    get {
                        val context = call.hermesAuthContext()
                        val platformScope = call.request.queryParameters["scope"]?.equals("platform", ignoreCase = true) == true
                        val result = if (platformScope) {
                            adminCatalogModule.listAdminRequestsUseCase.execute(
                                adminCatalogPlatformRequestsCommand(
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    organizationId = call.request.queryParameters["organizationId"],
                                    statuses = call.request.queryParameters["statuses"],
                                    requestedType = call.request.queryParameters["requestedType"],
                                    query = call.request.queryParameters["q"] ?: call.request.queryParameters["query"],
                                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                                ),
                            )
                        } else {
                            adminCatalogModule.listOrganizationRequestsUseCase.execute(
                                adminCatalogOrganizationRequestsCommand(
                                    organizationId = call.adminCatalogOrganizationId(),
                                    actorUserId = context.userId,
                                    actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    statuses = call.request.queryParameters["statuses"],
                                    requestedType = call.request.queryParameters["requestedType"],
                                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                                ),
                            )
                        }
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }

                    get("/{requestId}") {
                        val context = call.hermesAuthContext()
                        val result = adminCatalogModule.getRequestUseCase.execute(
                            GetAdminCatalogRequestCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                requestId = call.requiredAdminCatalogPath("requestId"),
                            ),
                        )
                        call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM) {
                    post {
                        val context = call.hermesAuthContext()
                        val request = call.receive<CreateAdminCatalogRequestRequest>()
                        val result = adminCatalogModule.requestNewItemUseCase.execute(
                            request.toCommand(
                                organizationId = call.adminCatalogOrganizationId(),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            ),
                        )
                        call.respond(HttpStatusCode.Created, result.toAdminCatalogResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                    post("/{requestId}/review") {
                        val context = call.hermesAuthContext()
                        val requestId = call.requiredAdminCatalogPath("requestId")
                        val request = call.receive<ReviewAdminCatalogRequestRequest>()
                        when (request.toAction()) {
                            AdminCatalogReviewAction.APPROVE_AS_TEMPLATE -> {
                                val result = adminCatalogModule.approveRequestAsTemplateUseCase.execute(
                                    request.toApproveAsTemplateCommand(
                                        requestId = requestId,
                                        actorUserId = context.userId,
                                        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    ),
                                )
                                call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                            }

                            AdminCatalogReviewAction.REJECT -> {
                                val result = adminCatalogModule.rejectRequestUseCase.execute(
                                    request.toRejectCommand(
                                        requestId = requestId,
                                        actorUserId = context.userId,
                                        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    ),
                                )
                                call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                            }

                            AdminCatalogReviewAction.LINK_TO_EXISTING_TEMPLATE -> {
                                val result = adminCatalogModule.linkRequestToExistingTemplateUseCase.execute(
                                    request.toLinkToExistingTemplateCommand(
                                        requestId = requestId,
                                        actorUserId = context.userId,
                                        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    ),
                                )
                                call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                            }

                            AdminCatalogReviewAction.REQUEST_MORE_INFO -> {
                                val result = adminCatalogModule.requestMoreInfoUseCase.execute(
                                    request.toRequestMoreInfoCommand(
                                        requestId = requestId,
                                        actorUserId = context.userId,
                                        actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                                    ),
                                )
                                call.respond(HttpStatusCode.OK, result.toAdminCatalogResponse())
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.adminCatalogOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.requiredAdminCatalogPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")
