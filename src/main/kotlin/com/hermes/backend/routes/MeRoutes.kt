package com.hermes.backend.routes

import com.hermes.application.auth.GetMeCommand
import com.hermes.application.auth.MeUseCase
import com.hermes.backend.auth.bearerTokenOrNull
import com.hermes.backend.auth.requestedOrganizationId
import com.hermes.backend.auth.toResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureMeRoutes(
    meUseCase: MeUseCase,
) {
    routing {
        meRoutes(meUseCase)
    }
}

fun io.ktor.server.routing.Route.meRoutes(
    meUseCase: MeUseCase,
) {
    get("/me") {
        val accessToken = call.bearerTokenOrNull()
        if (accessToken == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "missing_bearer_token"))
            return@get
        }

        val result = meUseCase.execute(
            GetMeCommand(
                accessToken = accessToken,
                requestedOrganizationId = call.requestedOrganizationId(),
            ),
        )

        call.respond(HttpStatusCode.OK, result.toResponse())
    }
}
