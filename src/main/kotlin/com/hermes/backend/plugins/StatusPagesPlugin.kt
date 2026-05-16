package com.hermes.backend.plugins

import com.hermes.backend.shared.ErrorEnvelope
import com.hermes.backend.shared.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.request.header

private val logger = KotlinLogging.logger {}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled server error" }
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "internal_error",
                        message = "Unexpected server error",
                        requestId = call.request.header("X-Request-Id"),
                        details = null,
                    ),
                ),
            )
        }
    }
}
