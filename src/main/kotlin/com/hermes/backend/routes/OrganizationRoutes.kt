package com.hermes.backend.routes

import com.hermes.application.auth.CreateOrganizationCommand
import com.hermes.application.auth.CreateOrganizationUseCase
import com.hermes.application.auth.CreateOwnerMembershipCommand
import com.hermes.application.auth.CreateOwnerMembershipUseCase
import com.hermes.backend.auth.CreateOrganizationRequest
import com.hermes.backend.auth.CreateOwnerMembershipRequest
import com.hermes.backend.auth.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureOrganizationRoutes(
    createOrganizationUseCase: CreateOrganizationUseCase,
    createOwnerMembershipUseCase: CreateOwnerMembershipUseCase,
) {
    routing {
        organizationRoutes(
            createOrganizationUseCase = createOrganizationUseCase,
            createOwnerMembershipUseCase = createOwnerMembershipUseCase,
        )
    }
}

fun io.ktor.server.routing.Route.organizationRoutes(
    createOrganizationUseCase: CreateOrganizationUseCase,
    createOwnerMembershipUseCase: CreateOwnerMembershipUseCase,
) {
    route("/organizations") {
        post {
            val request = call.receive<CreateOrganizationRequest>()
            val result = createOrganizationUseCase.execute(
                CreateOrganizationCommand(
                    ownerUserId = request.ownerUserId,
                    legalName = request.legalName,
                    commercialName = request.commercialName,
                    taxId = request.taxId,
                    countryCode = request.countryCode,
                ),
            )
            call.respond(HttpStatusCode.Created, result.toResponse())
        }

        post("/owner-memberships") {
            val request = call.receive<CreateOwnerMembershipRequest>()
            val result = createOwnerMembershipUseCase.execute(
                CreateOwnerMembershipCommand(
                    userId = request.userId,
                    organizationId = request.organizationId,
                ),
            )
            call.respond(HttpStatusCode.Created, result.toResponse())
        }
    }
}
