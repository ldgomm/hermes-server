package com.hermes.backend.routes

import com.hermes.backend.auth.*
import com.hermes.backend.sales.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureReservationSchedulingRoutes(
    authModule: AuthModule,
    reservationSchedulingModule: ReservationSchedulingModule,
) {
    routing { reservationSchedulingRoutes(authModule, reservationSchedulingModule) }
}

fun Route.reservationSchedulingRoutes(
    authModule: AuthModule,
    reservationSchedulingModule: ReservationSchedulingModule,
) {
    route("/organizations/{organizationId}/reservations") {
        hermesAuthenticated(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresAnyPermission(setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.SALES_CREATE)) {
                post("/availability") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredReservationSchedulingOrganizationId()
                    call.assertReservationSchedulingOrganizationMatchesContext(organizationId)
                    val request = call.receive<CheckReservationAvailabilityRequest>()
                    val result = reservationSchedulingModule.checkReservationAvailabilityUseCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresAnyPermission(setOf(PermissionCatalog.SALES_CONFIRM, PermissionCatalog.SALES_CANCEL)) {
                patch("/{reservationId}/status") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredReservationSchedulingOrganizationId()
                    call.assertReservationSchedulingOrganizationMatchesContext(organizationId)
                    val request = call.receive<ChangeReservationStatusRequest>()
                    val result = reservationSchedulingModule.changeReservationStatusUseCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            reservationId = call.requiredReservationSchedulingPath("reservationId"),
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.SALES_CREATE) {
                patch("/{reservationId}/reschedule") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredReservationSchedulingOrganizationId()
                    call.assertReservationSchedulingOrganizationMatchesContext(organizationId)
                    val request = call.receive<RescheduleReservationRequest>()
                    val result = reservationSchedulingModule.rescheduleReservationUseCase.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            reservationId = call.requiredReservationSchedulingPath("reservationId"),
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

private fun ApplicationCall.requiredReservationSchedulingOrganizationId(): String =
    requiredReservationSchedulingPath("organizationId")

private fun ApplicationCall.requiredReservationSchedulingPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")

private fun ApplicationCall.assertReservationSchedulingOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}
