package com.hermes.backend.routes

import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.catalog.CatalogInitialSeedRequest
import com.hermes.backend.catalog.CatalogModule
import com.hermes.backend.catalog.toCommand
import com.hermes.backend.catalog.toResponse
import com.hermes.domain.permission.PermissionCatalog
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCatalogSeedRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    routing { catalogSeedRoutes(authModule, catalogModule) }
}

fun Route.catalogSeedRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    route("/admin/catalog/seed") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.CATALOG_MANAGE_MASTER) {
                post("/initial") {
                    val context = call.hermesAuthContext()
                    val request = call.receive<CatalogInitialSeedRequest>()
                    val result = catalogModule.seedInitialCatalogUseCase.execute(
                        request.toCommand(
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}
