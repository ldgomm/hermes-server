package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.documents.CommercialDocumentsModule
import com.hermes.backend.documents.EmailCommercialDocumentRequest
import com.hermes.backend.documents.EmailCommercialDocumentResponse
import com.hermes.backend.documents.GenerateInternalTicketRequest
import com.hermes.backend.documents.RegisterPhysicalSaleNoteRequest
import com.hermes.backend.documents.downloadCommercialDocumentPdfCommand
import com.hermes.backend.documents.searchCommercialDocumentsCommand
import com.hermes.backend.documents.toCommand
import com.hermes.backend.documents.toResponse
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureCommercialDocumentRoutes(
    authModule: com.hermes.backend.auth.AuthModule,
    commercialDocumentsModule: CommercialDocumentsModule,
) {
    routing {
        commercialDocumentRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            commercialDocumentsModule = commercialDocumentsModule,
        )
    }
}

fun Route.commercialDocumentRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    commercialDocumentsModule: CommercialDocumentsModule,
) {
    route("/organizations/{organizationId}") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            route("/sales/{saleId}/documents") {
                hermesRequiresPermission(PermissionCatalog.DOCUMENTS_GENERATE_INTERNAL_TICKET) {
                    post("/internal-ticket") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val request = call.receive<GenerateInternalTicketRequest>()
                        val result = commercialDocumentsModule.generateInternalTicketUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredDocumentPath("saleId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.DOCUMENTS_GENERATE_PHYSICAL_SALE_NOTE_REGISTRY) {
                    post("/physical-sale-note") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val request = call.receive<RegisterPhysicalSaleNoteRequest>()
                        val result = commercialDocumentsModule.registerPhysicalSaleNoteUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                saleId = call.requiredDocumentPath("saleId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.Created, result.toResponse())
                    }
                }
            }

            route("/documents") {
                hermesRequiresPermission(PermissionCatalog.DOCUMENTS_VIEW) {
                    get {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val result = commercialDocumentsModule.searchCommercialDocumentsUseCase.execute(
                            searchCommercialDocumentsCommand(
                                organizationId = organizationId,
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                                saleId = call.request.queryParameters["saleId"],
                                documentType = call.request.queryParameters["documentType"],
                                statuses = call.request.queryParameters["statuses"],
                                from = call.request.queryParameters["from"],
                                to = call.request.queryParameters["to"],
                                limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }

                    get("/{documentId}") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val result = commercialDocumentsModule.getCommercialDocumentUseCase.execute(
                            com.hermes.application.documents.GetCommercialDocumentCommand(
                                organizationId = organizationId,
                                documentId = call.requiredDocumentPath("documentId"),
                                actorUserId = context.userId,
                                actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, result.toResponse())
                    }
                }

                hermesRequiresPermission(PermissionCatalog.DOCUMENTS_DOWNLOAD_PDF) {
                    get("/{documentId}/pdf") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val result = commercialDocumentsModule.downloadCommercialDocumentPdfUseCase.execute(
                            downloadCommercialDocumentPdfCommand(
                                organizationId = organizationId,
                                documentId = call.requiredDocumentPath("documentId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        val file = result.file ?: throw DomainRuleViolation("Commercial document PDF file is missing.")
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.filename).toString(),
                        )
                        call.respondBytes(file.bytes, ContentType.Application.Pdf, HttpStatusCode.OK)
                    }

                    post("/{documentId}/email") {
                        val context = call.hermesAuthContext()
                        val organizationId = call.requiredDocumentOrganizationId()
                        call.assertDocumentOrganizationMatchesContext(organizationId)
                        val request = call.receive<EmailCommercialDocumentRequest>()
                        val result = commercialDocumentsModule.emailCommercialDocumentUseCase.execute(
                            request.toCommand(
                                organizationId = organizationId,
                                documentId = call.requiredDocumentPath("documentId"),
                                actorUserId = context.userId,
                                permissions = context.effectivePermissions?.permissions.orEmpty(),
                            )
                        )
                        call.respond(HttpStatusCode.OK, EmailCommercialDocumentResponse(result.document.toResponse(), result.delivered))
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.requiredDocumentOrganizationId(): String = requiredDocumentPath("organizationId")

private fun ApplicationCall.requiredDocumentPath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("$name path parameter is required.")

private fun ApplicationCall.assertDocumentOrganizationMatchesContext(organizationId: String) {
    val activeOrganizationId = hermesAuthContext().requireActiveOrganization().organization.id
    if (activeOrganizationId != organizationId) {
        throw DomainRuleViolation("Authenticated organization does not match route organization.")
    }
}
