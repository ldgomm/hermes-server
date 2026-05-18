package com.hermes.backend.routes

import com.hermes.application.catalog.CatalogAuditAction
import com.hermes.application.catalog.CatalogListAuditEventsCommand
import com.hermes.application.catalog.CatalogListPriceHistoryCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.catalog.CatalogModule
import com.hermes.backend.catalog.toResponse
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.format.DateTimeParseException

fun Application.configureCatalogObservabilityRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    routing { catalogObservabilityRoutes(authModule, catalogModule) }
}

fun Route.catalogObservabilityRoutes(authModule: AuthModule, catalogModule: CatalogModule) {
    route("/organizations/{organizationId}/catalog") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(setOf(PermissionCatalog.AUDIT_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER)) {
                get("/audit") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogObservabilityOrganizationId()
                    call.assertCatalogObservabilityOrganizationMatchesContext(organizationId)

                    val result = catalogModule.listAuditEventsUseCase.execute(
                        CatalogListAuditEventsCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            actions = call.catalogAuditActions(),
                            targetId = call.request.queryParameters["targetId"],
                            auditedActorUserId = call.request.queryParameters["actorUserId"],
                            from = call.optionalCatalogInstant("from"),
                            to = call.optionalCatalogInstant("to"),
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(
                setOf(
                    PermissionCatalog.CATALOG_PRICE_HISTORY_VIEW,
                    PermissionCatalog.CATALOG_LOCAL_VIEW,
                    PermissionCatalog.CATALOG_MANAGE_MASTER,
                )
            ) {
                get("/items/{catalogItemId}/price-history") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredCatalogObservabilityOrganizationId()
                    call.assertCatalogObservabilityOrganizationMatchesContext(organizationId)

                    val result = catalogModule.listPriceHistoryUseCase.execute(
                        CatalogListPriceHistoryCommand(
                            organizationId = organizationId,
                            catalogItemId = call.requiredCatalogObservabilityPath("catalogItemId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            from = call.optionalCatalogInstant("from"),
                            to = call.optionalCatalogInstant("to"),
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private fun ApplicationCall.catalogAuditActions(): Set<CatalogAuditAction> =
    request.queryParameters["actions"]
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { raw ->
            runCatching { enumValueOf<CatalogAuditAction>(raw.uppercase()) }
                .getOrElse { throw DomainRuleViolation("Invalid catalog audit action: $raw.") }
        }
        ?.toSet()
        .orEmpty()

private fun ApplicationCall.optionalCatalogInstant(name: String): Instant? {
    val value = request.queryParameters[name]?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        throw DomainRuleViolation("Query parameter $name must be a valid ISO-8601 instant.")
    }
}

private fun ApplicationCall.requiredCatalogObservabilityOrganizationId(): String =
    requiredCatalogObservabilityPath("organizationId")

private fun ApplicationCall.requiredCatalogObservabilityPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

private fun ApplicationCall.assertCatalogObservabilityOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) throw DomainRuleViolation("Authenticated organization does not match route organization.")
}
