package com.hermes.backend.routes

import com.hermes.application.auth.ActiveOrganizationResolverUseCase
import com.hermes.application.auth.AuthenticateRequestUseCase
import com.hermes.application.auth.EffectivePermissionResolverUseCase
import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactFile
import com.hermes.application.electronicinvoicing.ElectronicInvoiceDownloadArtifactKind
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceCommand
import com.hermes.application.electronicinvoicing.GetOrganizationSriSettingsCommand
import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.hermesAuthContext
import com.hermes.backend.auth.hermesAuthenticated
import com.hermes.backend.auth.hermesRequiresPermission
import com.hermes.backend.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
        sriSettingsRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            electronicInvoicingModule = electronicInvoicingModule,
        )
        electronicSignatureRoutes(
            authenticateRequestUseCase = authModule.authenticateRequestUseCase,
            activeOrganizationResolverUseCase = authModule.activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = authModule.effectivePermissionResolverUseCase,
            electronicInvoicingModule = electronicInvoicingModule,
        )
        electronicSequenceRoutes(
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

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_ISSUE) {
                post {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request = call.receive<IssueElectronicInvoiceRequest>()
                    val result = electronicInvoicingModule.issueElectronicInvoiceFromSaleUseCase!!.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_RETRY) {
                post("/{documentId}/retry-authorization") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.retryElectronicInvoiceAuthorizationUseCase!!.execute(
                        retryAuthorizationCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_ERRORS) {
                get("/{documentId}/errors") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getElectronicInvoiceErrorsUseCase!!.execute(
                        electronicInvoiceErrorsCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_RIDE) {
                post("/{documentId}/ride") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request = GenerateRideRequest(
                        forceRegenerate = call.request.queryParameters["forceRegenerate"]?.toBooleanStrictOrNull()
                            ?: false,
                    )
                    val result = electronicInvoicingModule.generateElectronicInvoiceRideUseCase!!.execute(
                        generateRideCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            request = request,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/{documentId}/ride.pdf") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.downloadElectronicInvoiceArtifactUseCase!!.execute(
                        downloadArtifactCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            artifactKind = ElectronicInvoiceDownloadArtifactKind.RIDE_PDF,
                        )
                    )
                    call.respondElectronicInvoiceArtifact(result.artifact)
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_EMAIL) {
                post("/{documentId}/email") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request = call.receive<EmailElectronicInvoiceRequest>()
                    val result = electronicInvoicingModule.emailElectronicInvoiceUseCase!!.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_DOWNLOAD_XML) {
                get("/{documentId}/signed-xml") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.downloadElectronicInvoiceArtifactUseCase!!.execute(
                        downloadArtifactCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            artifactKind = ElectronicInvoiceDownloadArtifactKind.SIGNED_XML,
                        )
                    )
                    call.respondElectronicInvoiceArtifact(result.artifact)
                }

                get("/{documentId}/authorized-xml") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.downloadElectronicInvoiceArtifactUseCase!!.execute(
                        downloadArtifactCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            artifactKind = ElectronicInvoiceDownloadArtifactKind.AUTHORIZED_XML,
                        )
                    )
                    call.respondElectronicInvoiceArtifact(result.artifact)
                }
            }

            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_VIEW_AUDIT) {
                get("/{documentId}/timeline") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getElectronicInvoiceTimelineUseCase!!.execute(
                        timelineCommand(
                            organizationId = organizationId,
                            documentId = call.requiredElectronicInvoicePath("documentId"),
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
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

fun Route.sriSettingsRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    electronicInvoicingModule: ElectronicInvoicingModule,
) {
    route("/api/v1/sri/settings") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS) {
                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getOrganizationSriSettingsUseCase!!.execute(
                        GetOrganizationSriSettingsCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            actorEffectivePermissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                put {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request = call.receive<com.hermes.backend.electronicinvoicing.SriSettingsRequest>()
                    val result = electronicInvoicingModule.upsertOrganizationSriSettingsUseCase!!.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/readiness") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.checkOrganizationSriReadinessUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.readinessCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

fun Route.electronicSignatureRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    electronicInvoicingModule: ElectronicInvoicingModule,
) {
    route("/api/v1/electronic-signatures") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS) {
                post {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request =
                        call.receive<com.hermes.backend.electronicinvoicing.UploadElectronicSignatureRequest>()
                    val result = electronicInvoicingModule.uploadElectronicSignatureUseCase!!.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.Created, result.toResponse())
                }

                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.listElectronicSignaturesUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.listSignaturesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/{signatureId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getElectronicSignatureUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.getSignatureCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            signatureId = call.requiredElectronicInvoicePath("signatureId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/{signatureId}/validate") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.validateElectronicSignatureUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.validateSignatureCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            signatureId = call.requiredElectronicInvoicePath("signatureId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/{signatureId}/activate") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.activateElectronicSignatureUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.activateSignatureCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            signatureId = call.requiredElectronicInvoicePath("signatureId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/{signatureId}/revoke") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.revokeElectronicSignatureUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.revokeSignatureCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            signatureId = call.requiredElectronicInvoicePath("signatureId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

fun Route.electronicSequenceRoutes(
    authenticateRequestUseCase: AuthenticateRequestUseCase,
    activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
    effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
    electronicInvoicingModule: ElectronicInvoicingModule,
) {
    route("/api/v1/electronic-sequences") {
        hermesAuthenticated(
            authenticateRequestUseCase = authenticateRequestUseCase,
            activeOrganizationResolverUseCase = activeOrganizationResolverUseCase,
            effectivePermissionResolverUseCase = effectivePermissionResolverUseCase,
            requireOrganization = true,
        ) {
            hermesRequiresPermission(PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS) {
                get {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.listElectronicSequencesUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.listSequencesCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            environment = call.request.queryParameters["environment"],
                            documentType = call.request.queryParameters["documentType"],
                            status = call.request.queryParameters["status"],
                            limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100,
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                post("/ensure") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val request = call.receive<com.hermes.backend.electronicinvoicing.EnsureElectronicSequenceRequest>()
                    val result = electronicInvoicingModule.ensureElectronicSequenceAdminUseCase!!.execute(
                        request.toCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }

                get("/{sequenceId}") {
                    val context = call.hermesAuthContext()
                    val organizationId = call.requiredElectronicInvoiceOrganizationId()
                    val result = electronicInvoicingModule.getElectronicSequenceUseCase!!.execute(
                        com.hermes.backend.electronicinvoicing.getSequenceCommand(
                            organizationId = organizationId,
                            actorUserId = context.userId,
                            permissions = context.effectivePermissions?.permissions.orEmpty(),
                            sequenceId = call.requiredElectronicInvoicePath("sequenceId"),
                        )
                    )
                    call.respond(HttpStatusCode.OK, result.toResponse())
                }
            }
        }
    }
}

private suspend fun ApplicationCall.respondElectronicInvoiceArtifact(
    artifact: ElectronicDocumentArtifactFile,
) {
    response.header(
        HttpHeaders.ContentDisposition,
        "attachment; filename=\"${artifact.filename.safeDownloadFileName()}\"",
    )
    response.header("X-Hermes-Artifact-Sha256", artifact.sha256)

    respondBytes(
        bytes = artifact.bytes,
        contentType = ContentType.parse(artifact.contentType),
        status = HttpStatusCode.OK,
    )
}

private fun String.safeDownloadFileName(): String =
    substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9_.-]"), "_")
        .take(160)
        .ifBlank { "electronic-invoice-artifact.bin" }

private fun ApplicationCall.requiredElectronicInvoiceOrganizationId(): String =
    hermesAuthContext().requireActiveOrganization().organization.id

private fun ApplicationCall.requiredElectronicInvoicePath(name: String): String =
    parameters[name]?.trim()?.takeIf { it.isNotBlank() }
        ?: throw DomainRuleViolation("$name path parameter is required.")
