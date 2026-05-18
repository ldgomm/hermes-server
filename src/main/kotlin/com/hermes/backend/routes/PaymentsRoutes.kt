package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresAnyPermission
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.payments.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configurePaymentsRoutes(
    authModule: com.hermes.backend.auth.AuthModule,
    paymentsModule: PaymentsModule,
) {
    routing {
        paymentsRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            paymentsModule = paymentsModule,
        )
    }
}

fun Route.paymentsRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    paymentsModule: PaymentsModule,
) {
    route("/organizations/{organizationId}") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/cash/sessions") {
                hermesRequiresPermission(PermissionCatalog.CASH_SESSION_OPEN) {
                    post {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<OpenCashSessionRequest>()
                        val result = paymentsModule.openCashSessionUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.cashSession.toResponse())
                    }
                }

                hermesRequiresAnyPermission(
                    setOf(
                        PermissionCatalog.CASH_MOVEMENTS_REGISTER_INFLOW,
                        PermissionCatalog.CASH_MOVEMENTS_REGISTER_OUTFLOW,
                        PermissionCatalog.CASH_MOVEMENTS_ADJUST,
                    )
                ) {
                    post("/{cashSessionId}/movements") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<RegisterCashMovementRequest>()
                        val result = paymentsModule.registerCashMovementUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                cashSessionId = call.requiredPaymentsPath("cashSessionId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.cashMovement.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.CASH_SESSION_CLOSE) {
                    post("/{cashSessionId}/close") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<CloseCashSessionRequest>()
                        val result = paymentsModule.closeCashSessionUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                cashSessionId = call.requiredPaymentsPath("cashSessionId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }
            }

            route("/sales/{saleId}") {
                hermesRequiresPermission(PermissionCatalog.PAYMENTS_COLLECT) {
                    post("/payments") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<RegisterPaymentRequest>()
                        val result = paymentsModule.registerPaymentUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredPaymentsPath("saleId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }
                }

                hermesRequiresAnyPermission(
                    setOf(
                        PermissionCatalog.PAYMENTS_MARK_AS_CREDIT,
                        PermissionCatalog.RECEIVABLES_CREATE
                    )
                ) {
                    post("/receivable") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<CreateReceivableForSaleRequest>()
                        val result = paymentsModule.createReceivableForSaleUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredPaymentsPath("saleId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.receivable.toResponse())
                    }
                }
            }

            route("/receivables/{receivableId}") {
                hermesRequiresPermission(PermissionCatalog.RECEIVABLES_REGISTER_PAYMENT) {
                    post("/collections") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredPaymentsOrganizationId()
                        call.assertPaymentsOrganizationMatchesContext(organizationId)
                        val request = call.receive<RegisterReceivableCollectionRequest>()
                        val result = paymentsModule.registerReceivableCollectionUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                receivableId = call.requiredPaymentsPath("receivableId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.receivable.toResponse())
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.requiredPaymentsOrganizationId(): String = requiredPaymentsPath("organizationId")

private fun ApplicationCall.requiredPaymentsPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$name path parameter is required.")

private fun ApplicationCall.assertPaymentsOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}
